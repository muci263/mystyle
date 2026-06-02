import { PageHeader, SiteShell, Surface } from "@/components/site-shell";

export default function ArchitecturePage() {
  return (
    <SiteShell>
      <PageHeader
        eyebrow="Architecture"
        title="系统架构与 CI/CD"
        description="本页用于面试中展示项目自身的工程链路：前后端分离、Spring Boot API、MySQL、Redis、LLM Provider、Docker、Nginx 与 Jenkins。"
      />

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 pt-10 md:px-8 lg:grid-cols-3">
        {[
          ["Frontend", "Next.js App Router / Tailwind / 可扩展页面组件"],
          ["Backend", "Spring Boot / 统一响应 / 异常处理 / Swagger / 后续接入 MySQL Redis"],
          ["DevOps", "Docker Compose / Nginx 反代 / Jenkins Pipeline / Health Check"],
        ].map(([title, text]) => (
          <Surface key={title}>
            <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">{title}</p>
            <p className="mt-5 leading-7 text-graphite">{text}</p>
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
