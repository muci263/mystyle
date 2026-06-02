"use client";

import { FormEvent, useMemo, useState } from "react";
import { apiPost, JdAnalysisResponse, ModuleDemo, Project } from "@/lib/api";
import { Surface } from "@/components/site-shell";

const defaultJd = "Java 后端实习，要求 Spring Boot、Redis、MySQL、微服务，有 AI 或 RAG 经验加分";

export function JdAdapterClient({ projects, modules }: { projects: Project[]; modules: ModuleDemo[] }) {
  const [jd, setJd] = useState(defaultJd);
  const [analysis, setAnalysis] = useState<JdAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

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
    setLoading(true);
    setError("");
    try {
      const result = await apiPost<JdAnalysisResponse>("/jd/analyze", {
        jd,
        variantName: "前端联调生成版本",
      });
      setAnalysis(result);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "岗位分析失败");
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
          <button disabled={loading || jd.trim().length === 0} className="primary-action mt-5 px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55">
            {loading ? "分析中..." : "分析岗位匹配"}
          </button>
          {error ? <p className="mt-4 text-sm text-red-600">{error}</p> : null}
        </form>
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
              {orderedProjects.map((project) => {
                const recommendation = analysis?.projectRecommendations.find((item) => item.slug === project.slug);
                return (
                  <li key={project.slug}>
                    <span className="font-medium text-ink">{project.name}</span>
                    {recommendation ? <p className="mt-1 leading-6">{recommendation.emphasis}</p> : null}
                  </li>
                );
              })}
            </ol>
          </div>
          <div className="surface p-4">
            <h3 className="font-semibold text-ink">推荐 Demo</h3>
            <div className="mt-3 grid gap-2">
              {orderedModules.map((module) => {
                const recommendation = analysis?.moduleRecommendations.find((item) => item.slug === module.slug);
                return (
                  <div key={module.slug} className="border border-line p-3">
                    <p className="text-sm font-medium text-ink">{module.title}</p>
                    <p className="mt-1 text-xs leading-5 text-graphite">{recommendation?.reason ?? module.summary}</p>
                  </div>
                );
              })}
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
        </div>
      </Surface>
    </section>
  );
}
