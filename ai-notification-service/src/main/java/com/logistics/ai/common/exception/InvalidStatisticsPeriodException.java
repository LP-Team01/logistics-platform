package com.logistics.ai.common.exception;

import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 통계 조회 시작일이 종료일보다 늦은 경우 발생합니다.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStatisticsPeriodException
    extends RuntimeException {

    public InvalidStatisticsPeriodException(
        LocalDate startDate,
        LocalDate endDate
    ) {
        super(
            "통계 조회 시작일은 종료일보다 늦을 수 없습니다. "
                + "startDate=" + startDate
                + ", endDate=" + endDate
        );
    }
}
