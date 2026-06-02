package com.mystyle.portfolio.moduleDemo;

import java.util.List;
import java.util.Map;

public record VideoLearningSnapshot(
    Map<String, Object> redisRecord,
    Map<String, Object> mysqlRecord,
    String learningStatus,
    int writeCount,
    int syncCount,
    List<VideoLearningLog> logs) {
}
