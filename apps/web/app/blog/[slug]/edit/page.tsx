import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { FadeIn } from "@/components/motion-shell";
import { SiteShell } from "@/components/site-shell";
import { BlogEditorClient } from "@/app/blog/blog-editor-client";
import { apiGet } from "@/lib/api";
import type { BlogCategory, BlogPost } from "@/lib/api";

type EditBlogPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function EditBlogPage({ params }: EditBlogPageProps) {
  const { slug } = await params;
  const [post, categories] = await Promise.all([
    getPost(slug),
    apiGet<BlogCategory[]>("/public/blog-posts/categories"),
  ]);

  return (
    <SiteShell>
      <section className="mx-auto max-w-5xl px-5 py-14 md:px-8 md:py-20">
        <FadeIn>
          <Link href={`/blog/${slug}`} className="text-link inline-flex items-center gap-2 text-sm font-medium">
            <ArrowLeft size={16} />
            返回文章详情
          </Link>
          <div className="mt-10 border-b border-line pb-10">
            <p className="eyebrow">Edit Journal</p>
            <h1 className="display mt-6 text-5xl leading-[1.04] text-ink md:text-7xl">编辑博客</h1>
            <p className="mt-6 max-w-2xl leading-8 text-graphite">
              修改标题、分类、标签、摘要和正文后会通过后端 PUT 接口更新 MySQL，评论、旁注和点赞数据会保留。
            </p>
          </div>
        </FadeIn>
        <BlogEditorClient mode="edit" post={post} categories={categories} />
      </section>
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
