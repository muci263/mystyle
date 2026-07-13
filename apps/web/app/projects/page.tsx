import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { EditorialBackdrop, PageHeader, SiteShell } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { Project } from "@/lib/api";

export default async function ProjectsPage() {
  const projects = await apiGet<Project[]>("/public/projects");

  return (
    <SiteShell>
      <PageHeader
        eyebrow="Case Studies"
        title="项目案例系统"
        description="每个项目都按照背景、职责、技术栈、证据链和可演示模块组织，服务于面试讲解，而不是普通项目列表。"
      />

      <EditorialBackdrop word="PROJECTS" className="pb-20 pt-10">
        <div className="editorial-shelf-intro">
          <div>
            <p className="eyebrow">Case Studies</p>
            <h2 className="display-light mt-4 text-4xl leading-none text-ink md:text-6xl">项目陈列台</h2>
          </div>
          <p>每个项目都按“角色、技术、指标、证据链”组织，视觉上像工程资产货架，而不是普通卡片列表。</p>
        </div>

        <div className="project-shelf-grid">
          {projects.map((project) => (
            <article key={project.slug} className="project-shelf-card">
              <div className="project-shelf-card-main">
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
              </div>
              <div className="project-shelf-card-meta">
                <div className="grid gap-2 font-mono text-xs text-accent">
                  {project.metrics.map((metric) => (
                    <span key={metric}>{metric}</span>
                  ))}
                </div>
                <Link href={`/projects/${project.slug}`} className="primary-action mt-8 px-5 py-2.5 text-sm font-medium">
                  查看证据链 <ArrowUpRight size={15} />
                </Link>
              </div>
            </article>
          ))}
        </div>
      </EditorialBackdrop>
    </SiteShell>
  );
}
