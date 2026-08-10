package com.logistics.ai.airequest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.logistics.ai.airequest.dto.responsedto.VisitSequenceAiResult;
import com.logistics.ai.global.exception.GeminiProcessingException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Google Gen AI Java SDK를 이용하여 Gemini API를 직접 호출합니다.
 *
 * <p>{@link GeminiClient}(최종 발송 시한 계산)와 별개의 AI 작업(방문 순서 미세 조정)을
 * 처리하므로, 같은 gemini.* 설정을 공유하되 별도의 Client 인스턴스로 분리합니다.</p>
 */
@Component
public class GeminiVisitSequenceClient implements VisitSequenceAiClient {

    private final Client client;
    private final ObjectMapper objectMapper;
    private final GenerateContentConfig generateContentConfig;
    private final String model;

    /**
     * Gemini API 클라이언트와 요청 옵션을 생성합니다.
     */
    public GeminiVisitSequenceClient(
        ObjectMapper objectMapper,
        @Value("${gemini.api-key:}")
        String apiKey,
        @Value("${gemini.model:gemini-3.6-flash}")
        String model,
        @Value("${gemini.temperature:0.1}")
        Float temperature,
        @Value("${gemini.timeout-ms:30000}")
        Integer timeoutMs
    ) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                "Gemini API 키가 설정되지 않았습니다."
            );
        }

        if (timeoutMs == null || timeoutMs <= 0) {
            throw new IllegalStateException(
                "Gemini 타임아웃은 0보다 커야 합니다."
            );
        }

        HttpOptions httpOptions =
            HttpOptions.builder()
                .timeout(timeoutMs)
                .build();

        this.client = Client.builder()
            .apiKey(apiKey)
            .httpOptions(httpOptions)
            .build();

        this.objectMapper = objectMapper;
        this.model = model;

        this.generateContentConfig =
            GenerateContentConfig.builder()
                .temperature(temperature)
                .responseMimeType("application/json")
                .build();
    }

    /**
     * Gemini API에 프롬프트를 직접 전달하여 방문 순서를 미세 조정합니다.
     *
     * @param prompt Gemini에 전달할 최종 프롬프트
     * @return Gemini 실행 결과
     */
    @Override
    public AiExecutionResult refineVisitSequence(
        String prompt
    ) {
        long startTime = System.nanoTime();

        try {
            GenerateContentResponse response =
                client.models.generateContent(
                    model,
                    prompt,
                    generateContentConfig
                );

            String rawResponse = response.text();

            validateRawResponse(rawResponse);

            String jsonResponse = extractJson(rawResponse);

            VisitSequenceAiResult result =
                objectMapper.readValue(
                    jsonResponse,
                    VisitSequenceAiResult.class
                );

            if (!result.isValid()) {
                throw new GeminiProcessingException(
                    "Gemini 응답에 필수 방문 순서 결과가 없습니다."
                );
            }

            return new AiExecutionResult(
                rawResponse,
                result,
                model,
                calculateProcessingTime(startTime)
            );

        } catch (GeminiProcessingException exception) {
            throw exception;

        } catch (JsonProcessingException exception) {
            throw new GeminiProcessingException(
                "Gemini 응답을 JSON으로 변환할 수 없습니다.",
                exception
            );

        } catch (Exception exception) {
            throw new GeminiProcessingException(
                "Gemini API 직접 호출에 실패했습니다.",
                exception
            );
        }
    }

    /**
     * 현재 직접 호출에 사용되는 Gemini 모델을 반환합니다.
     */
    @Override
    public String getModel() {
        return model;
    }

    /**
     * Gemini 응답이 비어 있는지 검사합니다.
     */
    private void validateRawResponse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new GeminiProcessingException(
                "Gemini가 빈 응답을 반환했습니다."
            );
        }
    }

    /**
     * Gemini 응답에서 JSON 객체만 추출합니다.
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
     * Gemini 호출 처리시간을 밀리초 단위로 계산합니다.
     */
    private long calculateProcessingTime(long startTime) {
        long elapsedTime = System.nanoTime() - startTime;

        return TimeUnit.NANOSECONDS.toMillis(elapsedTime);
    }

    /**
     * 애플리케이션 종료 시 SDK의 HTTP 자원을 정리합니다.
     */
    @PreDestroy
    public void close() {
        client.close();
    }
}