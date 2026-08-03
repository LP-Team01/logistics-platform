package com.logistics.ai.airequest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.ai.airequest.dto.responsedto.AiCalculationResult;
import com.logistics.ai.common.exception.GeminiProcessingException;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Spring AI의 ChatClient를 이용하여 Gemini API를 호출합니다.
 *
 * <p>Gemini의 원본 문자열 응답을 AiCalculationResult로
 * 역직렬화하고 처리시간과 모델 정보를 함께 반환합니다.</p>
 */
@Component
public class GeminiClient {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    @Getter
    private final String model;

    /**
     * Spring AI가 자동 구성한 ChatModel을 이용하여 ChatClient를 생성합니다.
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public GeminiClient(
        ChatModel chatModel,
        ObjectMapper objectMapper,
        @Value(
            "${spring.ai.google.genai.chat.options.model:"
                + "gemini-2.0-flash}"
        )
        String model
    ) {
        this.chatClient = ChatClient.create(chatModel);
        this.objectMapper = objectMapper;
        this.model = model;
    }

    /**
     * Gemini에 프롬프트를 전달하고 계산 결과를 반환합니다.
     *
     * @param prompt Gemini에 전달할 최종 프롬프트
     * @return Gemini 실행 결과
     */
    public GeminiExecutionResult calculateDispatchDeadline(
        String prompt
    ) {
        long startTime = System.nanoTime();

        try {
            // Gemini에 프롬프트를 전달하고 문자열 응답을 받습니다.
            String rawResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

            validateRawResponse(rawResponse);

            // 마크다운이나 부가 설명을 제거하고 JSON 객체만 추출합니다.
            String jsonResponse = extractJson(rawResponse);

            // JSON 문자열을 Java DTO로 역직렬화합니다.
            AiCalculationResult calculationResult =
                objectMapper.readValue(
                    jsonResponse,
                    AiCalculationResult.class
                );

            if (!calculationResult.isValid()) {
                throw new GeminiProcessingException(
                    "Gemini 응답에 필수 계산 결과가 없습니다."
                );
            }

            return new GeminiExecutionResult(
                rawResponse,
                calculationResult,
                model,
                calculateProcessingTime(startTime)
            );

        } catch (GeminiProcessingException exception) {
            // 이미 정의된 Gemini 처리 예외는 그대로 전달합니다.
            throw exception;

        } catch (JsonProcessingException exception) {
            // Gemini 응답이 올바른 JSON 형식이 아닌 경우입니다.
            throw new GeminiProcessingException(
                "Gemini 응답을 JSON으로 변환할 수 없습니다.",
                exception
            );

        } catch (Exception exception) {
            // 네트워크 오류, 인증 실패 및 Gemini API 오류를 처리합니다.
            throw new GeminiProcessingException(
                "Gemini API 호출에 실패했습니다.",
                exception
            );
        }
    }

    /**
     * Gemini 응답이 비어 있는지 확인합니다.
     */
    private void validateRawResponse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new GeminiProcessingException(
                "Gemini가 빈 응답을 반환했습니다."
            );
        }
    }

    /**
     * Gemini 응답에서 JSON 객체 부분만 추출합니다.
     *
     * <p>Gemini가 마크다운 코드 블록 또는 추가 문장을 반환하더라도
     * 첫 번째 중괄호부터 마지막 중괄호까지 추출합니다.</p>
     */
    private String extractJson(String rawResponse) {
        int jsonStart = rawResponse.indexOf('{');
        int jsonEnd = rawResponse.lastIndexOf('}');

        if (jsonStart < 0 || jsonEnd < jsonStart) {
            throw new GeminiProcessingException(
                "Gemini 응답에서 JSON 객체를 찾을 수 없습니다."
            );
        }

        return rawResponse.substring(jsonStart, jsonEnd + 1);
    }

    /**
     * Gemini 호출에 걸린 시간을 밀리초 단위로 계산합니다.
     */
    private long calculateProcessingTime(long startTime) {
        long elapsedTime = System.nanoTime() - startTime;

        return TimeUnit.NANOSECONDS.toMillis(elapsedTime);
    }

    /**
     * Gemini 한 번의 실행 결과입니다.
     *
     * @param rawResponse Gemini 원본 응답
     * @param calculationResult 역직렬화된 계산 결과
     * @param model 사용된 Gemini 모델
     * @param processingTimeMs 처리시간(ms)
     */
    public record GeminiExecutionResult(
        String rawResponse,
        AiCalculationResult calculationResult,
        String model,
        Long processingTimeMs
    ) {
    }
}
