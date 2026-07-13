import { JdAdapterClient } from "@/app/jd-adapter/jd-adapter-client";
import { PageHeader, SiteShell } from "@/components/site-shell";

export default function InterviewKitPage() {
  return (
    <SiteShell>
      <PageHeader
        eyebrow="Interview Kit"
        title="模拟面试工作台"
        description="JD 适配、问答演练、简历草稿。"
      />
      <JdAdapterClient />
    </SiteShell>
  );
}
