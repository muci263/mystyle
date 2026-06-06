"use client";

import { FormEvent, SyntheticEvent, useMemo, useRef, useState } from "react";
import { Check, Loader2, MessageSquarePlus, Pencil, Trash2, X } from "lucide-react";
import { apiDelete, apiPost, apiPut } from "@/lib/api";
import type { BlogAnnotation, BlogInteractionSummary } from "@/lib/api";

type BlogArticleBodyClientProps = {
  slug: string;
  paragraphs: string[];
  initialAnnotations: BlogAnnotation[];
};

type SelectionDraft = {
  text: string;
  top: number;
  left: number;
};

type TextSegment = {
  text: string;
  annotations: BlogAnnotation[];
};

export function BlogArticleBodyClient({ slug, paragraphs, initialAnnotations }: BlogArticleBodyClientProps) {
  const bodyRef = useRef<HTMLElement>(null);
  const [annotations, setAnnotations] = useState(initialAnnotations);
  const [draft, setDraft] = useState<SelectionDraft | null>(null);
  const [note, setNote] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingNote, setEditingNote] = useState("");
  const [pendingAction, setPendingAction] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const annotationGroups = useMemo(() => groupAnnotations(annotations), [annotations]);

  function captureSelection(event: SyntheticEvent<HTMLElement>) {
    const root = bodyRef.current;
    const target = event.target instanceof HTMLElement ? event.target : null;
    if (target?.closest(".annotation-editor")) {
      return;
    }

    const selection = window.getSelection();
    if (!root || !selection || selection.rangeCount === 0 || selection.isCollapsed) {
      return;
    }

    const range = selection.getRangeAt(0);
    const container = range.commonAncestorContainer.nodeType === Node.TEXT_NODE
      ? range.commonAncestorContainer.parentElement
      : range.commonAncestorContainer;

    if (!(container instanceof Node) || !root.contains(container)) {
      return;
    }

    const selectedText = normalizeSelection(selection.toString());
    if (!selectedText) {
      return;
    }

    const rangeRect = range.getBoundingClientRect();
    const rootRect = root.getBoundingClientRect();
    const maxLeft = Math.max(180, rootRect.width - 180);
    const rawLeft = rangeRect.left + rangeRect.width / 2 - rootRect.left;
    setDraft({
      text: selectedText,
      top: Math.max(12, rangeRect.bottom - rootRect.top + 14),
      left: Math.min(Math.max(rawLeft, 180), maxLeft),
    });
    setNote("");
    setError(selectedText.length > 160 ? "选中的文字过长，请只选择一句关键原文。" : "");
  }

  async function submitAnnotation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!draft || !note.trim() || draft.text.length > 160) {
      return;
    }
    setLoading(true);
    setError("");
    try {
      const nextAnnotation = await apiPost<BlogAnnotation>(`/public/blog-posts/${slug}/annotations`, {
        anchorText: draft.text,
        note,
      });
      setAnnotations((current) => [nextAnnotation, ...current]);
      window.dispatchEvent(new CustomEvent("blog-annotation-created", { detail: nextAnnotation }));
      setDraft(null);
      setNote("");
      window.getSelection()?.removeAllRanges();
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "旁注保存失败");
    } finally {
      setLoading(false);
    }
  }

  function startEditing(annotation: BlogAnnotation) {
    setEditingId(annotation.id);
    setEditingNote(annotation.note);
    setError("");
  }

  async function updateAnnotation(annotation: BlogAnnotation) {
    if (!editingNote.trim()) {
      setError("旁注内容不能为空");
      return;
    }
    setPendingAction(`update-${annotation.id}`);
    setError("");
    try {
      const nextAnnotation = await apiPut<BlogAnnotation>(`/public/blog-posts/${slug}/annotations/${annotation.id}`, {
        anchorText: annotation.anchorText,
        note: editingNote,
      });
      setAnnotations((current) => current.map((item) => item.id === annotation.id ? nextAnnotation : item));
      setEditingId(null);
      setEditingNote("");
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "旁注更新失败");
    } finally {
      setPendingAction("");
    }
  }

  async function deleteAnnotation(annotationId: number) {
    setPendingAction(`delete-${annotationId}`);
    setError("");
    try {
      const nextSummary = await apiDelete<BlogInteractionSummary>(`/public/blog-posts/${slug}/annotations/${annotationId}`);
      setAnnotations((current) => current.filter((item) => item.id !== annotationId));
      window.dispatchEvent(new CustomEvent("blog-annotation-deleted", { detail: nextSummary }));
      if (editingId === annotationId) {
        setEditingId(null);
        setEditingNote("");
      }
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "旁注删除失败");
    } finally {
      setPendingAction("");
    }
  }

  return (
    <section
      ref={bodyRef}
      className="journal-article-body annotated-article"
      onMouseUp={captureSelection}
      onKeyUp={captureSelection}
    >
      <div className="mb-7 flex flex-wrap items-center justify-between gap-3 border-b border-line pb-5">
        <p className="text-sm leading-6 text-graphite">选中正文中的一句话即可添加旁注。已有旁注会以细腻高亮展示，悬停后查看内容。</p>
        <span className="font-mono text-[10px] uppercase tracking-[0.22em] text-accent">
          {annotations.length} Notes
        </span>
      </div>

      <div className="space-y-7">
        {paragraphs.map((paragraph, index) => (
          <p key={`${paragraph.slice(0, 18)}-${index}`} className="text-base leading-9 text-graphite md:text-lg">
            {annotateText(paragraph, annotationGroups).map((segment, segmentIndex) => (
              segment.annotations.length > 0 ? (
                <AnnotatedText
                  key={`${segment.text}-${segmentIndex}`}
                  text={segment.text}
                  annotations={segment.annotations}
                  editingId={editingId}
                  editingNote={editingNote}
                  pendingAction={pendingAction}
                  onStartEditing={startEditing}
                  onCancelEditing={() => {
                    setEditingId(null);
                    setEditingNote("");
                  }}
                  onEditingNoteChange={setEditingNote}
                  onUpdate={updateAnnotation}
                  onDelete={deleteAnnotation}
                />
              ) : (
                <span key={`${segment.text}-${segmentIndex}`}>{segment.text}</span>
              )
            ))}
          </p>
        ))}
      </div>

      {draft ? (
        <form
          onSubmit={submitAnnotation}
          className="annotation-editor"
          style={{ top: draft.top, left: draft.left }}
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-accent">New Side Note</p>
              <p className="mt-2 line-clamp-2 text-sm font-medium leading-6 text-ink">{draft.text}</p>
            </div>
            <button type="button" onClick={() => setDraft(null)} className="annotation-close" aria-label="关闭旁注编辑">
              <X size={15} />
            </button>
          </div>
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            className="mt-4 min-h-24 w-full resize-none border border-line bg-stonepaper px-3 py-2 text-sm leading-6 text-ink outline-none focus:border-accent"
            placeholder="写下这句话背后的技术理解、面试讲法或补充说明..."
            autoFocus
          />
          {error ? <p className="mt-3 text-sm leading-6 text-red-600">{error}</p> : null}
          <button disabled={loading || !note.trim() || draft.text.length > 160} className="primary-action mt-4 w-full px-4 py-2.5 text-sm font-medium disabled:opacity-55">
            {loading ? <Loader2 className="animate-spin" size={15} /> : <MessageSquarePlus size={15} />}
            {loading ? "保存中..." : "保存旁注"}
          </button>
        </form>
      ) : null}
    </section>
  );
}

