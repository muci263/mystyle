import { notFound } from "next/navigation";
import { FadeIn } from "@/components/motion-shell";
import { PageHeader, SiteShell, Surface } from "@/components/site-shell";
import { apiGet, ModuleDemo } from "@/lib/api";

const demoSteps = {
  "video-learning": ["播放视频", "timeupdate 事件", "节流上报进度", "写入 Redis", "完播同步 MySQL", "更新学习状态"],
  "cache-sync": ["高频请求进入", "Redis 聚合写入", "延迟任务触发", "幂等校验", "MySQL 落库", "指标对比"],
  "agent-workflow": ["用户输入", "意图识别", "工具路由", "RAG/SQL 查询", "LLM 生成", "结果校验"],
  "sql-bot": ["自然语言问题", "生成 SQL", "安全校验", "执行查询", "结果解释"],
  "rag-flow": ["文档切块", "向量写入", "Top-K 召回", "上下文组装", "模型回答"],
};

export default async function LabDetailPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const module = await getModule(slug);
  if (!module) {
    notFound();
  }

  const steps = demoSteps[module.slug as keyof typeof demoSteps] ?? ["业务输入", "接口处理", "状态更新", "结果展示"];

  return (
    <SiteShell>
      <PageHeader eyebrow="Module Demo" title={module.title} description={module.summary} />

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 pt-10 md:px-8 lg:grid-cols-[0.9fr_1.1fr]">
        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Business Context</p>
          <h2 className="mt-4 text-2xl font-semibold text-ink">{module.project}</h2>
          <p className="mt-5 leading-7 text-graphite">
            当前页面先实现可扩展的演示外壳和业务链路说明。具体交互会按优先级逐步补充，第一优先级是视频播放学习链路。
          </p>
          <div className="mt-6 flex flex-wrap gap-2">
            {module.tech.map((tech) => (
              <span key={tech} className="chapter-tag">
                {tech}
              </span>
            ))}
          </div>
        </Surface>

        <FadeIn delay={0.05} className="h-full">
          <article className="technical-night console-grid h-full p-6">
            <p className="technical-label font-mono text-xs uppercase tracking-[0.2em]">Flow Visualizer</p>
            <div className="mt-5 grid gap-3">
              {steps.map((step, index) => (
                <div key={step} className="console-card flex items-center gap-4 rounded-md p-3">
                  <span className="technical-label flex h-8 w-8 items-center justify-center bg-white/[0.07] font-mono text-xs">
                    {String(index + 1).padStart(2, "0")}
                  </span>
                  <span className="text-sm font-medium text-white">{step}</span>
                </div>
              ))}
            </div>
          </article>
        </FadeIn>

        <FadeIn delay={0.08} className="lg:col-span-2">
          <article className="technical-night console-grid p-6">
            <div className="flex items-center gap-2">
              <span className="runtime-dot" />
              <p className="technical-label font-mono text-xs uppercase tracking-[0.2em]">Runtime Console</p>
            </div>
            <div className="mt-5 border border-white/10 bg-white/[0.035] p-5 font-mono text-xs leading-7 text-white/72">
              <p>{"> demoType: " + module.demoType}</p>
              <p>{"> sourceProject: " + module.project}</p>
              <p>{"> renderer: planned"}</p>
              <p>{"> next: connect /api/lab/" + module.slug + " when this module enters implementation"}</p>
            </div>
          </article>
        </FadeIn>
      </section>
    </SiteShell>
  );
}

async function getModule(slug: string) {
  try {
    return await apiGet<ModuleDemo>(`/public/module-demos/${slug}`);
  } catch {
    return null;
  }
}
