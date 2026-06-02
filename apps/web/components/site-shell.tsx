import Link from "next/link";
import { FadeIn, MotionSurface } from "@/components/motion-shell";
import { PointerTrail } from "@/components/pointer-trail";

const navItems = [
  { href: "/resume", label: "履历" },
  { href: "/projects", label: "项目案例" },
  { href: "/lab", label: "模块实验室" },
  { href: "/jd-adapter", label: "JD 适配" },
  { href: "/interview", label: "面试模式" },
  { href: "/architecture", label: "架构" },
];

export function SiteShell({ children }: { children: React.ReactNode }) {
  return (
    <main className="min-h-screen">
      <PointerTrail />
      <nav className="sticky top-0 z-40 border-b border-line bg-stonepaper/90 backdrop-blur-xl">
        <div className="mx-auto flex h-[72px] max-w-7xl items-center justify-between px-5 md:px-8">
        <Link href="/" className="flex items-center gap-3 text-ink">
          <span className="flex h-9 w-9 items-center justify-center bg-ink font-mono text-[11px] font-semibold text-white">
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
        <Link
          href="/lab"
          className="primary-action px-4 py-2.5 text-xs font-medium md:px-5 md:text-sm"
        >
          Live Demo
        </Link>
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
      <div className="relative z-10 mx-auto max-w-7xl px-5 py-16 md:px-8 md:py-24">
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
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return <MotionSurface className={className}>{children}</MotionSurface>;
}
