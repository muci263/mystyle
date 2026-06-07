package com.mystyle.portfolio.content;

import com.mystyle.portfolio.content.ContentModels.Experience;
import com.mystyle.portfolio.content.ContentModels.InterviewGuide;
import com.mystyle.portfolio.knowledge.KnowledgeGraphEdgeRequest;
import com.mystyle.portfolio.knowledge.KnowledgeGraphNodeRequest;
import com.mystyle.portfolio.blog.BlogAnnotationRequest;
import com.mystyle.portfolio.blog.BlogCommentRequest;
import com.mystyle.portfolio.blog.BlogPostRequest;
import com.mystyle.portfolio.content.ContentModels.BlogAnnotation;
import com.mystyle.portfolio.content.ContentModels.BlogCategory;
import com.mystyle.portfolio.content.ContentModels.BlogComment;
import com.mystyle.portfolio.content.ContentModels.BlogInteractionSummary;
import com.mystyle.portfolio.content.ContentModels.BlogPost;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphEdge;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphNode;
import com.mystyle.portfolio.content.ContentModels.KnowledgeGraphView;
import com.mystyle.portfolio.content.ContentModels.ModuleDemo;
import com.mystyle.portfolio.content.ContentModels.Profile;
import com.mystyle.portfolio.content.ContentModels.Project;
import com.mystyle.portfolio.content.ContentModels.SkillGroup;
import com.mystyle.portfolio.content.ContentModels.TimelineItem;
import java.util.List;

public interface PortfolioContentRepository {
  Profile profile();

  List<SkillGroup> skills();

  List<Experience> experiences();

  List<Project> projects();

  List<ModuleDemo> moduleDemos();

  InterviewGuide interviewGuide();

  List<TimelineItem> timeline();

  List<BlogPost> blogPosts();

  List<BlogCategory> blogCategories();

  BlogPost createBlogPost(BlogPostRequest request);

  BlogPost updateBlogPost(String slug, BlogPostRequest request);

  List<BlogComment> blogComments(String slug);

  BlogComment addBlogComment(String slug, BlogCommentRequest request);

  List<BlogAnnotation> blogAnnotations(String slug);

  BlogAnnotation addBlogAnnotation(String slug, BlogAnnotationRequest request);

  BlogAnnotation updateBlogAnnotation(String slug, long annotationId, BlogAnnotationRequest request);

  BlogInteractionSummary deleteBlogAnnotation(String slug, long annotationId);

  BlogInteractionSummary blogInteractionSummary(String slug);

  BlogInteractionSummary likeBlogPost(String slug);

  KnowledgeGraphView knowledgeGraph();

  List<KnowledgeGraphNode> knowledgeGraphNodes(boolean includeHidden);

  KnowledgeGraphNode createKnowledgeGraphNode(KnowledgeGraphNodeRequest request);

  KnowledgeGraphNode updateKnowledgeGraphNode(String nodeKey, KnowledgeGraphNodeRequest request);

  void deleteKnowledgeGraphNode(String nodeKey);

  List<KnowledgeGraphEdge> knowledgeGraphEdges(boolean includeHidden);

  KnowledgeGraphEdge createKnowledgeGraphEdge(KnowledgeGraphEdgeRequest request);

  KnowledgeGraphEdge updateKnowledgeGraphEdge(long edgeId, KnowledgeGraphEdgeRequest request);

  void deleteKnowledgeGraphEdge(long edgeId);
}
