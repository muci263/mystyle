"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { LlmProgressPanel, useLlmProgress } from "@/components/llm-progress";
import { apiGet, apiPost } from "@/lib/api";
import type { JdAnalysisResponse, ModuleDemo, Project, ResumeDraftView, ResumeOptimizeResponse } from "@/lib/api";
import { Surface } from "@/components/site-shell";

const defaultJd = "Java 后端实习，要求 Spring Boot、Redis、MySQL、微服务，有 AI 或 RAG 经验加分";

export function JdAdapterClient() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [modules, setModules] = useState<ModuleDemo[]>([]);
  const [jd, setJd] = useState(defaultJd);
  const [analysis, setAnalysis] = useState<JdAnalysisResponse | null>(null);
  const [resumeOptimization, setResumeOptimization] = useState<ResumeOptimizeResponse | null>(null);
  const [assetLoading, setAssetLoading] = useState(true);
  const [assetError, setAssetError] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
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

  async function runAnalysis(allowFallback: boolean) {
    setLoading(true);
    setError("");
    llmProgress.start(allowFallback ? "规则兜底 JD 适配" : "JD 适配与简历优化", [
      { id: "contract", label: "输入输出契约", detail: "约定 JD、项目资产、模块资产和简历草稿的结构化字段。" },
      { id: "jd", label: "岗位匹配分析", detail: "Minimax 正在生成岗位定位、关键词、项目排序和风险提示。" },
      { id: "draft", label: "读取简历草稿", detail: "拉取当前草稿并组装真实简历文本。" },
      { id: "resume", label: "简历适配优化", detail: "Minimax 基于 JD 和真实草稿生成改写建议。" },
      { id: "render", label: "结果呈现", detail: "渲染推荐项目、Demo、面试重点和简历建议。" },
    ]);
    try {
      llmProgress.setStep("contract", "done", "输入和输出已限定为项目资产、模块资产、草稿简历和 JSON 建议。");
      llmProgress.activate("jd", allowFallback ? "用户已确认使用规则降级，本次不调用 Minimax。" : "正在分析岗位关键词、匹配度和证据资产。");
      const result = await apiPost<JdAnalysisResponse>("/jd/analyze", {
        jd,
        variantName: "前端联调生成版本",
        allowFallback,
      });
      setAnalysis(result);
      llmProgress.activate("draft", "正在读取当前简历草稿。");
      const draft = await apiGet<ResumeDraftView>("/admin/resume/draft");
      const resumeText = resumeTextFromDraft(draft);
      llmProgress.activate("resume", allowFallback ? "用户已确认使用规则降级，本次不调用 Minimax。" : "正在生成 JD 适配的简历优化建议。");
      const optimize = await apiPost<ResumeOptimizeResponse>("/llm/resume/optimize", {
        jdText: jd,
        resumeText,
        targetRole: result.role,
        assetHints: [
          ...result.projectRecommendations.slice(0, 3).map((item) => item.name),
          ...result.moduleRecommendations.slice(0, 2).map((item) => item.title),
        ],
        allowFallback,
      });
      setResumeOptimization(optimize);
      llmProgress.activate("render", "正在刷新推荐顺序和改写草案。");
      llmProgress.complete(`完成：${result.role}，匹配度 ${result.matchScore}，已生成简历优化草案。`);
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
            className="mt-3 min-h-56 w-full resize-none border border-line bg-stonepaper p-4 text-sm leading-7 text-ink outline-none placeholder:text-graphite focus:border-accent"
            placeholder="粘贴 Java 后端、AI 应用或全栈岗位 JD..."
          />
          <div className="mt-5 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => runAnalysis(true)}
              disabled={loading || jd.trim().length === 0}
              className="secondary-action px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55"
            >
              规则兜底分析
            </button>
            <button disabled={loading || jd.trim().length === 0} className="primary-action px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55">
              {loading ? "分析中..." : "分析岗位匹配"}
            </button>
          </div>
          {error ? <p className="mt-4 text-sm text-red-600">{error}</p> : null}
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
            : "右侧内容会根据后端 MySQL 中的项目与模块资产重排，点击分析后由后端 JD Provider 返回结构化结果。"}
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
              <h3 className="font-semibold text-ink">JD 适配简历建议</h3>
              <ul className="mt-3 space-y-2 text-sm leading-6 text-graphite">
                {analysis.resumeOptimizations.map((item) => (
                  <li key={item}>- {item}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {resumeOptimization ? (
            <div className="surface p-4">
              <h3 className="font-semibold text-ink">Minimax 简历改写草案</h3>
              <p className="mt-3 text-sm leading-7 text-graphite">{resumeOptimization.rewrittenSummary}</p>
              {resumeOptimization.sectionSuggestions.length ? (
                <ul className="mt-3 space-y-2 text-sm leading-6 text-graphite">
                  {resumeOptimization.sectionSuggestions.map((item) => (
                    <li key={item}>- {item}</li>
                  ))}
                </ul>
              ) : null}
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
