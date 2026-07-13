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
      <div className="editorial-topline">
        <span>Engineering Evidence / Backend Portfolio</span>
        <span className="hidden sm:inline">Shanxi University / Software Engineering</span>
      </div>
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
    <section className="subpage-stage editorial-hero">
      <div className="mx-auto grid max-w-7xl gap-9 px-5 pb-16 pt-32 md:px-8 md:pb-20 md:pt-36 lg:grid-cols-[minmax(0,1fr)_17rem] lg:items-end">
        <FadeIn>
          <p className="eyebrow">{eyebrow}</p>
          <h1 className="display editorial-hero-title mt-7 max-w-6xl text-balance text-5xl leading-[0.92] text-ink md:text-8xl">
            {title}
          </h1>
        </FadeIn>
        <FadeIn delay={0.08} className="editorial-hero-aside">
          <p className="font-mono text-[10px] uppercase tracking-[0.28em] text-accent">Current Section</p>
          <p className="mt-4 text-sm leading-7 text-graphite">{description}</p>
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

export function EditorialHero({
  eyebrow,
  title,
  description,
  rightLabel = "Portfolio System",
}: {
  eyebrow: string;
  title: string;
  description: string;
  rightLabel?: string;
}) {
  return (
    <section className="editorial-hero">
      <div className="mx-auto grid max-w-7xl gap-9 px-5 py-16 md:px-8 md:py-20 lg:grid-cols-[minmax(0,1fr)_18rem] lg:items-end">
        <div>
          <p className="eyebrow">{eyebrow}</p>
          <h1 className="display editorial-hero-title mt-7 text-balance text-5xl leading-[0.92] md:text-8xl">
            {title}
          </h1>
        </div>
        <div className="editorial-hero-aside">
          <p className="font-mono text-[10px] uppercase tracking-[0.28em] text-accent">{rightLabel}</p>
          <p className="mt-4 text-sm leading-7 text-graphite">{description}</p>
        </div>
      </div>
    </section>
  );
}

export function EditorialGrid({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return <section className={`editorial-grid mx-auto max-w-7xl px-5 md:px-8 ${className}`}>{children}</section>;
}

export function EditorialBackdrop({
  children,
  word,
  className = "",
}: {
  children: React.ReactNode;
  word: string;
  className?: string;
}) {
  return (
    <section className={`editorial-backdrop ${className}`} data-word={word}>
      <div className="editorial-backdrop-inner mx-auto max-w-7xl px-5 md:px-8">{children}</div>
    </section>
  );
}

export function FeatureShelf({
  eyebrow,
  title,
  children,
  className = "",
}: {
  eyebrow: string;
  title: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section className={`feature-shelf mx-auto max-w-7xl px-5 md:px-8 ${className}`}>
      <div className="feature-shelf-head">
        <p className="eyebrow">{eyebrow}</p>
        <h2 className="display-light mt-4 text-4xl leading-none text-ink md:text-5xl">{title}</h2>
      </div>
      {children}
    </section>
  );
}

export function AdminWorktop({
  eyebrow,
  title,
  children,
}: {
  eyebrow: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="admin-worktop">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1 className="display mt-5 text-5xl leading-[0.92] md:text-7xl">{title}</h1>
      </div>
      {children}
    </div>
  );
}
