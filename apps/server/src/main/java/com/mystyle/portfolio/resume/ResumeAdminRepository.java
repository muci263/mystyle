package com.mystyle.portfolio.resume;

import com.mystyle.portfolio.resume.ResumeModels.ResumeBasicInfo;
import com.mystyle.portfolio.resume.ResumeModels.ResumeDraftView;
import com.mystyle.portfolio.resume.ResumeModels.ResumeSectionItem;
import com.mystyle.portfolio.resume.ResumeModels.ResumeUploadTask;
import com.mystyle.portfolio.resume.ResumeModels.ResumeVersion;
import java.util.List;

public interface ResumeAdminRepository {
  ResumeDraftView draft();

  ResumeDraftView publicResume();

  ResumeBasicInfo updateBasicInfo(ResumeBasicInfoRequest request);

  List<ResumeSectionItem> sectionItems(ResumeSectionType sectionType);

  ResumeSectionItem createSectionItem(ResumeSectionType sectionType, ResumeSectionItemRequest request);

  ResumeSectionItem updateSectionItem(long itemId, ResumeSectionItemRequest request);

  void deleteSectionItem(long itemId);

  ResumeVersion publishDraft();

  List<ResumeVersion> versions();

  ResumeUploadTask createUploadTask(String filename, String contentType, String rawText, String status, String parsedJson, String errorMessage);

  ResumeUploadTask uploadTask(long taskId);

  ResumeDraftView confirmUploadTask(long taskId);
}
