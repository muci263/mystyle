import Link from "next/link";
import { ArrowUpRight, Blocks, GitBranch, ServerCog } from "lucide-react";
import { FadeIn } from "@/components/motion-shell";
import { PageHeader, SiteShell, Surface } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { ModuleDemo, Project } from "@/lib/api";

export default async function EvidencePage() {
  const [projects, modules] = await Promise.all([
    apiGet<Project[]>("/public/projects"),
    apiGet<ModuleDemo[]>("/public/module-demos"),
  ]);

  return (
    <SiteShell>
      <PageHeader
        eyebrow="Evidence System"
        title="项目证据与模块演示"
        description="把项目案例、工作模块复现和系统架构整合成一条可讲、可点、可追问的面试证据链。"
      />

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-16 pt-10 md:px-8">
        {projects.map((project, projectIndex) => {
          const relatedModules = modules.filter((module) => isRelated(project, module));

          return (
            <FadeIn key={project.slug} delay={projectIndex * 0.05}>
              <article className="enhanced-surface border border-line bg-surface p-6 md:p-8">
                <div className="grid gap-8 lg:grid-cols-[0.82fr_1.18fr]">
                  <div>
                    <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Case {project.index}</p>
                    <h2 className="display-light mt-5 text-4xl leading-tight text-ink">{project.name}</h2>
                    <p className="mt-3 text-sm font-medium text-graphite">{project.role}</p>
                    <p className="mt-6 leading-7 text-graphite">{project.summary}</p>
                    <div className="mt-5 flex flex-wrap gap-2">
                      {project.tech.map((tech) => (
                        <span key={tech} className="chapter-tag">
                          {tech}
                        </span>
                      ))}
                    </div>
                    <Link href={`/projects/${project.slug}`} className="text-link mt-7 inline-flex items-center gap-2 text-sm font-medium">
                      查看完整证据链 <ArrowUpRight className="text-accent" size={16} />
                    </Link>
                  </div>

                  <div className="grid gap-4">
                    <div className="grid gap-3 md:grid-cols-2">
                      {project.evidence.map((item) => (
                        <div key={item.problem} className="border border-line bg-stonepaper p-4">
                          <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-accent">Problem / Result</p>
                          <p className="mt-3 text-sm leading-6 text-graphite">{item.problem}</p>
                          <p className="mt-3 text-sm font-medium leading-6 text-ink">{item.result}</p>
                        </div>
                      ))}
                      {project.metrics.map((metric) => (
                        <div key={metric} className="border border-line bg-white p-4 font-mono text-sm text-accent">
                          {metric}
                        </div>
                      ))}
                    </div>

                    <div className="border border-line bg-white p-4">
                      <div className="flex items-center gap-2">
                        <Blocks size={16} className="text-accent" />
                        <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Runnable Modules</p>
                      </div>
                      <div className="mt-4 grid gap-3 md:grid-cols-2">
                        {relatedModules.map((module) => (
                          <Link key={module.slug} href={`/lab/${module.slug}`} className="surface-hover border border-line bg-stonepaper p-4">
                            <p className="text-sm font-semibold text-ink">{module.title}</p>
                            <p className="mt-2 text-xs leading-5 text-graphite">{module.summary}</p>
                          </Link>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            </FadeIn>
          );
        })}
      </section>

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 md:px-8 lg:grid-cols-3">
        {[
          ["Frontend", "Next.js App Router / Tailwind / 可扩展页面组件", ServerCog],
          ["Backend", "Spring Boot API / MySQL 内容库 / 统一响应与异常处理", Blocks],
          ["DevOps", "Docker Compose / Nginx 单入口 / Jenkins Pipeline / Health Check", GitBranch],
        ].map(([title, text, Icon]) => (
          <Surface key={String(title)}>
            {Icon && <Icon size={18} className="text-accent" />}
            <p className="mt-5 font-mono text-xs uppercase tracking-[0.2em] text-accent">{String(title)}</p>
            <p className="mt-5 leading-7 text-graphite">{String(text)}</p>
          </Surface>
        ))}

        <Surface className="lg:col-span-3">
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Deployment Flow</p>
          <div className="mt-5 grid gap-3 md:grid-cols-5">
            {["GitHub Push", "Jenkins Build", "Docker Image", "Compose Deploy", "Health Check"].map((step) => (
              <div key={step} className="border border-line bg-stonepaper p-4 text-sm font-medium text-ink">
                {step}
              </div>
            ))}
          </div>
        </Surface>
      </section>
    </SiteShell>
  );
}

function isRelated(project: Project, module: ModuleDemo) {
  return (
    project.name.includes(module.project) ||
    module.project.includes(project.name) ||
    (project.name.includes("矿山") && module.project.includes("矿山")) ||
    (project.name.includes("AI") && module.project.includes("AI"))
  );
}
