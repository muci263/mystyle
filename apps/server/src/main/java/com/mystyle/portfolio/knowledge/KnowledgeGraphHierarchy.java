package com.mystyle.portfolio.knowledge;

import com.mystyle.portfolio.common.ApiException;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import java.util.Locale;
import java.util.Set;

public final class KnowledgeGraphHierarchy {
  private static final Set<String> TERTIARY_TYPES = Set.of("SKILL", "PROJECT", "MODULE", "BLOG");

  private KnowledgeGraphHierarchy() {
  }

  public static int expectedLevel(String nodeType) {
    return switch (normalizeType(nodeType)) {
      case "CORE" -> 0;
      case "SECTION" -> 1;
      case "SKILL", "PROJECT", "MODULE", "BLOG" -> 2;
      default -> throw ApiException.badRequest("未知节点类型：" + nodeType);
    };
  }

  public static void validateNodeLevel(String nodeType, int level) {
    int expected = expectedLevel(nodeType);
    if (level != expected) {
      throw ApiException.badRequest("节点层级不匹配：" + normalizeType(nodeType) + " 必须为 " + displayLevel(expected));
    }
  }

  public static void validateEdge(KnowledgeGraphNode from, KnowledgeGraphNode to, String relationType) {
    String relation = normalizeRelation(relationType);
    if (from.nodeKey().equals(to.nodeKey())) {
      throw ApiException.badRequest("关系起点和终点不能相同");
    }
    if (Math.abs(from.level() - to.level()) > 1) {
      throw ApiException.badRequest("不能跨级连接：" + displayLevel(from.level()) + " 节点不能直接连接 " + displayLevel(to.level()) + " 节点");
    }
    if (from.level() == 0 || to.level() == 0) {
      if (!(from.level() == 0 && to.level() == 1 && "OWNS".equals(relation))) {
        throw ApiException.badRequest("一级个人节点只能通过 OWNS 指向二级栏目");
      }
      return;
    }
    if (from.level() == 1 || to.level() == 1) {
      if (!(from.level() == 1 && to.level() == 2 && Set.of("INCLUDES", "CONTAINS").contains(relation))) {
        throw ApiException.badRequest("二级栏目只能通过 INCLUDES/CONTAINS 指向三级内容节点");
      }
      return;
    }
    if (from.level() == 2 && to.level() == 2 && !Set.of("USES", "EXPLAINS", "RELATED").contains(relation)) {
      throw ApiException.badRequest("三级内容节点之间只能使用 USES/EXPLAINS/RELATED 证据关系");
    }
  }

  public static boolean edgeAllowed(KnowledgeGraphNode from, KnowledgeGraphNode to, String relationType) {
    try {
      validateEdge(from, to, relationType);
      return true;
    } catch (ApiException exception) {
      return false;
    }
  }

  public static boolean isTertiary(String nodeType) {
    return TERTIARY_TYPES.contains(normalizeType(nodeType));
  }

  public static String normalizeRelation(String relationType) {
    return relationType == null || relationType.isBlank()
        ? "RELATED"
        : relationType.trim().toUpperCase(Locale.ROOT);
  }

  public static String normalizeType(String nodeType) {
    return nodeType == null ? "" : nodeType.trim().toUpperCase(Locale.ROOT);
  }

  public static String displayLevel(int level) {
    return switch (level) {
      case 0 -> "一级";
      case 1 -> "二级";
      case 2 -> "三级";
      default -> "未知层级";
    };
  }
}
