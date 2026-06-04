import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import { JdAdapterClient } from "@/app/jd-adapter/jd-adapter-client";
import { PageHeader, SiteShell, Surface } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { InterviewGuide } from "@/lib/api";

export default async function InterviewKitPage() {
  const interviewGuide = await apiGet<InterviewGuide>("/public/interview");

  return (
    <SiteShell>
      <PageHeader
        eyebrow="Interview Kit"
        title="岗位适配与面试讲解"
        description="先用 JD 分析重排项目重点，再用面试叙事框架组织自我介绍、项目顺序、演示入口和高频追问。"
      />

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pt-10 md:px-8 lg:grid-cols-[1.08fr_0.92fr]">
        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">30s Intro</p>
          <p className="display-light mt-5 text-2xl leading-10 text-ink">{interviewGuide.shortIntro}</p>
          <div className="mt-7 flex flex-wrap gap-3">
            {interviewGuide.openLinks.map((href) => (
              <Link key={href} href={href} className="secondary-action px-4 py-2 text-sm text-ink">
                打开 {labelFor(href)} <ArrowUpRight size={14} />
              </Link>
            ))}
          </div>
        </Surface>

        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Narrative Order</p>
          <ol className="mt-5 space-y-3">
            {interviewGuide.projectOrder.map((item, index) => (
              <li key={item} className="flex gap-3">
                <span className="font-mono text-accent">{String(index + 1).padStart(2, "0")}</span>
                <span className="text-ink">{item}</span>
              </li>
            ))}
          </ol>
        </Surface>

        <Surface className="lg:col-span-2">
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Likely Questions</p>
          <div className="mt-5 grid gap-3 md:grid-cols-3">
            {interviewGuide.questions.map((question) => (
              <div key={question} className="border border-line bg-stonepaper p-4 text-sm leading-6 text-graphite">
                {question}
              </div>
            ))}
          </div>
        </Surface>
      </section>

      <JdAdapterClient />
    </SiteShell>
  );
}

function labelFor(href: string) {
  if (href.includes("lab")) return "模块演示";
  if (href.includes("jd-adapter")) return "JD 适配";
  if (href.includes("projects")) return "项目证据";
  if (href.includes("evidence")) return "证据系统";
  return "页面";
}
