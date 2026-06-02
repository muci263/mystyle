package com.mystyle.portfolio.moduleDemo;

import com.mystyle.portfolio.common.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class VideoLearningService {
  private final List<VideoLearningLog> logs = new ArrayList<>();
  private final Map<String, Object> redisRecord = new LinkedHashMap<>();
  private final Map<String, Object> mysqlRecord = new LinkedHashMap<>();
  private int writeCount;
  private int syncCount;

  public synchronized VideoLearningSnapshot reset() {
    logs.clear();
    redisRecord.clear();
    mysqlRecord.clear();
    writeCount = 0;
    syncCount = 0;
    logs.add(log("reset", "演示数据已重置"));
    return snapshot();
  }

  public synchronized VideoLearningSnapshot progress(VideoProgressRequest request) {
    if (request.currentSecond() > request.durationSecond()) {
      throw ApiException.badRequest("currentSecond 不能大于 durationSecond");
    }
    int percent = (int) Math.round(request.currentSecond() * 100.0 / request.durationSecond());
    redisRecord.put("userId", request.userId());
    redisRecord.put("courseId", request.courseId());
    redisRecord.put("lessonId", request.lessonId());
    redisRecord.put("currentSecond", request.currentSecond());
    redisRecord.put("durationSecond", request.durationSecond());
    redisRecord.put("progressPercent", percent);
    redisRecord.put("updatedAt", Instant.now().toString());
    writeCount++;
    logs.add(log("cache_write", "进度写入 Redis 模拟缓存，避免每次播放事件直接落库"));
    return snapshot();
  }

  public synchronized VideoLearningSnapshot complete() {
    if (redisRecord.isEmpty()) {
      throw ApiException.badRequest("请先上报视频进度");
    }
    mysqlRecord.clear();
    mysqlRecord.putAll(redisRecord);
    mysqlRecord.put("learningStatus", "COMPLETED");
    mysqlRecord.put("syncedAt", Instant.now().toString());
    syncCount++;
    logs.add(log("db_sync", "完播触发 MySQL 模拟落库，并更新学习状态"));
    return snapshot();
  }

  public synchronized VideoLearningSnapshot snapshot() {
    String status = mysqlRecord.isEmpty() ? "IN_PROGRESS" : "COMPLETED";
    return new VideoLearningSnapshot(
        Map.copyOf(redisRecord),
        Map.copyOf(mysqlRecord),
        status,
        writeCount,
        syncCount,
        List.copyOf(logs));
  }

  private VideoLearningLog log(String event, String message) {
    return new VideoLearningLog(event, message, Instant.now().toString());
  }
}
