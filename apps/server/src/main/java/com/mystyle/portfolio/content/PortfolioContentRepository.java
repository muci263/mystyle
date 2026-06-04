package com.mystyle.portfolio.content;

import com.mystyle.portfolio.content.ContentModels.Experience;
import com.mystyle.portfolio.content.ContentModels.InterviewGuide;
import com.mystyle.portfolio.content.ContentModels.BlogPost;
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
}
