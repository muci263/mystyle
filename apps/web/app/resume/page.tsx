import { PageHeader, SiteShell, Surface } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { ResumeView } from "@/lib/api";

export default async function ResumePage() {
  const resume = await apiGet<ResumeView>("/public/resume");
  const { experiences, profile, skills, projects } = resume;

  return (
    <SiteShell>
      <PageHeader
        eyebrow="Structured Resume"
        title="在线简历与经历资产库"
        description="这里展示真实经历的结构化版本。后续新增实习、项目或技能时，页面会从数据层自动读取，而不是重写页面。"
      />

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 pt-10 md:px-8 lg:grid-cols-[0.9fr_1.1fr]">
        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Profile</p>
          <h2 className="display-light mt-5 text-4xl text-ink">{profile.name}</h2>
          <p className="mt-2 text-lg text-graphite">{profile.title}</p>
          <p className="mt-5 leading-7 text-graphite">{profile.summary}</p>
          <div className="mt-6 space-y-2 text-sm text-graphite">
            <p>教育背景：{profile.education}</p>
            <p>邮箱：{profile.email}</p>
            <p>公开页面默认不展示完整手机号，PDF 简历中可保留。</p>
          </div>
        </Surface>

        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Skills</p>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            {skills.map((group) => (
              <div key={group.category} className="border border-line p-4">
                <h3 className="font-semibold text-ink">{group.category}</h3>
                <div className="mt-3 flex flex-wrap gap-2">
                  {group.items.map((item) => (
                    <span key={item} className="chapter-tag">
                      {item}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Surface>

        <Surface className="lg:col-span-2">
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Experience</p>
          <div className="mt-5 space-y-6">
            {experiences.map((item) => (
              <div key={item.company} className="border-l-2 border-accent pl-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                <h3 className="display-light text-3xl text-ink">{item.company}</h3>
                  <span className="font-mono text-xs text-accent">{item.period}</span>
                </div>
                <p className="mt-2 text-graphite">{item.position}</p>
                <ul className="mt-4 grid gap-2 text-sm leading-6 text-graphite md:grid-cols-2">
                  {item.highlights.map((highlight) => (
                    <li key={highlight}>- {highlight}</li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </Surface>

        <Surface className="lg:col-span-2">
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Project Snapshot</p>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            {projects.map((project) => (
              <div key={project.slug} className="surface p-5">
                <h3 className="display-light text-2xl text-ink">{project.name}</h3>
                <p className="mt-3 text-sm leading-6 text-graphite">{project.summary}</p>
                <div className="mt-4 flex flex-wrap gap-2">
                  {project.tech.map((tech) => (
                    <span key={tech} className="chapter-tag">
                      {tech}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </Surface>
      </section>
    </SiteShell>
  );
}
