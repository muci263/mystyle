import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { PageHeader, SiteShell, Surface } from "@/components/site-shell";
import { apiGet, Project } from "@/lib/api";

export default async function ProjectsPage() {
  const projects = await apiGet<Project[]>("/public/projects");

  return (
    <SiteShell>
      <PageHeader
        eyebrow="Case Studies"
        title="项目案例系统"
        description="每个项目都按照背景、职责、技术栈、证据链和可演示模块组织，服务于面试讲解，而不是普通项目列表。"
      />

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 pt-10 md:grid-cols-2 md:px-8">
        {projects.map((project) => (
          <Surface key={project.slug}>
            <span className="font-mono text-sm text-accent">{project.index}</span>
            <h2 className="display-light mt-6 text-4xl leading-tight text-ink">{project.name}</h2>
            <p className="mt-3 text-sm text-graphite">{project.role}</p>
            <p className="mt-5 leading-7 text-graphite">{project.summary}</p>
            <div className="mt-5 flex flex-wrap gap-2">
              {project.tech.map((item) => (
                <span key={item} className="chapter-tag">
                  {item}
                </span>
              ))}
            </div>
            <div className="mt-5 flex flex-wrap gap-3 font-mono text-xs text-accent">
              {project.metrics.map((metric) => (
                <span key={metric}>{metric}</span>
              ))}
            </div>
            <Link href={`/projects/${project.slug}`} className="primary-action mt-8 px-5 py-2.5 text-sm font-medium">
              查看证据链 <ArrowUpRight size={15} />
            </Link>
          </Surface>
        ))}
      </section>
    </SiteShell>
  );
}
