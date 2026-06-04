import Link from "next/link";
import { PageHeader, SiteShell, Surface } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { InterviewGuide } from "@/lib/api";

export default async function InterviewPage() {
  const interviewGuide = await apiGet<InterviewGuide>("/public/interview");

  return (
    <SiteShell>
      <PageHeader
        eyebrow="Interview Mode"
        title="面试模式"
        description="把自我介绍、项目讲解顺序、可打开 Demo 和追问问题集中到一个页面，面试前和面试中都可以快速使用。"
      />

      <section className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 pt-10 md:px-8 lg:grid-cols-[1fr_1fr]">
        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">30s Intro</p>
          <p className="display-light mt-5 text-2xl leading-10 text-ink">{interviewGuide.shortIntro}</p>
        </Surface>

        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Project Order</p>
          <ol className="mt-5 space-y-3">
            {interviewGuide.projectOrder.map((item, index) => (
              <li key={item} className="flex gap-3">
                <span className="font-mono text-accent">{String(index + 1).padStart(2, "0")}</span>
                <span className="text-ink">{item}</span>
              </li>
            ))}
          </ol>
        </Surface>

        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Open During Interview</p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Link href="/projects/mine-education-system" className="secondary-action px-4 py-2 text-sm text-ink">
              矿山教育系统
            </Link>
            <Link href="/lab/video-learning" className="secondary-action px-4 py-2 text-sm text-ink">
              视频学习 Demo
            </Link>
            <Link href="/jd-adapter" className="secondary-action px-4 py-2 text-sm text-ink">
              JD 适配器
            </Link>
          </div>
        </Surface>

        <Surface>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Likely Questions</p>
          <ul className="mt-5 space-y-3 text-sm leading-6 text-graphite">
            {interviewGuide.questions.map((question) => (
              <li key={question}>- {question}</li>
            ))}
          </ul>
        </Surface>
      </section>
    </SiteShell>
  );
}
