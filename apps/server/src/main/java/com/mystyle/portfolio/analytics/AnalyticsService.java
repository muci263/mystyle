package com.mystyle.portfolio.analytics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
  private final AtomicLong idGenerator = new AtomicLong(1);
  private final Map<Long, Map<String, Object>> events = new ConcurrentHashMap<>();

  public AnalyticsEventResponse record(AnalyticsEventRequest request) {
    long eventId = idGenerator.getAndIncrement();
    Map<String, Object> event = new LinkedHashMap<>();
    event.put("eventType", request.eventType());
    event.put("path", request.path());
    event.put("payload", request.payload());
    events.put(eventId, event);
    return new AnalyticsEventResponse(eventId, request.eventType(), "ACCEPTED");
  }

  public int totalCount() {
    return events.size();
  }
}
