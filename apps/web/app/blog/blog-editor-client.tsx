"use client";

import { FormEvent, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { CheckCircle2, Loader2 } from "lucide-react";
import { apiPost, apiPut } from "@/lib/api";
import type { BlogCategory, BlogPost } from "@/lib/api";

const starterContent = "这里写技术总结正文。\n\n建议按照：背景问题、技术方案、实现细节、踩坑复盘、面试可讲点来组织。";

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

      <label className="block">
        <span className="text-sm font-medium text-ink">正文</span>
        <textarea
          value={content}
          onChange={(event) => setContent(event.target.value)}
          className="mt-2 min-h-80 w-full resize-y border border-line bg-stonepaper px-4 py-3 text-sm leading-7 text-ink outline-none focus:border-accent"
          required
        />
      </label>

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
