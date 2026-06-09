import Link from "next/link";
import { FadeIn, MotionSurface } from "@/components/motion-shell";
import { PointerTrail } from "@/components/pointer-trail";

const navItems = [
  { href: "/resume", label: "履历" },
  { href: "/evidence", label: "项目证据" },
  { href: "/interview-kit", label: "面试助手" },
  { href: "/blog", label: "技术博客" },
];

export function SiteShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="min-h-screen">
      <PointerTrail />
      <nav className="site-nav-pill">
        <div className="mx-auto flex h-full items-center justify-between px-4 md:px-5">
          <Link href="/" className="flex items-center gap-3 text-ink">
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-ink font-mono text-[11px] font-semibold text-white shadow-sm">
              ZH
            </span>
            <span>
              <span className="block text-sm font-medium leading-none">赵豪然</span>
              <span className="mt-1.5 hidden font-mono text-[9px] uppercase tracking-[0.24em] text-graphite sm:block">
                Backend / Portfolio
              </span>
            </span>
          </Link>
          <div className="hidden items-center gap-6 text-sm text-graphite lg:flex">
            {navItems.map((item, index) => (
              <Link key={item.href} href={item.href} className="group flex items-center gap-2 transition hover:text-ink">
                <span className="font-mono text-[10px] text-graphite/70">0{index + 1}</span>
                {item.label}
              </Link>
            ))}
          </div>
        </div>
      </nav>
      {children}
    </main>
  );
}

export function PageHeader({
  eyebrow,
  title,
  description,
}: {
  eyebrow: string;
  title: string;
  description: string;
}) {
  return (
    <section className="subpage-stage console-grid">
      <div aria-hidden="true" className="subpage-signal">
        <span className="subpage-signal-orbit subpage-signal-orbit-a" />
        <span className="subpage-signal-orbit subpage-signal-orbit-b" />
        <span className="subpage-signal-axis" />
        <span className="subpage-signal-core" />
      </div>
      <div className="relative z-10 mx-auto max-w-7xl px-5 pb-16 pt-32 md:px-8 md:pb-24 md:pt-36">
        <FadeIn>
          <p className="eyebrow technical-label">{eyebrow}</p>
          <h1 className="display mt-7 max-w-5xl text-5xl leading-[1.04] text-white md:text-8xl">
            {title}
          </h1>
          <p className="mt-8 max-w-2xl text-base leading-8 text-white/68 md:text-lg">{description}</p>
        </FadeIn>
      </div>
    </section>
  );
}

export function Surface({
  children,
  className = "",
  id,
}: {
  children: React.ReactNode;
  className?: string;
  id?: string;
}) {
  return <MotionSurface id={id} className={className}>{children}</MotionSurface>;
}