function AnnotatedText({
  text,
  annotations,
  editingId,
  editingNote,
  pendingAction,
  onStartEditing,
  onCancelEditing,
  onEditingNoteChange,
  onUpdate,
  onDelete,
}: {
  text: string;
  annotations: BlogAnnotation[];
  editingId: number | null;
  editingNote: string;
  pendingAction: string;
  onStartEditing: (annotation: BlogAnnotation) => void;
  onCancelEditing: () => void;
  onEditingNoteChange: (value: string) => void;
  onUpdate: (annotation: BlogAnnotation) => void;
  onDelete: (annotationId: number) => void;
}) {
  return (
    <span className="annotated-text" tabIndex={0}>
      {text}
      <span className="annotation-tooltip" role="note">
        {annotations.map((annotation) => (
          <span key={annotation.id} className="annotation-tooltip-item">
            {editingId === annotation.id ? (
              <>
                <textarea
                  value={editingNote}
                  onChange={(event) => onEditingNoteChange(event.target.value)}
                  className="min-h-24 w-full resize-none border border-white/15 bg-white/10 px-3 py-2 text-sm leading-6 text-white outline-none focus:border-white/45"
                  aria-label="编辑旁注内容"
                />
                <span className="mt-3 flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => onUpdate(annotation)}
                    disabled={pendingAction === `update-${annotation.id}` || !editingNote.trim()}
                    className="annotation-action annotation-action-primary"
                  >
                    {pendingAction === `update-${annotation.id}` ? <Loader2 className="animate-spin" size={13} /> : <Check size={13} />}
                    保存
                  </button>
                  <button type="button" onClick={onCancelEditing} className="annotation-action">
                    <X size={13} />
                    取消
                  </button>
                </span>
              </>
            ) : (
              <>
                <span className="block text-sm leading-6 text-white">{annotation.note}</span>
                <span className="mt-3 flex items-center justify-between gap-3">
                  <span className="font-mono text-[10px] text-white/55">{annotation.createdAt}</span>
                  <span className="flex items-center gap-1.5">
                    <button type="button" onClick={() => onStartEditing(annotation)} className="annotation-icon-action" aria-label="编辑旁注">
                      <Pencil size={13} />
                    </button>
                    <button
                      type="button"
                      onClick={() => onDelete(annotation.id)}
                      disabled={pendingAction === `delete-${annotation.id}`}
                      className="annotation-icon-action annotation-danger-action"
                      aria-label="删除旁注"
                    >
                      {pendingAction === `delete-${annotation.id}` ? <Loader2 className="animate-spin" size={13} /> : <Trash2 size={13} />}
                    </button>
                  </span>
                </span>
              </>
            )}
          </span>
        ))}
      </span>
    </span>
  );
}

