import Link from "next/link";
import { ArrowUpRight, BookOpen, MessageSquareText, PencilLine, Sparkles, ThumbsUp } from "lucide-react";
import { FadeIn } from "@/components/motion-shell";
import { SiteShell, Surface } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { BlogCategory, BlogPost } from "@/lib/api";

type BlogPageProps = {
  searchParams: Promise<{ category?: string; tag?: string }>;
};

export default async function BlogPage({ searchParams }: BlogPageProps) {
  const { category, tag } = await searchParams;
  const query = new URLSearchParams();
  if (category) {
    query.set("category", category);
  }
  if (tag) {
    query.set("tag", tag);
  }
  const queryString = query.toString();
  const [posts, categories] = await Promise.all([
    apiGet<BlogPost[]>(`/public/blog-posts${queryString ? `?${queryString}` : ""}`),
    apiGet<BlogCategory[]>("/public/blog-posts/categories"),
  ]);
  const activeCategory = categories.find((item) => item.name === category || item.code === category);
  const activeLabel = activeCategory?.name ?? (tag ? `#${tag}` : "全部文章");
  const totalPosts = categories.reduce((sum, item) => sum + item.postCount, 0);

  return (
    <SiteShell>
      <section className="journal-stage">
        <div className="mx-auto max-w-7xl px-5 py-14 md:px-8 md:py-20">
          <FadeIn>
            <div className="grid gap-10 lg:grid-cols-[minmax(0,7fr)_minmax(320px,5fr)] lg:items-end">
              <div>
                <p className="eyebrow">Technical Journal</p>
                <h1 className="display mt-7 max-w-4xl text-balance text-5xl leading-[0.98] text-ink md:text-7xl">
                  技术博客与思考记录
                </h1>
                <p className="mt-7 max-w-2xl text-base leading-8 text-graphite md:text-lg">
                  这里不堆砌概念，只记录项目复盘、工程取舍和技术学习。每篇文章都应该能成为面试时的一段清晰证据。
                </p>
              </div>
              <div className="journal-hero-panel">
                <div className="grid grid-cols-3 border-b border-line">
                  <div className="p-5">
                    <span className="journal-stat">{totalPosts}</span>
                    <span className="journal-stat-label">Posts</span>
                  </div>
                  <div className="border-l border-line p-5">
                    <span className="journal-stat">{categories.length}</span>
                    <span className="journal-stat-label">Types</span>
                  </div>
                  <div className="border-l border-line p-5">
                    <span className="journal-stat">{posts.length}</span>
                    <span className="journal-stat-label">Shown</span>
                  </div>
                </div>
                <div className="flex flex-wrap items-center justify-between gap-4 p-5">
                  <p className="max-w-xs text-sm leading-6 text-graphite">
                    当前视图：<span className="font-medium text-ink">{activeLabel}</span>
                  </p>
                  <Link href="/blog/new" className="primary-action px-5 py-3 text-sm font-medium">
                    <PencilLine size={16} />
                    新增博客
                  </Link>
                </div>
              </div>
            </div>
          </FadeIn>
        </div>
      </section>

      <section className="mx-auto grid max-w-7xl gap-8 px-5 pb-16 md:px-8 md:pb-24 lg:grid-cols-[280px_minmax(0,1fr)]">
        <FadeIn className="lg:sticky lg:top-28 lg:self-start">
          <aside className="journal-rail">
            <div className="flex items-center justify-between gap-4 border-b border-line pb-5">
              <div>
                <p className="font-mono text-[10px] uppercase tracking-[0.24em] text-accent">Browse</p>
                <h2 className="mt-2 text-xl font-semibold text-ink">文章索引</h2>
              </div>
              {(activeCategory || tag) ? (
                <Link href="/blog" className="text-link text-sm font-medium">
                  清除
                </Link>
              ) : null}
            </div>

            <div className="mt-5 grid gap-2">
              <Link href="/blog" className={`journal-filter ${category || tag ? "" : "is-active"}`}>
                <span>全部文章</span>
                <span>{totalPosts}</span>
              </Link>
              {categories.map((item) => (
                <Link
                  key={item.name}
                  href={`/blog?category=${encodeURIComponent(item.name)}`}
                  className={`journal-filter ${activeCategory?.code === item.code ? "is-active" : ""}`}
                >
                  <span>{item.name}</span>
                  <span>{item.postCount}</span>
                </Link>
              ))}
            </div>

            {tag ? (
              <div className="mt-6 border-t border-line pt-5">
                <p className="text-xs text-graphite">正在查看标签</p>
                <span className="chapter-tag mt-3 inline-flex">#{tag}</span>
              </div>
            ) : null}
          </aside>
        </FadeIn>

        <div>
          <FadeIn>
            <div className="journal-list-head">
              <div>
                <p className="font-mono text-[10px] uppercase tracking-[0.24em] text-accent">Selected Reading</p>
                <h2 className="mt-2 text-2xl font-semibold text-ink md:text-3xl">{activeLabel}</h2>
              </div>
              <Link href="/blog/new" className="secondary-action px-4 py-2.5 text-sm font-medium">
                <PencilLine size={16} />
                写一篇
              </Link>
            </div>
          </FadeIn>

          <div className="mt-5 grid gap-3">
            {posts.map((post, index) => (
              <FadeIn key={post.slug} delay={index * 0.05}>
                <article className="journal-card group">
                  <div className="journal-card-index">
                    <span>{String(index + 1).padStart(2, "0")}</span>
                    <span>{post.readMinutes} min</span>
                  </div>

                  <div className="min-w-0">
                    <div className="journal-meta-row">
                      <Link
                        href={`/blog?category=${encodeURIComponent(post.category)}`}
                        className="font-mono text-[11px] uppercase tracking-[0.22em] text-accent transition hover:text-ink"
                      >
                        {post.category}
                      </Link>
                      <span className="flex items-center gap-1.5 text-xs text-graphite">
                        <ThumbsUp size={13} />
                        {post.likeCount}
                      </span>
                      <span className="flex items-center gap-1.5 text-xs text-graphite">
                        <MessageSquareText size={13} />
                        {post.commentCount}
                      </span>
                      <span className="flex items-center gap-1.5 text-xs text-graphite">
                        <Sparkles size={13} />
                        {post.annotationCount}
                      </span>
                    </div>

                    <Link href={`/blog/${post.slug}`} className="block cursor-pointer">
                      <h3 className="journal-card-title">{post.title}</h3>
                    </Link>
                    <p className="mt-4 max-w-2xl leading-7 text-graphite">{post.excerpt}</p>
                    <div className="mt-5 flex flex-wrap gap-2">
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

                  <Link href={`/blog/${post.slug}`} aria-label={`阅读 ${post.title}`} className="journal-card-arrow">
                    <ArrowUpRight size={21} />
                  </Link>
                </article>
              </FadeIn>
            ))}

            {posts.length === 0 && (
              <Surface>
                <BookOpen className="text-accent" size={22} />
                <p className="mt-5 text-lg font-medium text-ink">还没有发布文章</p>
                <p className="mt-3 text-sm leading-7 text-graphite">后端接口已经预留，写入数据库后这里会自动展示。</p>
              </Surface>
            )}
          </div>
        </div>
      </section>
    </SiteShell>
  );
}
