package com.mystyle.portfolio.llm;

import com.mystyle.portfolio.common.ApiException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LlmBudgetService {
  private final Clock clock;
  private final boolean enabled;
  private final double dailyLimitRmb;
  private final double inputPriceRmbPerMillionTokens;
  private final double outputPriceRmbPerMillionTokens;
  private LocalDate activeDate;
  private double usedRmb;
  private double reservedRmb;

  @Autowired
  public LlmBudgetService(
      @Value("${app.llm.budget.zone-id:${LLM_BUDGET_ZONE_ID:Asia/Shanghai}}") String zoneId,
      @Value("${app.llm.budget.enabled:${LLM_BUDGET_ENABLED:true}}") boolean enabled,
      @Value("${app.llm.budget.daily-limit-rmb:${LLM_DAILY_BUDGET_RMB:5.0}}") double dailyLimitRmb,
      @Value("${app.llm.budget.input-price-rmb-per-million-tokens:${LLM_INPUT_PRICE_RMB_PER_MILLION_TOKENS:2.1}}") double inputPriceRmbPerMillionTokens,
      @Value("${app.llm.budget.output-price-rmb-per-million-tokens:${LLM_OUTPUT_PRICE_RMB_PER_MILLION_TOKENS:8.4}}") double outputPriceRmbPerMillionTokens) {
    this(Clock.system(resolveZoneId(zoneId)), enabled, dailyLimitRmb, inputPriceRmbPerMillionTokens, outputPriceRmbPerMillionTokens);
  }

  LlmBudgetService(
      Clock clock,
      boolean enabled,
      double dailyLimitRmb,
      double inputPriceRmbPerMillionTokens,
      double outputPriceRmbPerMillionTokens) {
    this.clock = clock;
    this.enabled = enabled;
    this.dailyLimitRmb = Math.max(0, dailyLimitRmb);
    this.inputPriceRmbPerMillionTokens = Math.max(0, inputPriceRmbPerMillionTokens);
    this.outputPriceRmbPerMillionTokens = Math.max(0, outputPriceRmbPerMillionTokens);
    this.activeDate = LocalDate.now(clock);
  }

  public synchronized Reservation reserve(int estimatedInputTokens, int reservedOutputTokens) {
    resetIfNeeded();
    if (!enabled || dailyLimitRmb <= 0) {
      return new Reservation(this, 0);
    }
    double estimatedCost = costRmb(estimatedInputTokens, reservedOutputTokens);
    double available = dailyLimitRmb - usedRmb - reservedRmb;
    if (estimatedCost > available) {
      throw ApiException.tooManyRequests("LLM 今日预算不足：每日上限 "
          + money(dailyLimitRmb)
          + " RMB，已使用 "
          + money(usedRmb)
          + " RMB，处理中预留 "
          + money(reservedRmb)
          + " RMB，当前请求预计 "
          + money(estimatedCost)
          + " RMB。请明天再试，或由维护者调整每日预算。");
    }
    reservedRmb += estimatedCost;
    return new Reservation(this, estimatedCost);
  }

  public synchronized LlmBudgetStatus status() {
    resetIfNeeded();
    double remaining = Math.max(0, dailyLimitRmb - usedRmb - reservedRmb);
    return new LlmBudgetStatus(
        activeDate.toString(),
        clock.getZone().toString(),
        enabled,
        dailyLimitRmb,
        roundMoney(usedRmb),
        roundMoney(reservedRmb),
        roundMoney(remaining),
        inputPriceRmbPerMillionTokens,
        outputPriceRmbPerMillionTokens);
  }

  public double costRmb(int inputTokens, int outputTokens) {
    return Math.max(0, inputTokens) * inputPriceRmbPerMillionTokens / 1_000_000d
        + Math.max(0, outputTokens) * outputPriceRmbPerMillionTokens / 1_000_000d;
  }

  private synchronized void complete(Reservation reservation, int inputTokens, int outputTokens) {
    resetIfNeeded();
    reservedRmb = Math.max(0, reservedRmb - reservation.reservedRmb);
    usedRmb += costRmb(inputTokens, outputTokens);
  }

  private synchronized void release(Reservation reservation) {
    resetIfNeeded();
    reservedRmb = Math.max(0, reservedRmb - reservation.reservedRmb);
  }

  private void resetIfNeeded() {
    LocalDate today = LocalDate.now(clock);
    if (!today.equals(activeDate)) {
      activeDate = today;
      usedRmb = 0;
      reservedRmb = 0;
    }
  }

  private static double roundMoney(double value) {
    return Math.round(value * 10_000d) / 10_000d;
  }

  private static String money(double value) {
    return String.format("%.4f", roundMoney(value));
  }

  private static ZoneId resolveZoneId(String zoneId) {
    try {
      return ZoneId.of(zoneId == null || zoneId.isBlank() ? "Asia/Shanghai" : zoneId.trim());
    } catch (DateTimeException exception) {
      return ZoneId.of("Asia/Shanghai");
    }
  }

  public static final class Reservation implements AutoCloseable {
    private final LlmBudgetService owner;
    private final double reservedRmb;
    private boolean closed;

    private Reservation(LlmBudgetService owner, double reservedRmb) {
      this.owner = owner;
      this.reservedRmb = reservedRmb;
    }

    public void complete(int inputTokens, int outputTokens) {
      if (closed) {
        return;
      }
      closed = true;
      owner.complete(this, inputTokens, outputTokens);
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closed = true;
      owner.release(this);
    }
  }
}
