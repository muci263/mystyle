package com.mystyle.portfolio.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mystyle.portfolio.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class LlmBudgetServiceTest {
  private static final Clock FIXED_CLOCK = Clock.fixed(
      Instant.parse("2026-06-21T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  @Test
  void shouldReserveSettleAndRejectWhenDailyBudgetExceeded() {
    LlmBudgetService service = new LlmBudgetService(FIXED_CLOCK, true, 0.01, 100.0, 100.0);

    LlmBudgetService.Reservation reservation = service.reserve(20, 20);
    assertEquals(0.004, service.status().reservedRmb());

    reservation.complete(20, 20);
    assertEquals(0.004, service.status().usedRmb());
    assertEquals(0.0, service.status().reservedRmb());

    assertThrows(ApiException.class, () -> service.reserve(40, 40));
  }
}
