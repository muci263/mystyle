package com.mystyle.portfolio.resume;

import com.mystyle.portfolio.common.ApiResponse;
import com.mystyle.portfolio.resume.ResumeModels.ResumeBasicInfo;
import com.mystyle.portfolio.resume.ResumeModels.ResumeDraftView;
import com.mystyle.portfolio.resume.ResumeModels.ResumeSectionItem;
import com.mystyle.portfolio.resume.ResumeModels.ResumeUploadTask;
import com.mystyle.portfolio.resume.ResumeModels.ResumeVersion;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/resume")
public class ResumeAdminController {
  private final ResumeAdminService resumeAdminService;

  public ResumeAdminController(ResumeAdminService resumeAdminService) {
    this.resumeAdminService = resumeAdminService;
  }

  @GetMapping("/draft")
  public ApiResponse<ResumeDraftView> draft() {
    return ApiResponse.success(resumeAdminService.draft());
  }

  @PutMapping("/basic-info")
  public ApiResponse<ResumeBasicInfo> updateBasicInfo(@Valid @RequestBody ResumeBasicInfoRequest request) {
    return ApiResponse.success(resumeAdminService.updateBasicInfo(request));
  }

  @GetMapping("/sections/{sectionType}/items")
  public ApiResponse<List<ResumeSectionItem>> sectionItems(@PathVariable("sectionType") String sectionType) {
    return ApiResponse.success(resumeAdminService.sectionItems(sectionType));
  }

  @PostMapping("/sections/{sectionType}/items")
  public ApiResponse<ResumeSectionItem> createSectionItem(
      @PathVariable("sectionType") String sectionType,
      @Valid @RequestBody ResumeSectionItemRequest request) {
    return ApiResponse.success(resumeAdminService.createSectionItem(sectionType, request));
  }

  @PutMapping("/items/{itemId}")
  public ApiResponse<ResumeSectionItem> updateSectionItem(
      @PathVariable("itemId") long itemId,
      @Valid @RequestBody ResumeSectionItemRequest request) {
    return ApiResponse.success(resumeAdminService.updateSectionItem(itemId, request));
  }

  @DeleteMapping("/items/{itemId}")
  public ApiResponse<Void> deleteSectionItem(@PathVariable("itemId") long itemId) {
    resumeAdminService.deleteSectionItem(itemId);
    return ApiResponse.success(null);
  }

  @PostMapping("/publish")
  public ApiResponse<ResumeVersion> publishDraft() {
    return ApiResponse.success(resumeAdminService.publishDraft());
  }

  @GetMapping("/versions")
  public ApiResponse<List<ResumeVersion>> versions() {
    return ApiResponse.success(resumeAdminService.versions());
  }

  @PostMapping("/uploads/parse")
  public ApiResponse<ResumeUploadTask> parseUpload(@Valid @RequestBody ResumeUploadParseRequest request) {
    return ApiResponse.success(resumeAdminService.parseUpload(request));
  }

  @GetMapping("/uploads/{taskId}")
  public ApiResponse<ResumeUploadTask> uploadTask(@PathVariable("taskId") long taskId) {
    return ApiResponse.success(resumeAdminService.uploadTask(taskId));
  }

  @PostMapping("/uploads/{taskId}/confirm")
  public ApiResponse<ResumeDraftView> confirmUploadTask(@PathVariable("taskId") long taskId) {
    return ApiResponse.success(resumeAdminService.confirmUploadTask(taskId));
  }
}
