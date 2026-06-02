import Link from "next/link";
import { ArrowUpRight, CircleDot } from "lucide-react";
import { FadeIn } from "@/components/motion-shell";
import { PageHeader, SiteShell } from "@/components/site-shell";
import { apiGet, ModuleDemo } from "@/lib/api";

export default async function LabPage() {
  const labModules = await apiGet<ModuleDemo[]>("/public/module-demos");

  return (
    <SiteShell>
      <PageHeader
        eyebrow="Work Reproduction Lab"
        title="工作模块复现实验室"
        description="把实习和项目中参与过的关键工作抽象成可交互 Demo。后续新增经历时，只需要补充 ModuleDemo 元数据和必要 Renderer。"
      />

      <section className="technical-night console-grid mx-auto mb-20 max-w-7xl px-5 py-9 md:px-8 md:py-12">
        <FadeIn>
          <div className="mb-8 flex flex-col justify-between gap-5 border-b border-white/10 pb-7 md:flex-row md:items-center">
            <div className="flex items-center gap-3 text-sm text-white/65">
              <CircleDot className="technical-label" size={16} />
              <span>Runtime modules ready for interview walkthrough</span>
            </div>
            <div className="technical-label flex gap-2 font-mono text-xs">
              <span className="rounded-md border border-white/10 px-3 py-2">5 MODULES</span>
              <span className="rounded-md border border-white/10 px-3 py-2">EXTENSIBLE</span>
            </div>
          </div>
        </FadeIn>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {labModules.map((module, index) => (
            <FadeIn key={module.slug} className="h-full" delay={index * 0.045}>
              <article className="console-card flex h-full min-h-[310px] flex-col p-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="runtime-dot" />
                    <p className="technical-label font-mono text-xs uppercase tracking-[0.2em]">{module.demoType}</p>
                  </div>
                  <span className="font-mono text-xs text-white/40">{String(index + 1).padStart(2, "0")}</span>
                </div>
                <h2 className="display-light mt-8 text-2xl text-white">{module.title}</h2>
                <p className="mt-2 text-sm text-white/48">来源项目：{module.project}</p>
                <p className="mt-5 flex-1 text-sm leading-6 text-white/66">{module.summary}</p>
                <div className="mt-6 flex flex-wrap gap-2">
                  {module.tech.slice(0, 3).map((tech) => (
                    <span key={tech} className="rounded-full border border-white/14 px-3 py-1.5 text-xs text-white/62">
                      {tech}
                    </span>
                  ))}
                </div>
                <Link href={`/lab/${module.slug}`} className="mt-7 inline-flex items-center gap-2 text-sm font-medium text-white">
                  打开演示 <ArrowUpRight className="technical-label" size={16} />
                </Link>
              </article>
            </FadeIn>
          ))}
        </div>
      </section>
    </SiteShell>
  );
}
