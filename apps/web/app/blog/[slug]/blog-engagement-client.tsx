"use client";

import { FormEvent, useEffect, useState } from "react";
import { Loader2, MessageSquareText, Sparkles, ThumbsUp } from "lucide-react";
import { apiPost } from "@/lib/api";
import type { BlogComment, BlogInteractionSummary } from "@/lib/api";

type BlogEngagementClientProps = {
  slug: string;
  initialSummary: BlogInteractionSummary;
  initialComments: BlogComment[];
};

export function BlogEngagementClient({
  slug,
  initialSummary,
  initialComments,
}: BlogEngagementClientProps) {
  const [summary, setSummary] = useState(initialSummary);
  const [comments, setComments] = useState(initialComments);
  const [author, setAuthor] = useState("访客");
  const [comment, setComment] = useState("");
  const [likeLoading, setLikeLoading] = useState(false);
  const [commentLoading, setCommentLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    function handleAnnotationCreated() {
      setSummary((current) => ({ ...current, annotationCount: current.annotationCount + 1 }));
    }

    window.addEventListener("blog-annotation-created", handleAnnotationCreated);
    return () => window.removeEventListener("blog-annotation-created", handleAnnotationCreated);
  }, []);

  async function like() {
    setLikeLoading(true);
    setError("");
    try {
      const nextSummary = await apiPost<BlogInteractionSummary>(`/public/blog-posts/${slug}/likes`);
      setSummary(nextSummary);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "点赞失败");
    } finally {
      setLikeLoading(false);
    }
  }

  async function submitComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCommentLoading(true);
    setError("");
    try {
      const nextComment = await apiPost<BlogComment>(`/public/blog-posts/${slug}/comments`, {
        author,
        content: comment,
      });
      setComments((current) => [nextComment, ...current]);
      setSummary((current) => ({ ...current, commentCount: current.commentCount + 1 }));
      setComment("");
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "评论失败");
    } finally {
      setCommentLoading(false);
    }
  }

  return (
    <aside className="grid gap-5">
      <section className="surface enhanced-surface p-5">
        <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Engagement</p>
        <div className="mt-5 grid grid-cols-3 gap-2 text-center">
          <Metric icon={<ThumbsUp size={16} />} value={summary.likeCount} label="点赞" />
          <Metric icon={<MessageSquareText size={16} />} value={summary.commentCount} label="评论" />
          <Metric icon={<Sparkles size={16} />} value={summary.annotationCount} label="旁注" />
        </div>
        <button onClick={like} disabled={likeLoading} className="primary-action mt-5 w-full px-5 py-3 text-sm font-medium disabled:opacity-55">
          {likeLoading ? <Loader2 className="animate-spin" size={16} /> : <ThumbsUp size={16} />}
          觉得有用
        </button>
        {error ? <p className="mt-4 text-sm leading-6 text-red-600">{error}</p> : null}
      </section>

      <section className="surface p-5">
        <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Comment</p>
        <form onSubmit={submitComment} className="mt-4 grid gap-3">
          <input
            value={author}
            onChange={(event) => setAuthor(event.target.value)}
            className="border border-line bg-white px-3 py-2 text-sm text-ink outline-none focus:border-accent"
            placeholder="你的名字"
            required
          />
          <textarea
            value={comment}
            onChange={(event) => setComment(event.target.value)}
            className="min-h-24 resize-none border border-line bg-stonepaper px-3 py-2 text-sm leading-6 text-ink outline-none focus:border-accent"
            placeholder="写一条评论或面试追问..."
            required
          />
          <button disabled={commentLoading || !comment.trim()} className="secondary-action px-4 py-2 text-sm font-medium disabled:opacity-55">
            {commentLoading ? "提交中..." : "提交评论"}
          </button>
        </form>
        <div className="mt-5 grid gap-3">
          {comments.map((item) => (
            <div key={item.id} className="border border-line bg-stonepaper p-3">
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-medium text-ink">{item.author}</p>
                <p className="font-mono text-[10px] text-graphite">{item.createdAt}</p>
              </div>
              <p className="mt-2 text-sm leading-6 text-graphite">{item.content}</p>
            </div>
          ))}
        </div>
      </section>
    </aside>
  );
}

function Metric({ icon, value, label }: { icon: React.ReactNode; value: number; label: string }) {
  return (
    <div className="border border-line bg-stonepaper px-2 py-3">
      <div className="mx-auto flex justify-center text-accent">{icon}</div>
      <p className="mt-2 text-lg font-semibold text-ink">{value}</p>
      <p className="text-xs text-graphite">{label}</p>
    </div>
  );
}
