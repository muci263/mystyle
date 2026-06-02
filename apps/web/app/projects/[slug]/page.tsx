import Link from "next/link";
import { notFound } from "next/navigation";
import { PageHeader, SiteShell, Surface } from "@/components/site-shell";
import { apiGet, ModuleDemo, Project } from "@/lib/api";

export default async function ProjectDetailPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const project = await getProject(slug);
  if (!project) {
    notFound();
  }

  const labModules = await apiGet<ModuleDemo[]>("/public/module-demos");
  const relatedModules = labModules.filter((module) => module.project === project.name);

  return (
    <SiteShell>
      <PageHeader eyebrow="Project Evidence" title={project.name} description={project.summary} />

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 pt-10 md:px-8 lg:grid-cols-[0.85fr_1.15fr]">
        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Role</p>
          <h2 className="display-light mt-5 text-3xl text-ink">{project.role}</h2>
          <div className="mt-5 flex flex-wrap gap-2">
            {project.tech.map((tech) => (
              <span key={tech} className="chapter-tag">
                {tech}
              </span>
            ))}
          </div>
          <div className="mt-6 space-y-2 font-mono text-xs text-accent">
            {project.metrics.map((metric) => (
              <p key={metric}>{metric}</p>
            ))}
          </div>
        </Surface>

        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Responsibilities</p>
          <ul className="mt-5 space-y-3 text-sm leading-6 text-graphite">
            {project.responsibilities.map((item) => (
              <li key={item}>- {item}</li>
            ))}
          </ul>
        </Surface>

        <Surface className="lg:col-span-2">
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Evidence Chain</p>
          <div className="mt-5 grid gap-4">
            {project.evidence.map((item) => (
              <div key={item.problem} className="grid gap-7 border-t border-line py-7 first:border-t-0 first:pt-0 md:grid-cols-3">
                <div>
                  <h3 className="font-semibold text-ink">业务问题</h3>
                  <p className="mt-3 text-sm leading-6 text-graphite">{item.problem}</p>
                </div>
                <div>
                  <h3 className="font-semibold text-ink">解决方案</h3>
                  <p className="mt-3 text-sm leading-6 text-graphite">{item.solution}</p>
                </div>
                <div>
                  <h3 className="font-semibold text-ink">结果指标</h3>
                  <p className="mt-3 text-sm leading-6 text-graphite">{item.result}</p>
                </div>
              </div>
            ))}
          </div>
        </Surface>

        <Surface className="lg:col-span-2">
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Related Demos</p>
          <div className="mt-5 grid gap-4 md:grid-cols-3">
            {relatedModules.length > 0 ? (
              relatedModules.map((module) => (
                <Link key={module.slug} href={`/lab/${module.slug}`} className="surface surface-hover p-5">
                  <h3 className="font-semibold text-ink">{module.title}</h3>
                  <p className="mt-3 text-sm leading-6 text-graphite">{module.summary}</p>
                </Link>
              ))
            ) : (
              <p className="text-sm text-graphite">该项目的可交互 Demo 会在后续迭代中补充。</p>
            )}
          </div>
        </Surface>
      </section>
    </SiteShell>
  );
}

async function getProject(slug: string) {
  try {
    return await apiGet<Project>(`/public/projects/${slug}`);
  } catch {
    return null;
  }
}
