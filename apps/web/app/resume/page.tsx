import Link from "next/link";
import { PageHeader, SiteShell, Surface } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { ResumeDraftView, ResumeSectionItem, ResumeSectionType } from "@/lib/api";

const sectionMeta: Array<{ type: ResumeSectionType; eyebrow: string; title: string }> = [
  { type: "SKILL", eyebrow: "Skill System", title: "技术能力" },
  { type: "AWARD", eyebrow: "Awards", title: "获奖经历" },
  { type: "INTERNSHIP", eyebrow: "Internship", title: "实习经历" },
  { type: "PROJECT", eyebrow: "Projects", title: "项目经历" },
  { type: "ADVANTAGE", eyebrow: "Advantages", title: "个人优势" },
];

export default async function ResumePage() {
  const resume = await apiGet<ResumeDraftView>("/public/resume-content");
  const { basicInfo, sections, version } = resume;

  return (
    <SiteShell>
      <PageHeader
        eyebrow="Structured Resume"
        title="在线简历"
        description="结构化展示个人信息、能力、经历与项目证明。"
      />

      <section className="resume-content-stage">
        <div className="mx-auto grid max-w-7xl gap-5 px-5 pb-20 pt-10 md:px-8 lg:grid-cols-[0.82fr_1.18fr]">
          <Surface className="resume-profile-card">
            <div className="flex flex-wrap items-start justify-between gap-5">
              <div>
                <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Profile</p>
                <h2 className="display-light mt-5 text-5xl leading-none text-ink md:text-6xl">{basicInfo.name}</h2>
                <p className="mt-3 text-xl text-graphite">{basicInfo.title}</p>
              </div>
              <span className="metric-pill">{version.status}</span>
            </div>
            <p className="mt-7 leading-8 text-graphite">{basicInfo.summary}</p>
            <div className="mt-8 grid gap-3 text-sm text-graphite sm:grid-cols-2">
              <InfoLine label="教育" value={basicInfo.education} />
              <InfoLine label="城市" value={basicInfo.location || "待补充"} />
              <InfoLine label="邮箱" value={basicInfo.email} />
              <InfoLine label="电话" value={maskPhone(basicInfo.phone)} />
            </div>
            <div className="mt-8 flex flex-wrap gap-3">
              {basicInfo.githubUrl ? (
                <Link href={basicInfo.githubUrl} className="secondary-action px-4 py-2.5 text-sm">
                  GitHub
                </Link>
              ) : null}
              {basicInfo.websiteUrl ? (
                <Link href={basicInfo.websiteUrl} className="secondary-action px-4 py-2.5 text-sm">
                  Website
                </Link>
              ) : null}
              <Link href="/admin/resume" className="primary-action px-4 py-2.5 text-sm">
                编辑履历
              </Link>
            </div>
          </Surface>

          <Surface className="resume-index-card">
            <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Resume Map</p>
            <div className="mt-5 grid gap-3">
              {sectionMeta.map((meta, index) => {
                const count = sections[meta.type]?.length ?? 0;
                return (
                  <a key={meta.type} href={`#${meta.type.toLowerCase()}`} className="resume-map-row">
                    <span className="font-mono text-[10px] text-accent">{String(index + 1).padStart(2, "0")}</span>
                    <span>{meta.title}</span>
                    <span>{count}</span>
                  </a>
                );
              })}
            </div>
            <p className="mt-6 font-mono text-[10px] uppercase tracking-[0.2em] text-graphite">
              Updated / {version.updatedAt}
            </p>
          </Surface>

          {sectionMeta.map((meta) => (
            <ResumeSection key={meta.type} meta={meta} items={sections[meta.type] ?? []} />
          ))}
        </div>
      </section>
    </SiteShell>
  );
}

function ResumeSection({
  meta,
  items,
}: {
  meta: { type: ResumeSectionType; eyebrow: string; title: string };
  items: ResumeSectionItem[];
}) {
  return (
    <Surface id={meta.type.toLowerCase()} className="resume-section-card lg:col-span-2">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">{meta.eyebrow}</p>
          <h2 className="display-light mt-4 text-4xl text-ink md:text-5xl">{meta.title}</h2>
        </div>
        <span className="metric-pill">{items.length} items</span>
      </div>
      {items.length === 0 ? (
        <p className="mt-8 text-sm text-graphite">暂无公开条目。</p>
      ) : (
        <div className="mt-8 grid gap-4 md:grid-cols-2">
          {items.map((item) => (
            <article key={item.id} className="resume-entry-card">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-accent">
                    {item.period || item.subtitle || meta.title}
                  </p>
                  <h3 className="mt-3 text-2xl font-semibold leading-tight text-ink">{item.title}</h3>
                </div>
                <span className="font-mono text-[10px] text-graphite">
                  {String(item.sortOrder).padStart(2, "0")}
                </span>
              </div>
              {item.summary ? <p className="mt-4 text-sm leading-6 text-graphite">{item.summary}</p> : null}
              {detailLines(item).length > 0 ? (
                <ul className="mt-5 grid gap-2 text-sm leading-6 text-graphite">
                  {detailLines(item).map((line) => (
                    <li key={line} className="resume-detail-line">
                      {line}
                    </li>
                  ))}
                </ul>
              ) : null}
              {item.tags.length > 0 ? (
                <div className="mt-5 flex flex-wrap gap-2">
                  {item.tags.map((tag) => (
                    <span key={tag} className="chapter-tag">
                      {tag}
                    </span>
                  ))}
                </div>
              ) : null}
            </article>
          ))}
        </div>
      )}
    </Surface>
  );
}

function InfoLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="resume-info-line">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function detailLines(item: ResumeSectionItem) {
  return item.detail
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line && line !== item.title && line !== item.summary);
}

function maskPhone(phone: string) {
  if (!phone) {
    return "公开页隐藏";
  }
  if (phone.length < 7) {
    return "公开页隐藏";
  }
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`;
}
