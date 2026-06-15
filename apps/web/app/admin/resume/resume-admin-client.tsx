"use client";

import { FormEvent, useMemo, useState } from "react";
import { CheckCircle2, Eye, EyeOff, Loader2, Plus, RefreshCw, Rocket, Save, Trash2, UploadCloud } from "lucide-react";
import { LlmProgressPanel, useLlmProgress } from "@/components/llm-progress";
import { apiDelete, apiGet, apiPost, apiPut } from "@/lib/api";
import type {
  ResumeBasicInfo,
  ResumeDraftView,
  ResumeSectionItem,
  ResumeSectionType,
  ResumeUploadTask,
  ResumeVersion,
} from "@/lib/api";

const sections: Array<{ type: ResumeSectionType; label: string; hint: string }> = [
  { type: "SKILL", label: "技术能力", hint: "语言、框架、中间件、AI 工程化能力" },
  { type: "AWARD", label: "获奖经历", hint: "奖学金、竞赛、荣誉" },
  { type: "INTERNSHIP", label: "实习经历", hint: "公司、岗位、职责、成果" },
  { type: "PROJECT", label: "项目经历", hint: "项目名、角色、技术点、证明指标" },
  { type: "ADVANTAGE", label: "个人优势", hint: "表达、工程习惯、学习能力" },
];

const emptyItem = {
  title: "",
  subtitle: "",
  period: "",
  summary: "",
  detail: "",
  tags: "",
  visible: true,
  sortOrder: 1,
};

type ItemForm = typeof emptyItem;

type ResumeAdminClientProps = {
  initialDraft: ResumeDraftView;
  initialVersions: ResumeVersion[];
};

