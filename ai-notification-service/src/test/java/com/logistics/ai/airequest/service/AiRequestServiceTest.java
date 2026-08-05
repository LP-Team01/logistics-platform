package com.logistics.ai.airequest.service;

import com.logistics.ai.airequest.dto.requestdto.AiRequestDto;
import com.logistics.ai.airequest.dto.responsedto.AiCalculationResult;
import com.logistics.ai.airequest.dto.responsedto.AiResponseDto;
import com.logistics.ai.airequest.entity.AiRequest;
import com.logistics.ai.airequest.entity.AiRequestStatus;
import com.logistics.ai.airequest.repository.AiRequestRepository;
import com.logistics.ai.global.exception.BusinessException;
import com.logistics.ai.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiRequestServiceTest {

    @Mock
    private AiRequestRepository aiRequestRepository;

    @Mock
    private AiPromptService aiPromptService;

    @Mock
    private DispatchDeadlineAiClient dispatchDeadlineAiClient;

    @InjectMocks
    private AiRequestService aiRequestService;

    @Test
    @DisplayName("새로운 AI 요청을 생성하고 성공 결과를 저장한다")
    void createAiRequestSuccess() {
        // given
        AiRequestDto requestDto = createRequestDto();
        String prompt = "최종 발송 시한을 계산해주세요.";

        LocalDateTime dispatchDeadline =
            LocalDateTime.of(2026, 8, 5, 9, 0);

        AiCalculationResult calculationResult =
            mock(AiCalculationResult.class);

        when(
            calculationResult.dispatchDeadline()
        ).thenReturn(dispatchDeadline);

        DispatchDeadlineAiClient.AiExecutionResult executionResult =
            new DispatchDeadlineAiClient.AiExecutionResult(
                "{\"dispatchDeadline\":\"2026-08-05T09:00:00\"}",
                calculationResult,
                "gemini-3.6-flash",
                1500L
            );

        when(
            aiRequestRepository.existsByEventId(
                requestDto.eventId()
            )
        ).thenReturn(false);

        when(
            aiPromptService.createPrompt(requestDto)
        ).thenReturn(prompt);

        when(
            aiRequestRepository.save(any(AiRequest.class))
        ).thenAnswer(invocation -> invocation.getArgument(0));

        when(
            dispatchDeadlineAiClient.calculateDispatchDeadline(prompt)
        ).thenReturn(executionResult);

        // when
        AiResponseDto response =
            aiRequestService.createAiRequest(requestDto);

        // then
        assertThat(response).isNotNull();

        ArgumentCaptor<AiRequest> captor =
            ArgumentCaptor.forClass(AiRequest.class);

        verify(aiRequestRepository, times(2))
            .save(captor.capture());

        AiRequest completedRequest =
            captor.getAllValues().get(1);

        assertThat(completedRequest.getStatus())
            .isEqualTo(AiRequestStatus.SUCCESS);

        assertThat(completedRequest.getDispatchDeadline())
            .isEqualTo(dispatchDeadline);

        assertThat(completedRequest.getModel())
            .isEqualTo("gemini-3.6-flash");
    }

    @Test
    @DisplayName("동일한 이벤트가 존재하면 AI를 다시 호출하지 않는다")
    void returnExistingAiRequest() {
        // given
        UUID eventId = UUID.randomUUID();

        AiRequestDto requestDto =
            mock(AiRequestDto.class);

        AiRequest existingRequest =
            createTestAiRequest();

        // 이 테스트에서 실제 사용하는 eventId만 설정합니다.
        when(requestDto.eventId())
            .thenReturn(eventId);

        when(
            aiRequestRepository.existsByEventId(eventId)
        ).thenReturn(true);

        when(
            aiRequestRepository
                .findByEventIdAndDeletedAtIsNull(eventId)
        ).thenReturn(Optional.of(existingRequest));

        // when
        AiResponseDto response =
            aiRequestService.createAiRequest(requestDto);

        // then
        assertThat(response).isNotNull();

        verifyNoInteractions(
            aiPromptService,
            dispatchDeadlineAiClient
        );

        verify(
            aiRequestRepository,
            never()
        ).save(any(AiRequest.class));
    }

    @Test
    @DisplayName("AI 호출 실패 시 FAILED 상태를 저장한다")
    void createAiRequestFailed() {
        // given
        AiRequestDto requestDto = createRequestDto();
        String prompt = "최종 발송 시한을 계산해주세요.";

        BusinessException failure =
            new BusinessException(
                ErrorCode.AI_RESPONSE_FAILED
            );

        when(
            aiRequestRepository.existsByEventId(
                requestDto.eventId()
            )
        ).thenReturn(false);

        when(
            aiPromptService.createPrompt(requestDto)
        ).thenReturn(prompt);

        when(
            aiRequestRepository.save(any(AiRequest.class))
        ).thenAnswer(invocation -> invocation.getArgument(0));

        when(
            dispatchDeadlineAiClient.calculateDispatchDeadline(prompt)
        ).thenThrow(failure);

        when(
            dispatchDeadlineAiClient.getModel()
        ).thenReturn("gemini-3.6-flash");

        // when
        BusinessException exception =
            catchThrowableOfType(
                () -> aiRequestService.createAiRequest(requestDto),
                BusinessException.class
            );

        // then
        assertThat(exception).isSameAs(failure);

        ArgumentCaptor<AiRequest> captor =
            ArgumentCaptor.forClass(AiRequest.class);

        verify(aiRequestRepository, times(2))
            .save(captor.capture());

        AiRequest failedRequest =
            captor.getAllValues().get(1);

        assertThat(failedRequest.getStatus())
            .isEqualTo(AiRequestStatus.FAILED);

        assertThat(failedRequest.getErrorMessage())
            .isEqualTo(failure.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 AI 요청을 조회하면 예외가 발생한다")
    void getAiRequestNotFound() {
        // given
        UUID aiRequestId = UUID.randomUUID();

        when(
            aiRequestRepository
                .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
        ).thenReturn(Optional.empty());

        // when
        BusinessException exception =
            catchThrowableOfType(
                () -> aiRequestService.getAiRequest(aiRequestId),
                BusinessException.class
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.AI_REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("FAILED 상태가 아닌 AI 요청은 재처리할 수 없다")
    void retryAiRequestNotAllowed() {
        // given
        UUID aiRequestId = UUID.randomUUID();
        AiRequest aiRequest = createTestAiRequest();

        when(
            aiRequestRepository
                .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
        ).thenReturn(Optional.of(aiRequest));

        // when
        BusinessException exception =
            catchThrowableOfType(
                () -> aiRequestService.retryAiRequest(aiRequestId),
                BusinessException.class
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(
                ErrorCode.AI_REQUEST_RETRY_NOT_ALLOWED
            );

        verify(
            aiRequestRepository,
            never()
        ).save(any(AiRequest.class));
    }

    @Test
    @DisplayName("통계 시작일이 종료일보다 늦으면 예외가 발생한다")
    void invalidStatisticsPeriod() {
        // given
        LocalDate startDate = LocalDate.of(2026, 8, 5);
        LocalDate endDate = LocalDate.of(2026, 8, 4);

        // when
        BusinessException exception =
            catchThrowableOfType(
                () -> aiRequestService.getAiStatistics(
                    startDate,
                    endDate
                ),
                BusinessException.class
            );

        // then
        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.INVALID_STATISTICS_PERIOD);

        verifyNoInteractions(aiRequestRepository);
    }

    @Test
    @DisplayName("AI 요청을 논리 삭제할 수 있다")
    void deleteAiRequest() {
        // given
        UUID aiRequestId = UUID.randomUUID();
        UUID deletedBy = UUID.randomUUID();
        AiRequest aiRequest = createTestAiRequest();

        when(
            aiRequestRepository
                .findByAiRequestIdAndDeletedAtIsNull(aiRequestId)
        ).thenReturn(Optional.of(aiRequest));

        // when
        aiRequestService.deleteAiRequest(
            aiRequestId,
            deletedBy
        );

        // then
        assertThat(aiRequest.isDeleted()).isTrue();

        assertThat(aiRequest.getDeletedAt()).isNotNull();

        assertThat(aiRequest.getDeletedBy())
            .isEqualTo(deletedBy);
    }

    /**
     * 테스트에 사용할 AI 요청 DTO를 생성합니다.
     */
    private AiRequestDto createRequestDto() {
        AiRequestDto requestDto =
            mock(AiRequestDto.class);

        when(requestDto.eventId())
            .thenReturn(UUID.randomUUID());

        when(requestDto.orderId())
            .thenReturn(UUID.randomUUID());

        when(requestDto.deliveryId())
            .thenReturn(UUID.randomUUID());

        when(requestDto.requestText())
            .thenReturn("8월 5일까지 배송해주세요.");

        when(requestDto.requestedArrivalAt())
            .thenReturn(
                LocalDateTime.of(2026, 8, 5, 15, 0)
            );

        when(requestDto.estimatedDurationMinutes())
            .thenReturn(300);

        when(requestDto.preparationBufferMinutes())
            .thenReturn(30);

        return requestDto;
    }

    /**
     * 테스트에 사용할 AI 요청 엔티티를 생성합니다.
     */
    private AiRequest createTestAiRequest() {
        return AiRequest.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "8월 5일까지 배송해주세요.",
            LocalDateTime.of(2026, 8, 5, 15, 0),
            300,
            30,
            "최종 발송 시한을 계산해주세요."
        );
    }
}
