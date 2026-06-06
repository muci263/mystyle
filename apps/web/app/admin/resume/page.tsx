import { SiteShell } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { ResumeDraftView, ResumeVersion } from "@/lib/api";
import { ResumeAdminClient } from "./resume-admin-client";

export default async function ResumeAdminPage() {
  const [draft, versions] = await Promise.all([
    apiGet<ResumeDraftView>("/admin/resume/draft"),
    apiGet<ResumeVersion[]>("/admin/resume/versions"),
  ]);

  return (
    <SiteShell>
      <ResumeAdminClient initialDraft={draft} initialVersions={versions} />
    </SiteShell>
  );
}