function groupAnnotations(annotations: BlogAnnotation[]) {
  return annotations.reduce<Record<string, BlogAnnotation[]>>((groups, annotation) => {
    const anchor = annotation.anchorText.trim();
    if (!anchor) {
      return groups;
    }
    groups[anchor] = groups[anchor] ? [...groups[anchor], annotation] : [annotation];
    return groups;
  }, {});
}

function annotateText(text: string, annotationGroups: Record<string, BlogAnnotation[]>): TextSegment[] {
  const anchors = Object.keys(annotationGroups)
    .filter((anchor) => text.includes(anchor))
    .sort((left, right) => right.length - left.length);
  if (anchors.length === 0) {
    return [{ text, annotations: [] }];
  }

  const ranges: Array<{ start: number; end: number; anchor: string }> = [];
  for (const anchor of anchors) {
    let start = text.indexOf(anchor);
    while (start >= 0) {
      const end = start + anchor.length;
      const overlaps = ranges.some((range) => start < range.end && end > range.start);
      if (!overlaps) {
        ranges.push({ start, end, anchor });
      }
      start = text.indexOf(anchor, end);
    }
  }

  ranges.sort((left, right) => left.start - right.start);
  const segments: TextSegment[] = [];
  let cursor = 0;
  for (const range of ranges) {
    if (range.start > cursor) {
      segments.push({ text: text.slice(cursor, range.start), annotations: [] });
    }
    segments.push({ text: text.slice(range.start, range.end), annotations: annotationGroups[range.anchor] });
    cursor = range.end;
  }
  if (cursor < text.length) {
    segments.push({ text: text.slice(cursor), annotations: [] });
  }
  return segments;
}

function normalizeSelection(value: string) {
  return value.replace(/\s+/g, " ").trim();
}
