"use client";

import { ChangeEvent, FormEvent, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { CheckCircle2, Loader2, Upload } from "lucide-react";
import { apiPost, apiPut } from "@/lib/api";
import type { BlogCategory, BlogPost } from "@/lib/api";
import { MarkdownRenderer } from "@/components/markdown-renderer";

const starterContent = `# 技术总结标题

## 背景问题

这里写清楚问题发生的业务场景和约束。

## 技术方案

- 方案一：说明核心设计。
- 方案二：说明边界和取舍。

## 实现细节

\`\`\`java
// 关键代码或伪代码
\`\`\`

## 复盘

总结踩坑、优化结果和面试可讲点。`;

type BlogEditorClientProps = {
  categories: BlogCategory[];
  mode?: "create" | "edit";
  post?: BlogPost;
};

export function BlogEditorClient({ categories, mode = "create", post }: BlogEditorClientProps) {
  const router = useRouter();
  const initialCategory = post?.category ?? categories[0]?.name ?? "实习心得";
  const [title, setTitle] = useState(post?.title ?? "");
  const [category, setCategory] = useState(initialCategory);
  const [excerpt, setExcerpt] = useState(post?.excerpt ?? "");
  const [content, setContent] = useState(post?.content ? post.content.replaceAll("\\n", "\n") : starterContent);
  const [tags, setTags] = useState(post?.tags.join(", ") ?? "Java, Spring Boot");
  const [readMinutes, setReadMinutes] = useState(String(post?.readMinutes ?? 4));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState<BlogPost | null>(null);
  const isEdit = mode === "edit" && post !== undefined;

  const parsedTags = useMemo(
    () => tags.split(",").map((tag) => tag.trim()).filter(Boolean),
    [tags],
  );

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setSaved(null);
    try {
      const payload = {
        title,
        category,
        excerpt,
        content,
        tags: parsedTags,
        readMinutes: Number(readMinutes) || undefined,
      };
      const savedPost = isEdit && post
        ? await apiPut<BlogPost>(`/public/blog-posts/${post.slug}`, payload)
        : await apiPost<BlogPost>("/public/blog-posts", payload);
      setSaved(savedPost);
      router.push(`/blog/${savedPost.slug}`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "博客保存失败");
    } finally {
      setLoading(false);
    }
  }

  async function importMarkdown(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setError("");
    try {
      if (!file.name.toLowerCase().endsWith(".md") && !file.name.toLowerCase().endsWith(".markdown")) {
        setError("请上传 .md 或 .markdown 文件。");
        return;
      }
      const text = await file.text();
      setContent(text);
      const firstHeading = text.match(/^#\s+(.+)$/m)?.[1]?.trim();
      if (!title.trim() && firstHeading) {
        setTitle(firstHeading);
      }
      if (!excerpt.trim()) {
        const firstParagraph = text
          .replace(/^---[\s\S]*?---\s*/m, "")
          .split(/\n\s*\n/)
          .map((item) => item.trim())
          .find((item) => item && !item.startsWith("#") && !item.startsWith("```"));
        if (firstParagraph) {
          setExcerpt(firstParagraph.replace(/^[-*]\s+/, "").slice(0, 220));
        }
      }
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Markdown 文件读取失败");
    } finally {
      event.target.value = "";
    }
  }

  return (
    <form onSubmit={submit} className="mt-10 grid gap-5">
      <div className="grid gap-5 md:grid-cols-[1fr_220px]">
        <label className="block">
          <span className="text-sm font-medium text-ink">标题</span>
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            className="mt-2 w-full border border-line bg-white px-4 py-3 text-sm text-ink outline-none focus:border-accent"
            placeholder="例如：Redis 缓存一致性复盘"
            required
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-ink">分类</span>
          <select
            value={category}
            onChange={(event) => setCategory(event.target.value)}
            className="mt-2 w-full border border-line bg-white px-4 py-3 text-sm text-ink outline-none focus:border-accent"
            required
          >
            {categories.map((item) => (
              <option key={item.code} value={item.name}>
                {item.name} · {item.postCount}
              </option>
            ))}
          </select>
        </label>
      </div>

      <label className="block">
        <span className="text-sm font-medium text-ink">摘要</span>
        <textarea
          value={excerpt}
          onChange={(event) => setExcerpt(event.target.value)}
          className="mt-2 min-h-28 w-full resize-none border border-line bg-white px-4 py-3 text-sm leading-7 text-ink outline-none focus:border-accent"
          placeholder="用一两句话说明这篇文章解决的问题。"
          required
        />
      </label>

      <section>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <span className="text-sm font-medium text-ink">正文 Markdown</span>
          <label className="secondary-action inline-flex cursor-pointer items-center gap-2 px-4 py-2 text-sm font-medium">
            <Upload size={15} />
            导入 .md
            <input type="file" accept=".md,.markdown,text/markdown,text/plain" className="sr-only" onChange={importMarkdown} />
          </label>
        </div>
        <div className="mt-2 grid gap-4 lg:grid-cols-2">
          <textarea
            value={content}
            onChange={(event) => setContent(event.target.value)}
            className="min-h-[32rem] w-full resize-y border border-line bg-stonepaper px-4 py-3 font-mono text-sm leading-7 text-ink outline-none focus:border-accent"
            required
          />
          <div className="min-h-[32rem] overflow-auto border border-line bg-white p-5">
            <p className="mb-4 font-mono text-[10px] uppercase tracking-[0.2em] text-accent">Preview</p>
            <MarkdownRenderer content={content} />
          </div>
        </div>
      </section>

      <div className="grid gap-5 md:grid-cols-[1fr_180px]">
        <label className="block">
          <span className="text-sm font-medium text-ink">标签，逗号分隔</span>
          <input
            value={tags}
            onChange={(event) => setTags(event.target.value)}
            className="mt-2 w-full border border-line bg-white px-4 py-3 text-sm text-ink outline-none focus:border-accent"
            placeholder="Redis, MySQL, 缓存同步"
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-ink">阅读分钟</span>
          <input
            value={readMinutes}
            onChange={(event) => setReadMinutes(event.target.value)}
            className="mt-2 w-full border border-line bg-white px-4 py-3 text-sm text-ink outline-none focus:border-accent"
            inputMode="numeric"
          />
        </label>
      </div>

      <div className="flex flex-wrap items-center gap-4">
        <button disabled={loading || !title.trim() || !excerpt.trim() || !content.trim()} className="primary-action px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55">
          {loading ? <Loader2 className="animate-spin" size={16} /> : null}
          {loading ? "提交中..." : isEdit ? "保存修改" : "发布博客"}
        </button>
        {saved ? (
          <span className="inline-flex items-center gap-2 text-sm text-accent">
            <CheckCircle2 size={16} />
            已保存，正在进入详情页
          </span>
        ) : null}
        {error ? <span className="text-sm text-red-600">{error}</span> : null}
      </div>
    </form>
  );
}
