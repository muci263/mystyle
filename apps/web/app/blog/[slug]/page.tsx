import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft, Clock3, MessageSquareText, PencilLine, Sparkles, ThumbsUp } from "lucide-react";
import { FadeIn } from "@/components/motion-shell";
import { SiteShell } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { BlogAnnotation, BlogComment, BlogInteractionSummary, BlogPost } from "@/lib/api";
import { BlogArticleBodyClient } from "@/app/blog/[slug]/blog-article-body-client";
import { BlogEngagementClient } from "@/app/blog/[slug]/blog-engagement-client";

type BlogDetailPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function BlogDetailPage({ params }: BlogDetailPageProps) {
  const { slug } = await params;
  const [post, comments, annotations, summary] = await Promise.all([
    getPost(slug),
    apiGet<BlogComment[]>(`/public/blog-posts/${slug}/comments`),
    apiGet<BlogAnnotation[]>(`/public/blog-posts/${slug}/annotations`),
    apiGet<BlogInteractionSummary>(`/public/blog-posts/${slug}/interactions`),
  ]);
  const paragraphs = toParagraphs(post.content);

  return (
    <SiteShell>
      <article className="journal-article-shell">
        <div className="mx-auto max-w-7xl px-5 py-14 md:px-8 md:py-20">
          <FadeIn>
            <div className="flex flex-wrap items-center justify-between gap-4">
              <Link href="/blog" className="text-link inline-flex items-center gap-2 text-sm font-medium">
                <ArrowLeft size={16} />
                返回技术博客
              </Link>
              <Link href={`/blog/${slug}/edit`} className="secondary-action px-4 py-2 text-sm font-medium">
                <PencilLine size={15} />
                编辑博客
              </Link>
            </div>

            <header className="mt-12 grid gap-10 border-b border-line pb-12 lg:grid-cols-[minmax(0,0.9fr)_280px] lg:items-end">
              <div>
                <div className="flex flex-wrap items-center gap-3">
                  <Link
                    href={`/blog?category=${encodeURIComponent(post.category)}`}
                    className="font-mono text-xs uppercase tracking-[0.22em] text-accent transition hover:text-ink"
                  >
                    {post.category}
                  </Link>
                  <span className="text-sm text-graphite">{post.publishedAt}</span>
                  <span className="flex items-center gap-1.5 text-sm text-graphite">
                    <Clock3 size={14} />
                    {post.readMinutes} min read
                  </span>
                </div>
                <h1 className="display mt-6 max-w-5xl text-balance text-5xl leading-[1.02] text-ink md:text-7xl">
                  {post.title}
                </h1>
                <p className="mt-7 max-w-3xl text-lg leading-9 text-graphite">{post.excerpt}</p>
              </div>

              <div className="journal-hero-panel">
                <div className="grid grid-cols-3 border-b border-line">
                  <div className="p-4">
                    <ThumbsUp className="text-accent" size={16} />
                    <span className="mt-3 block text-xl font-semibold text-ink">{summary.likeCount}</span>
                  </div>
                  <div className="border-l border-line p-4">
                    <MessageSquareText className="text-accent" size={16} />
                    <span className="mt-3 block text-xl font-semibold text-ink">{summary.commentCount}</span>
                  </div>
                  <div className="border-l border-line p-4">
                    <Sparkles className="text-accent" size={16} />
                    <span className="mt-3 block text-xl font-semibold text-ink">{summary.annotationCount}</span>
                  </div>
                </div>
                <div className="flex flex-wrap gap-2 p-4">
                  {post.tags.map((tag) => (
                    <Link
                      key={tag}
                      href={`/blog?tag=${encodeURIComponent(tag)}`}
                      className="chapter-tag transition hover:border-accent hover:text-accent"
                    >
                      {tag}
                    </Link>
                  ))}
                </div>
              </div>
            </header>
          </FadeIn>

          <div className="mt-10 grid gap-8 lg:grid-cols-[minmax(0,1fr)_340px]">
            <FadeIn>
              <BlogArticleBodyClient slug={slug} paragraphs={paragraphs} initialAnnotations={annotations} />
            </FadeIn>
            <div className="lg:sticky lg:top-28 lg:self-start">
              <BlogEngagementClient
                slug={slug}
                initialSummary={summary}
                initialComments={comments}
              />
            </div>
          </div>
        </div>
      </article>
    </SiteShell>
  );
}

async function getPost(slug: string) {
  try {
    return await apiGet<BlogPost>(`/public/blog-posts/${slug}`);
  } catch {
    notFound();
  }
}

function toParagraphs(content: string) {
  return content
    .replaceAll("\\n", "\n")
    .split(/\n\s*\n/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean);
}
