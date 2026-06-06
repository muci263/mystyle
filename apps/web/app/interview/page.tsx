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
        description="把自我介绍、项目讲解顺序和追问问题集中到一个页面，面试前和面试中都可以快速使用。"
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
