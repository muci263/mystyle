import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { FadeIn } from "@/components/motion-shell";
import { SiteShell } from "@/components/site-shell";
import { BlogEditorClient } from "@/app/blog/blog-editor-client";
import { apiGet } from "@/lib/api";
import type { BlogCategory } from "@/lib/api";

export default async function NewBlogPage() {
  const categories = await apiGet<BlogCategory[]>("/public/blog-posts/categories");

  return (
    <SiteShell>
      <section className="mx-auto max-w-5xl px-5 py-14 md:px-8 md:py-20">
        <FadeIn>
          <Link href="/blog" className="text-link inline-flex items-center gap-2 text-sm font-medium">
            <ArrowLeft size={16} />
            返回技术博客
          </Link>
          <div className="mt-10 border-b border-line pb-10">
            <p className="eyebrow">New Journal</p>
            <h1 className="display mt-6 text-5xl leading-[1.04] text-ink md:text-7xl">新增技术博客</h1>
            <p className="mt-6 max-w-2xl leading-8 text-graphite">
              用真实后端接口写入 MySQL。分类、标签、摘要和正文都会进入博客内容库，提交后可以直接进入详情页。
            </p>
          </div>
        </FadeIn>
        <BlogEditorClient categories={categories} />
      </section>
    </SiteShell>
  );
}
