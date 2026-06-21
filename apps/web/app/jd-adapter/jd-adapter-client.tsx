"use client";

import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";
import { Clipboard, FileText, Upload } from "lucide-react";
import { LlmProgressPanel, useLlmProgress } from "@/components/llm-progress";
import { apiGet, apiPost } from "@/lib/api";
import type {
  JdAnalysisResponse,
  MockInterviewResponse,
  ModuleDemo,
  Project,
  ResumeDraftView,
  ResumeExtractedText,
  ResumeOptimizeResponse,
} from "@/lib/api";
import { Surface } from "@/components/site-shell";

const defaultJd = "Java 后端实习，要求 Spring Boot、Redis、MySQL、微服务，有 AI 或 RAG 经验加分";
const maxResumeChars = 12000;
const maxResumeFileBytes = 5 * 1024 * 1024;

export function JdAdapterClient() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [modules, setModules] = useState<ModuleDemo[]>([]);
  const [jd, setJd] = useState(defaultJd);
  const [resumeText, setResumeText] = useState("");
  const [resumeSource, setResumeSource] = useState("未选择");
  const [analysis, setAnalysis] = useState<JdAnalysisResponse | null>(null);
  const [resumeOptimization, setResumeOptimization] = useState<ResumeOptimizeResponse | null>(null);
  const [mockInterview, setMockInterview] = useState<MockInterviewResponse | null>(null);
  const [assetLoading, setAssetLoading] = useState(true);
  const [assetError, setAssetError] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [copyMessage, setCopyMessage] = useState("");
  const llmProgress = useLlmProgress();

  useEffect(() => {
    let active = true;

    async function loadAssets() {
      setAssetLoading(true);
      setAssetError("");
      try {
        const [projectAssets, moduleAssets] = await Promise.all([
          apiGet<Project[]>("/public/projects"),
          apiGet<ModuleDemo[]>("/public/module-demos"),
        ]);
        if (!active) return;
        setProjects(projectAssets);
        setModules(moduleAssets);
      } catch (exception) {
        if (!active) return;
        setAssetError(exception instanceof Error ? exception.message : "经历资产加载失败");
      } finally {
        if (active) {
          setAssetLoading(false);
        }
      }
    }

    loadAssets();

    return () => {
      active = false;
    };
  }, []);

  const orderedProjects = useMemo(() => {
    if (!analysis) return projects;
    return analysis.projectRecommendations
      .map((recommendation) => projects.find((project) => project.slug === recommendation.slug))
      .filter((project): project is Project => Boolean(project));
  }, [analysis, projects]);

  const orderedModules = useMemo(() => {
    if (!analysis) return modules.slice(0, 3);
    return analysis.moduleRecommendations
      .map((recommendation) => modules.find((module) => module.slug === recommendation.slug))
      .filter((module): module is ModuleDemo => Boolean(module));
  }, [analysis, modules]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAnalysis(false);
  }

  async function readResumeFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setError("");
    try {
      if (file.size > maxResumeFileBytes) {
        setError("简历文件不能超过 5MB。");
        return;
      }
      const content = await resumeTextFromFile(file);
      if (content.trim().length > maxResumeChars) {
        setError(`简历文本超过 ${maxResumeChars} 字符，请压缩后再上传。`);
        return;
      }
      setResumeText(content);
      setResumeSource(file.name);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "简历文件读取失败");
    } finally {
      event.target.value = "";
    }
  }

  async function useDraftResume() {
    setError("");
    try {
      const draft = await apiGet<ResumeDraftView>("/admin/resume/draft");
      const draftText = resumeTextFromDraft(draft);
      if (!draftText.trim()) {
        setError("当前草稿为空，请上传或粘贴简历内容。");
        return "";
      }
      setResumeText(draftText);
      setResumeSource("当前简历草稿");
      return draftText;
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "当前草稿读取失败";
      setError(message);
      return "";
    }
  }

  async function copyGeneratedResume() {
    if (!resumeOptimization?.generatedResumeMarkdown) return;
    await navigator.clipboard.writeText(resumeOptimization.generatedResumeMarkdown);
    setCopyMessage("已复制 Markdown");
    window.setTimeout(() => setCopyMessage(""), 1800);
  }

  async function runAnalysis(allowFallback: boolean) {
    setLoading(true);
    setError("");
    setCopyMessage("");
    setAnalysis(null);
    setResumeOptimization(null);
    setMockInterview(null);
    llmProgress.start(allowFallback ? "规则兜底 JD 简历链路" : "JD 简历优化与模拟面试", [
      { id: "contract", label: "输入输出契约", detail: "约定 JD、简历文本、项目资产、模块资产、生成简历和面试问答字段。" },
      { id: "resume-source", label: "确认简历来源", detail: "读取上传、粘贴或当前草稿中的真实简历文本。" },
      { id: "jd", label: "岗位匹配分析", detail: "Minimax 正在生成岗位定位、关键词、项目排序和风险提示。" },
      { id: "resume", label: "生成适配简历", detail: "Minimax 基于 JD 和真实简历生成可投递 Markdown 草案。" },
      { id: "interview", label: "模拟面试问答", detail: "Minimax 正在生成面试题、追问、强回答和评分点。" },
      { id: "render", label: "结果呈现", detail: "渲染推荐资产、生成简历和面试问答。" },
    ]);
    try {
      llmProgress.setStep("contract", "done", "输入输出已限定：不编造经历，默认必须真实调用 LLM，只有显式按钮才规则兜底。");
      llmProgress.activate("resume-source", "正在确认简历文本来源。");
      let finalResumeText = resumeText.trim();
      if (!finalResumeText) {
        finalResumeText = await useDraftResume();
      }
      if (!finalResumeText) {
        throw new Error("请先上传、粘贴简历，或使用当前简历草稿。");
      }

      llmProgress.activate("jd", allowFallback ? "用户已确认使用规则降级，本次不调用 Minimax。" : "正在分析岗位关键词、匹配度和证据资产。");
      const result = await apiPost<JdAnalysisResponse>("/jd/analyze", {
        jd,
        variantName: "前端联调生成版本",
        allowFallback,
      });
      setAnalysis(result);

      const assetHints = [
        ...result.projectRecommendations.slice(0, 3).map((item) => item.name),
        ...result.moduleRecommendations.slice(0, 2).map((item) => item.title),
      ];

      llmProgress.activate("resume", allowFallback ? "用户已确认使用规则降级，正在生成规则草案。" : "正在生成 JD 适配版简历 Markdown。");
      const optimize = await apiPost<ResumeOptimizeResponse>("/llm/resume/optimize", {
        jdText: jd,
        resumeText: finalResumeText,
        targetRole: result.role,
        assetHints,
        allowFallback,
      });
      setResumeOptimization(optimize);

      llmProgress.activate("interview", allowFallback ? "用户已确认使用规则降级，正在生成面试演练模板。" : "正在基于生成简历和 JD 生成模拟面试问答。");
      const interview = await apiPost<MockInterviewResponse>("/llm/interview/mock", {
        jdText: jd,
        resumeText: optimize.generatedResumeMarkdown || finalResumeText,
        targetRole: result.role,
        assetHints,
        allowFallback,
      });
      setMockInterview(interview);

      llmProgress.activate("render", "正在刷新推荐顺序、生成简历和模拟面试问答。");
      llmProgress.complete(`完成：${result.role}，匹配度 ${result.matchScore}，已生成简历与 ${interview.questions.length} 道面试问答。`);
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "岗位分析失败";
      setError(message);
      llmProgress.fail(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 pt-10 md:px-8 lg:grid-cols-[0.9fr_1.1fr]">
      <Surface>
        <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Input</p>
        <form onSubmit={submit}>
          <label className="mt-5 block text-sm font-medium text-ink" htmlFor="jd">
            岗位 JD
          </label>
          <textarea
            id="jd"
            value={jd}
            onChange={(event) => setJd(event.target.value)}
            className="mt-3 min-h-48 w-full resize-none border border-line bg-stonepaper p-4 text-sm leading-7 text-ink outline-none placeholder:text-graphite focus:border-accent"
            placeholder="粘贴 Java 后端、AI 应用或全栈岗位 JD..."
          />

          <div className="mt-5 flex items-center justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-ink">简历来源</p>
              <p className="mt-1 text-xs text-graphite">{resumeSource}</p>
            </div>
            <div className="flex flex-wrap justify-end gap-2">
              <label className="secondary-action inline-flex cursor-pointer items-center gap-2 px-4 py-2 text-sm font-medium">
                <Upload size={15} />
                上传简历
                <input
                  type="file"
                  accept=".txt,.md,.markdown,.pdf,.doc,.docx,text/plain,text/markdown,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  className="sr-only"
                  onChange={readResumeFile}
                />
              </label>
              <button type="button" onClick={useDraftResume} className="secondary-action inline-flex items-center gap-2 px-4 py-2 text-sm font-medium">
                <FileText size={15} />
                使用当前草稿
              </button>
            </div>
          </div>

          <textarea
            value={resumeText}
            onChange={(event) => {
              setResumeText(event.target.value);
              setResumeSource(event.target.value.trim() ? "手动粘贴" : "未选择");
            }}
            className="mt-3 min-h-64 w-full resize-y border border-line bg-white p-4 text-sm leading-7 text-ink outline-none placeholder:text-graphite focus:border-accent"
            placeholder="上传 PDF、DOC、DOCX、TXT、Markdown 简历，或直接粘贴简历文本。为空时会尝试读取当前简历草稿。"
          />
          <p className="mt-2 text-xs text-graphite">
            {resumeText.trim().length}/{maxResumeChars} 字符
          </p>

          <div className="mt-5 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => runAnalysis(true)}
              disabled={loading || jd.trim().length === 0}
              className="secondary-action px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55"
            >
              规则兜底预览
            </button>
            <button disabled={loading || jd.trim().length === 0} className="primary-action px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55">
              {loading ? "生成中..." : "生成 JD 简历与面试"}
            </button>
          </div>
          {error ? <p className="mt-4 border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-700">{error}</p> : null}
        </form>
        <LlmProgressPanel state={llmProgress.progress} onDismiss={llmProgress.reset} />
      </Surface>

      <Surface>
        <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">
          {analysis ? analysis.provider : "Database Preview"}
        </p>
        <h2 className="display-light mt-5 text-4xl text-ink">
          {analysis ? `${analysis.role} 匹配度 ${analysis.matchScore}` : "等待岗位 JD 分析"}
        </h2>
        <p className="mt-5 leading-7 text-graphite">
          {analysis
            ? analysis.summary
            : "系统会结合后端项目、模块资产与上传简历，生成岗位匹配、JD 适配版简历和模拟面试问答。"}
        </p>
        {assetLoading ? <p className="mt-4 text-sm text-graphite">正在从后端加载经历资产...</p> : null}
        {assetError ? (
          <p className="mt-4 border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-700">
            后端数据暂时不可用：{assetError}
          </p>
        ) : null}
        {analysis ? (
          <div className="mt-5 flex flex-wrap gap-2">
            {analysis.keywords.map((keyword) => (
              <span key={keyword} className="chapter-tag">
                {keyword}
              </span>
            ))}
          </div>
        ) : null}
        <div className="mt-6 grid gap-4">
          <div className="surface p-4">
            <h3 className="font-semibold text-ink">推荐项目顺序</h3>
            <ol className="mt-3 space-y-3 text-sm text-graphite">
              {orderedProjects.length > 0 ? orderedProjects.map((project) => {
                const recommendation = analysis?.projectRecommendations.find((item) => item.slug === project.slug);
                return (
                  <li key={project.slug}>
                    <span className="font-medium text-ink">{project.name}</span>
                    {recommendation ? <p className="mt-1 leading-6">{recommendation.emphasis}</p> : null}
                  </li>
                );
              }) : <li>等待数据库项目资产加载。</li>}
            </ol>
          </div>
          <div className="surface p-4">
            <h3 className="font-semibold text-ink">推荐 Demo</h3>
            <div className="mt-3 grid gap-2">
              {orderedModules.length > 0 ? orderedModules.map((module) => {
                const recommendation = analysis?.moduleRecommendations.find((item) => item.slug === module.slug);
                return (
                  <div key={module.slug} className="border border-line p-3">
                    <p className="text-sm font-medium text-ink">{module.title}</p>
                    <p className="mt-1 text-xs leading-5 text-graphite">{recommendation?.reason ?? module.summary}</p>
                  </div>
                );
              }) : <p className="text-sm text-graphite">等待数据库模块资产加载。</p>}
            </div>
          </div>
          {analysis?.riskNotes.length ? (
            <div className="surface p-4">
              <h3 className="font-semibold text-ink">风险提示</h3>
              <ul className="mt-3 space-y-2 text-sm text-graphite">
                {analysis.riskNotes.map((note) => (
                  <li key={note}>- {note}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {analysis?.resumeOptimizations.length ? (
            <div className="surface p-4">
              <h3 className="font-semibold text-ink">JD 适配建议</h3>
              <ul className="mt-3 space-y-2 text-sm leading-6 text-graphite">
                {analysis.resumeOptimizations.map((item) => (
                  <li key={item}>- {item}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {resumeOptimization ? (
            <div className="surface p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h3 className="font-semibold text-ink">JD 适配版简历</h3>
                  <p className="mt-1 text-xs text-graphite">{resumeOptimization.provider}</p>
                </div>
                <button type="button" onClick={copyGeneratedResume} className="secondary-action inline-flex items-center gap-2 px-4 py-2 text-sm font-medium">
                  <Clipboard size={15} />
                  {copyMessage || "复制 Markdown"}
                </button>
              </div>
              <p className="mt-4 text-sm leading-7 text-graphite">{resumeOptimization.rewrittenSummary}</p>
              <pre className="mt-4 max-h-[34rem] overflow-auto whitespace-pre-wrap border border-line bg-stonepaper p-4 text-sm leading-7 text-ink">
                {resumeOptimization.generatedResumeMarkdown}
              </pre>
              {resumeOptimization.riskNotes.length ? (
                <ul className="mt-3 space-y-2 text-sm leading-6 text-graphite">
                  {resumeOptimization.riskNotes.map((item) => (
                    <li key={item}>- {item}</li>
                  ))}
                </ul>
              ) : null}
            </div>
          ) : null}
          {mockInterview ? (
            <div className="surface p-4">
              <h3 className="font-semibold text-ink">模拟面试问答</h3>
              <p className="mt-2 text-sm leading-6 text-graphite">{mockInterview.closingAdvice}</p>
              <div className="mt-4 grid gap-3">
                {mockInterview.questions.map((item, index) => (
                  <article key={`${item.question}-${index}`} className="border border-line bg-white p-4">
                    <p className="font-mono text-xs uppercase tracking-[0.16em] text-accent">Question {index + 1}</p>
                    <h4 className="mt-2 text-base font-semibold text-ink">{item.question}</h4>
                    {item.intent ? <p className="mt-2 text-sm leading-6 text-graphite">{item.intent}</p> : null}
                    <p className="mt-3 text-sm leading-7 text-ink">{item.strongAnswer}</p>
                    {item.followUps.length ? (
                      <div className="mt-3">
                        <p className="text-xs font-semibold uppercase tracking-[0.16em] text-graphite">追问</p>
                        <ul className="mt-2 space-y-1 text-sm leading-6 text-graphite">
                          {item.followUps.map((followUp) => (
                            <li key={followUp}>- {followUp}</li>
                          ))}
                        </ul>
                      </div>
                    ) : null}
                    {item.scoreFocus.length ? (
                      <div className="mt-3 flex flex-wrap gap-2">
                        {item.scoreFocus.map((focus) => (
                          <span key={focus} className="chapter-tag">
                            {focus}
                          </span>
                        ))}
                      </div>
                    ) : null}
                  </article>
                ))}
              </div>
            </div>
          ) : null}
          {analysis?.interviewTalkingPoints.length ? (
            <div className="surface p-4">
              <h3 className="font-semibold text-ink">面试讲解重点</h3>
              <ul className="mt-3 space-y-2 text-sm leading-6 text-graphite">
                {analysis.interviewTalkingPoints.map((item) => (
                  <li key={item}>- {item}</li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      </Surface>
    </section>
  );
}

function resumeTextFromDraft(draft: ResumeDraftView) {
  const basic = draft.basicInfo;
  const sectionText = Object.entries(draft.sections)
    .map(([type, items]) => {
      const content = items
        .map((item) => [
          item.title,
          item.subtitle,
          item.period,
          item.summary,
          item.detail,
          item.tags.join("、"),
        ].filter(Boolean).join("\n"))
        .join("\n\n");
      return `${type}\n${content}`;
    })
    .join("\n\n");
  return [
    basic.name,
    basic.title,
    basic.summary,
    basic.email,
    basic.education,
    sectionText,
  ].filter(Boolean).join("\n");
}

async function resumeTextFromFile(file: File) {
  if (isTextResumeFile(file)) {
    return file.text();
  }
  const extracted = await apiPost<ResumeExtractedText>("/admin/resume/uploads/extract-text", {
    filename: file.name,
    contentType: file.type || "application/octet-stream",
    contentBase64: await fileToBase64(file),
  });
  return extracted.rawText;
}

function isTextResumeFile(file: File) {
  const name = file.name.toLowerCase();
  return file.type.startsWith("text/")
    || name.endsWith(".txt")
    || name.endsWith(".md")
    || name.endsWith(".markdown");
}

async function fileToBase64(file: File) {
  const buffer = await file.arrayBuffer();
  const bytes = new Uint8Array(buffer);
  let binary = "";
  const chunkSize = 0x8000;
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize));
  }
  return window.btoa(binary);
}