export function ResumeAdminClient({ initialDraft, initialVersions }: ResumeAdminClientProps) {
  const [draft, setDraft] = useState(initialDraft);
  const [versions, setVersions] = useState(initialVersions);
  const [activeSection, setActiveSection] = useState<ResumeSectionType>("SKILL");
  const [basicInfo, setBasicInfo] = useState(initialDraft.basicInfo);
  const [itemForm, setItemForm] = useState<ItemForm>({ ...emptyItem });
  const [editingItemId, setEditingItemId] = useState<number | null>(null);
  const [uploadText, setUploadText] = useState("");
  const [uploadTask, setUploadTask] = useState<ResumeUploadTask | null>(null);
  const [busy, setBusy] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const llmProgress = useLlmProgress();

  const activeItems = draft.sections[activeSection] ?? [];
  const activeMeta = sections.find((section) => section.type === activeSection) ?? sections[0];

  const visibleCount = useMemo(
    () => Object.values(draft.sections).flat().filter((item) => item.visible).length,
    [draft.sections],
  );

  async function refreshDraft() {
    const nextDraft = await apiGet<ResumeDraftView>("/admin/resume/draft");
    setDraft(nextDraft);
    setBasicInfo(nextDraft.basicInfo);
  }

  async function refreshVersions() {
    setVersions(await apiGet<ResumeVersion[]>("/admin/resume/versions"));
  }

  async function saveBasicInfo(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await run("basic", async () => {
      const saved = await apiPut<ResumeBasicInfo>("/admin/resume/basic-info", basicInfoPayload(basicInfo));
      setBasicInfo(saved);
      await refreshDraft();
      setNotice("个人信息已保存到草稿");
    });
  }

  async function saveItem(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await run("item", async () => {
      const payload = itemPayload(itemForm);
      if (editingItemId) {
        await apiPut<ResumeSectionItem>(`/admin/resume/items/${editingItemId}`, payload);
      } else {
        await apiPost<ResumeSectionItem>(`/admin/resume/sections/${activeSection}/items`, payload);
      }
      setItemForm({ ...emptyItem, sortOrder: activeItems.length + 1 });
      setEditingItemId(null);
      await refreshDraft();
      setNotice(editingItemId ? "条目已更新" : "条目已新增");
    });
  }

  async function deleteItem(itemId: number) {
    await run(`delete-${itemId}`, async () => {
      await apiDelete<null>(`/admin/resume/items/${itemId}`);
      await refreshDraft();
      if (editingItemId === itemId) {
        cancelEditing();
      }
      setNotice("条目已删除");
    });
  }

  async function publishDraft() {
    await run("publish", async () => {
      await apiPost<ResumeVersion>("/admin/resume/publish");
      await refreshVersions();
      setNotice("草稿已发布为新版本");
    });
  }

  async function parseUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runParseUpload(false);
  }

  async function runParseUpload(allowFallback: boolean) {
    await run("parse", async () => {
      llmProgress.start(allowFallback ? "规则兜底简历解析" : "AI 简历扫描", [
        { id: "contract", label: "解析契约", detail: "约定 basicInfo 与 SKILL/AWARD/INTERNSHIP/PROJECT/ADVANTAGE 输出结构。" },
        { id: "request", label: "模型扫描", detail: "Minimax 正在抽取真实简历信息，不补造经历。" },
        { id: "guard", label: "结构校验", detail: "后端校验字段、分组、可见性和排序。" },
        { id: "preview", label: "生成预览", detail: "创建解析任务，等待人工确认写入草稿。" },
      ]);
      llmProgress.setStep("contract", "done", "输入为原始简历文本，输出为受控 JSON 草稿。");
      llmProgress.activate("request", allowFallback ? "用户已确认使用规则降级，本次不调用 Minimax。" : "正在调用简历结构化解析。");
      const task = await apiPost<ResumeUploadTask>("/admin/resume/uploads/parse", {
        filename: "resume-text.txt",
        contentType: "text/plain",
        content: uploadText,
        allowFallback,
      });
      llmProgress.activate("guard", "正在检查解析结果是否可写入草稿。");
      llmProgress.setStep("guard", "done", task.errorMessage ?? "解析结果已通过结构化校验。");
      llmProgress.activate("preview", "正在生成可确认的解析任务。");
      setUploadTask(task);
      setNotice(task.errorMessage ?? "解析完成，请确认后写入草稿");
      llmProgress.complete(task.errorMessage ?? "完成：解析任务已生成，请确认后写入草稿。");
    }, llmProgress.fail);
  }

  async function confirmUpload() {
    if (!uploadTask) return;
    await run("confirm", async () => {
      const nextDraft = await apiPost<ResumeDraftView>(`/admin/resume/uploads/${uploadTask.id}/confirm`);
      setDraft(nextDraft);
      setBasicInfo(nextDraft.basicInfo);
      setUploadTask(null);
      await refreshVersions();
      setNotice("解析结果已写入草稿");
    });
  }

  function startEditing(item: ResumeSectionItem) {
    setEditingItemId(item.id);
    setItemForm({
      title: item.title,
      subtitle: item.subtitle ?? "",
      period: item.period ?? "",
      summary: item.summary ?? "",
      detail: item.detail ?? "",
      tags: item.tags.join(", "),
      visible: item.visible,
      sortOrder: item.sortOrder,
    });
  }

  function cancelEditing() {
    setEditingItemId(null);
    setItemForm({ ...emptyItem, sortOrder: activeItems.length + 1 });
  }

  async function run(action: string, task: () => Promise<void>, onError?: (message: string) => void) {
    setBusy(action);
    setError("");
    setNotice("");
    try {
      await task();
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "操作失败";
      setError(message);
      onError?.(message);
    } finally {
      setBusy("");
    }
  }

  return (
    <section className="admin-resume-page">
      <div className="mx-auto max-w-7xl px-5 py-10 md:px-8 md:py-14">
        <div className="admin-resume-hero">
          <div>
            <p className="eyebrow">Resume CMS</p>
            <h1 className="display mt-6 text-5xl leading-[1.02] md:text-7xl">履历内容管理</h1>
          </div>
          <div className="admin-status-grid">
            <StatusTile label="草稿版本" value={draft.version.versionName} />
            <StatusTile label="可见条目" value={`${visibleCount}`} />
            <StatusTile label="版本状态" value={draft.version.status} />
          </div>
        </div>

        {(notice || error) ? (
          <div className={`admin-message ${error ? "is-error" : ""}`}>
            {error || notice}
          </div>
        ) : null}

        <LlmProgressPanel state={llmProgress.progress} onDismiss={llmProgress.reset} />

        <div className="mt-8 grid gap-5 xl:grid-cols-[0.82fr_1.18fr]">
          <form onSubmit={saveBasicInfo} className="admin-panel">
            <PanelTitle title="个人信息" action="保存草稿" loading={busy === "basic"} />
            <div className="mt-6 grid gap-4 md:grid-cols-2">
              <TextField label="姓名" value={basicInfo.name} onChange={(value) => setBasicInfo({ ...basicInfo, name: value })} required />
              <TextField label="求职方向" value={basicInfo.title} onChange={(value) => setBasicInfo({ ...basicInfo, title: value })} required />
              <TextField label="邮箱" value={basicInfo.email} onChange={(value) => setBasicInfo({ ...basicInfo, email: value })} required />
              <TextField label="手机号" value={basicInfo.phone ?? ""} onChange={(value) => setBasicInfo({ ...basicInfo, phone: value })} />
              <TextField label="所在地" value={basicInfo.location ?? ""} onChange={(value) => setBasicInfo({ ...basicInfo, location: value })} />
              <TextField label="教育经历" value={basicInfo.education} onChange={(value) => setBasicInfo({ ...basicInfo, education: value })} required />
              <TextField label="GitHub" value={basicInfo.githubUrl ?? ""} onChange={(value) => setBasicInfo({ ...basicInfo, githubUrl: value })} />
              <TextField label="个人站点" value={basicInfo.websiteUrl ?? ""} onChange={(value) => setBasicInfo({ ...basicInfo, websiteUrl: value })} />
            </div>
            <TextAreaField label="个人介绍" value={basicInfo.summary} onChange={(value) => setBasicInfo({ ...basicInfo, summary: value })} className="mt-4" required />
          </form>

          <div className="admin-panel">
            <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
              <div>
                <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-accent">Section Editor</p>
                <h2 className="mt-3 text-2xl font-semibold">板块条目</h2>
              </div>
              <div className="flex flex-wrap gap-2">
                {sections.map((section) => (
                  <button
                    key={section.type}
                    type="button"
                    onClick={() => {
                      setActiveSection(section.type);
                      setEditingItemId(null);
                      setItemForm({ ...emptyItem, sortOrder: (draft.sections[section.type]?.length ?? 0) + 1 });
                    }}
                    className={`admin-tab ${activeSection === section.type ? "is-active" : ""}`}
                  >
                    {section.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-6 grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
              <form onSubmit={saveItem} className="admin-item-form">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-medium text-ink">{editingItemId ? "编辑条目" : `新增${activeMeta.label}`}</p>
                    <p className="mt-1 text-xs leading-5 text-graphite">{activeMeta.hint}</p>
                  </div>
                  {editingItemId ? (
                    <button type="button" onClick={cancelEditing} className="admin-link-button">取消</button>
                  ) : null}
                </div>
                <div className="mt-5 grid gap-3">
                  <TextField label="标题" value={itemForm.title} onChange={(value) => setItemForm({ ...itemForm, title: value })} required />
                  <TextField label="副标题" value={itemForm.subtitle} onChange={(value) => setItemForm({ ...itemForm, subtitle: value })} />
                  <TextField label="时间" value={itemForm.period} onChange={(value) => setItemForm({ ...itemForm, period: value })} />
                  <TextField label="摘要" value={itemForm.summary} onChange={(value) => setItemForm({ ...itemForm, summary: value })} />
                  <TextAreaField label="细节" value={itemForm.detail} onChange={(value) => setItemForm({ ...itemForm, detail: value })} />
                  <TextField label="标签，逗号分隔" value={itemForm.tags} onChange={(value) => setItemForm({ ...itemForm, tags: value })} />
                  <div className="grid grid-cols-[1fr_120px] gap-3">
                    <label className="admin-check">
                      <input
                        type="checkbox"
                        checked={itemForm.visible}
                        onChange={(event) => setItemForm({ ...itemForm, visible: event.target.checked })}
                      />
                      前台可见
                    </label>
                    <TextField
                      label="排序"
                      value={String(itemForm.sortOrder)}
                      onChange={(value) => setItemForm({ ...itemForm, sortOrder: Number(value) || 1 })}
                    />
                  </div>
                  <button disabled={busy === "item" || !itemForm.title.trim()} className="primary-action px-5 py-3 text-sm font-medium disabled:opacity-55">
                    {busy === "item" ? <Loader2 className="animate-spin" size={16} /> : editingItemId ? <Save size={16} /> : <Plus size={16} />}
                    {editingItemId ? "保存条目" : "新增条目"}
                  </button>
                </div>
              </form>

              <div className="grid content-start gap-3">
                {activeItems.length === 0 ? (
                  <div className="admin-empty">当前板块还没有条目。</div>
                ) : activeItems.map((item) => (
                  <article key={item.id} className="admin-resume-item">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-mono text-[10px] text-accent">{String(item.sortOrder).padStart(2, "0")}</span>
                          {item.visible ? <Eye size={14} className="text-accent" /> : <EyeOff size={14} className="text-graphite" />}
                        </div>
                        <h3 className="mt-3 text-lg font-semibold">{item.title}</h3>
                        <p className="mt-1 text-sm text-graphite">{[item.subtitle, item.period].filter(Boolean).join(" / ")}</p>
                      </div>
                      <div className="flex gap-2">
                        <button type="button" onClick={() => startEditing(item)} className="secondary-action px-3 py-2 text-xs">编辑</button>
                        <button
                          type="button"
                          onClick={() => deleteItem(item.id)}
                          disabled={busy === `delete-${item.id}`}
                          className="admin-danger-button"
                        >
                          {busy === `delete-${item.id}` ? <Loader2 className="animate-spin" size={14} /> : <Trash2 size={14} />}
                        </button>
                      </div>
                    </div>
                    {item.summary ? <p className="mt-4 text-sm leading-6 text-graphite">{item.summary}</p> : null}
                    {item.tags.length > 0 ? (
                      <div className="mt-4 flex flex-wrap gap-2">
                        {item.tags.map((tag) => <span key={tag} className="chapter-tag">{tag}</span>)}
                      </div>
                    ) : null}
                  </article>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="mt-5 grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
          <form onSubmit={parseUpload} className="admin-panel">
            <div className="flex flex-col justify-between gap-4 md:flex-row md:items-start">
              <div>
                <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-accent">Resume Upload</p>
                <h2 className="mt-3 text-2xl font-semibold">简历上传解析</h2>
                <p className="mt-2 max-w-xl text-sm leading-6 text-graphite">支持粘贴文本扫描。默认必须真实调用 Minimax；未配置、调用失败或模型返回不合格时会直接报错。只有点击规则兜底解析时，才使用本地规则生成可确认任务。</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => runParseUpload(true)}
                  disabled={busy === "parse" || !uploadText.trim()}
                  className="secondary-action px-5 py-3 text-sm font-medium disabled:opacity-55"
                >
                  规则兜底解析
                </button>
                <button disabled={busy === "parse" || !uploadText.trim()} className="primary-action px-5 py-3 text-sm font-medium disabled:opacity-55">
                  {busy === "parse" ? <Loader2 className="animate-spin" size={16} /> : <UploadCloud size={16} />}
                  AI 扫描简历
                </button>
              </div>
            </div>
            <textarea
              value={uploadText}
              onChange={(event) => setUploadText(event.target.value)}
              className="mt-6 min-h-56 w-full resize-y border border-line bg-white px-4 py-3 text-sm leading-7 text-ink outline-none focus:border-accent"
              placeholder="粘贴简历原文，Minimax 会尝试自动拆分个人信息、技术能力、获奖经历、实习经历、项目经历、个人优势..."
            />
            {uploadTask ? (
              <div className="mt-5 border border-line bg-stonepaper p-4">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <span className="metric-pill">{uploadTask.status}</span>
                  <button
                    type="button"
                    onClick={confirmUpload}
                    disabled={busy === "confirm" || uploadTask.status === "FAILED" || uploadTask.status === "CONFIRMED"}
                    className="secondary-action px-4 py-2.5 text-sm font-medium disabled:opacity-55"
                  >
                    {busy === "confirm" ? <Loader2 className="animate-spin" size={15} /> : <CheckCircle2 size={15} />}
                    确认写入草稿
                  </button>
                </div>
                <p className="mt-3 text-sm leading-6 text-graphite">{uploadTask.errorMessage}</p>
              </div>
            ) : null}
          </form>

          <div className="admin-panel">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-accent">Publish</p>
                <h2 className="mt-3 text-2xl font-semibold">版本发布</h2>
              </div>
              <button onClick={publishDraft} disabled={busy === "publish"} className="primary-action px-5 py-3 text-sm font-medium disabled:opacity-55">
                {busy === "publish" ? <Loader2 className="animate-spin" size={16} /> : <Rocket size={16} />}
                发布
              </button>
            </div>
            <div className="mt-6 grid gap-3">
              {versions.map((version) => (
                <div key={version.id} className="admin-version-row">
                  <div>
                    <p className="text-sm font-medium">{version.versionName}</p>
                    <p className="mt-1 font-mono text-[10px] uppercase tracking-[0.18em] text-graphite">{version.createdAt}</p>
                  </div>
                  <span className="metric-pill">{version.status}</span>
                </div>
              ))}
            </div>
            <button type="button" onClick={() => void refreshVersions()} className="secondary-action mt-5 px-4 py-2.5 text-sm">
              <RefreshCw size={15} />
              刷新版本
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}

function StatusTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="admin-status-tile">
      <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-graphite">{label}</p>
      <p className="mt-3 text-lg font-semibold">{value}</p>
    </div>
  );
}

function PanelTitle({ title, action, loading }: { title: string; action: string; loading: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div>
        <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-accent">Draft</p>
        <h2 className="mt-3 text-2xl font-semibold">{title}</h2>
      </div>
      <button disabled={loading} className="primary-action px-5 py-3 text-sm font-medium disabled:opacity-55">
        {loading ? <Loader2 className="animate-spin" size={16} /> : <Save size={16} />}
        {action}
      </button>
    </div>
  );
}

function TextField({
  label,
  value,
  onChange,
  required = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
}) {
  return (
    <label className="block">
      <span className="text-xs font-medium text-graphite">{label}</span>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required={required}
        className="mt-1.5 w-full border border-line bg-white px-3 py-2.5 text-sm text-ink outline-none focus:border-accent"
      />
    </label>
  );
}

function TextAreaField({
  label,
  value,
  onChange,
  required = false,
  className = "",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  className?: string;
}) {
  return (
    <label className={`block ${className}`}>
      <span className="text-xs font-medium text-graphite">{label}</span>
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required={required}
        className="mt-1.5 min-h-28 w-full resize-y border border-line bg-white px-3 py-2.5 text-sm leading-6 text-ink outline-none focus:border-accent"
      />
    </label>
  );
}

function basicInfoPayload(info: ResumeBasicInfo) {
  return {
    name: info.name,
    title: info.title,
    summary: info.summary,
    email: info.email,
    phone: info.phone ?? "",
    location: info.location ?? "",
    education: info.education,
    githubUrl: info.githubUrl ?? "",
    websiteUrl: info.websiteUrl ?? "",
  };
}

function itemPayload(form: ItemForm) {
  return {
    title: form.title,
    subtitle: form.subtitle,
    period: form.period,
    summary: form.summary,
    detail: form.detail,
    tags: form.tags.split(",").map((tag) => tag.trim()).filter(Boolean),
    visible: form.visible,
    sortOrder: form.sortOrder,
  };
}
