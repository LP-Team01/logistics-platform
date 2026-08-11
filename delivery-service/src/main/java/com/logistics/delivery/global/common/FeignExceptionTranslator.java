package com.logistics.delivery.global.common;

import com.logistics.delivery.global.exception.BusinessException;
import com.logistics.delivery.global.exception.ErrorCode;
import feign.FeignException;
import java.util.function.Supplier;

// 외부 서비스(Feign) 호출 실패를 도메인 예외로 변환하는 로직을 한 곳에 모은 유틸.
public final class FeignExceptionTranslator {

    private FeignExceptionTranslator() {
    }

    public static <T> T call(Supplier<T> feignCall, ErrorCode notFoundError) {
        try {
            return feignCall.get();
        } catch (FeignException.NotFound e) {
            throw new BusinessException(notFoundError);
        }
    }

    public static <T> T call(Supplier<T> feignCall, ErrorCode notFoundError, ErrorCode invalidRequestError) {
        try {
            return feignCall.get();
        } catch (FeignException.NotFound e) {
            throw new BusinessException(notFoundError);
        } catch (FeignException.BadRequest | FeignException.UnprocessableEntity e) {
            throw new BusinessException(invalidRequestError);
        }
    }
}
