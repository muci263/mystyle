"use client";

import Link from "next/link";
import { motion, useMotionTemplate, useMotionValue, useReducedMotion, useScroll, useSpring, useTransform } from "framer-motion";
import { ArrowDown, ArrowUpRight, Blocks, BrainCircuit, Database, GitBranch, PlayCircle } from "lucide-react";
import { PointerEvent as ReactPointerEvent, ReactNode, useRef } from "react";
import { HeroScene } from "@/components/hero-scene";
import type { HomeView, Project } from "@/lib/api";

const impact = [
  { value: "60+", title: "问题修复", copy: "真实接口异常与业务逻辑缺陷定位" },
  { value: "800ms", title: "查询响应", copy: "由 2s+ 优化至可量化表现" },
  { value: "-70%", title: "重复写入", copy: "视频进度缓存同步的结果" },
];

const labIcons = [PlayCircle, Database, BrainCircuit, Blocks, GitBranch];

export function ScrollPortfolio({ home }: { home: HomeView }) {
  const reduceMotion = Boolean(useReducedMotion());
  const { featuredProjects, moduleDemos, profile } = home;
  const heroRef = useRef<HTMLElement>(null);
  const labRef = useRef<HTMLElement>(null);
  const { scrollYProgress: pageScroll } = useScroll();
  const smoothPageProgress = useSpring(pageScroll, { stiffness: 120, damping: 26, mass: 0.28 });
  const { scrollYProgress: heroScroll } = useScroll({
    target: heroRef,
    offset: ["start start", "end start"],
  });
  const smoothHero = useSpring(heroScroll, { stiffness: 100, damping: 25, mass: 0.35 });
  const heroContentY = useTransform(smoothHero, [0, 1], [0, -72]);
  const heroContentOpacity = useTransform(smoothHero, [0, 0.66, 1], [1, 0.82, 0]);
  const heroCanvasScale = useTransform(smoothHero, [0, 1], [1, 1.14]);
  const heroCanvasY = useTransform(smoothHero, [0, 1], [0, 56]);
  const rawGlowX = useMotionValue(1060);
  const rawGlowY = useMotionValue(360);
  const glowX = useSpring(rawGlowX, { stiffness: 110, damping: 28, mass: 0.28 });
  const glowY = useSpring(rawGlowY, { stiffness: 110, damping: 28, mass: 0.28 });
  const spotlight = useMotionTemplate`radial-gradient(470px circle at ${glowX}px ${glowY}px, rgba(37, 99, 235, 0.27), rgba(37, 99, 235, 0.07) 42%, transparent 72%)`;
  const { scrollYProgress: labScroll } = useScroll({
    target: labRef,
    offset: ["start end", "center center"],
  });
  const smoothLab = useSpring(labScroll, { stiffness: 100, damping: 24, mass: 0.34 });
  const labBackgroundY = useTransform(smoothLab, [0, 1], [42, 0]);

  function moveHeroSpotlight(event: ReactPointerEvent<HTMLElement>) {
    if (reduceMotion) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    rawGlowX.set(event.clientX - bounds.left);
    rawGlowY.set(event.clientY - bounds.top);
  }

  function resetHeroSpotlight() {
    const bounds = heroRef.current?.getBoundingClientRect();
    if (!bounds) return;
    rawGlowX.set(bounds.width * 0.7);
    rawGlowY.set(bounds.height * 0.46);
  }

  return (
    <>
      {!reduceMotion && (
        <div aria-hidden="true" className="fixed right-5 top-1/2 z-50 hidden h-28 w-px -translate-y-1/2 bg-black/10 lg:block">
          <motion.div className="h-full origin-top bg-accent" style={{ scaleY: smoothPageProgress }} />
        </div>
      )}

      <section
        ref={heroRef}
        className="hero-stage relative isolate min-h-[calc(100svh-72px)] overflow-hidden"
        onPointerMove={moveHeroSpotlight}
        onPointerLeave={resetHeroSpotlight}
      >
        <motion.div
          className="scroll-transform absolute inset-0"
          style={reduceMotion ? undefined : { scale: heroCanvasScale, y: heroCanvasY }}
        >
          <HeroScene />
        </motion.div>
        {!reduceMotion && (
          <motion.div aria-hidden="true" className="hero-spotlight pointer-events-none absolute inset-0 hidden md:block" style={{ background: spotlight }} />
        )}
        <div aria-hidden="true" className="hero-scan pointer-events-none absolute inset-0 hidden md:block" />
        <motion.div
          className="scroll-transform relative z-10 mx-auto flex min-h-[calc(100svh-72px)] max-w-7xl flex-col justify-between px-5 py-9 md:px-8 md:pb-12 md:pt-11"
          style={reduceMotion ? undefined : { opacity: heroContentOpacity, y: heroContentY }}
        >
          <Reveal disabled={reduceMotion}>
            <div className="flex items-center justify-between text-[11px] uppercase tracking-[0.23em] text-white/56">
              <span>Java Backend Engineer</span>
              <span className="hidden md:block">Shanxi University / 2026 Portfolio</span>
            </div>
          </Reveal>

          <div className="max-w-5xl pb-3">
            <Reveal disabled={reduceMotion}>
              <p className="font-mono text-[11px] uppercase tracking-[0.3em] text-[#82aaff]">{profile.name} / Engineering Evidence</p>
              <h1 className="display mt-7 text-balance text-[clamp(3.6rem,9.2vw,8.4rem)] leading-[0.92] text-white">
                Backend systems,
                <br />
                made visible.
              </h1>
            </Reveal>
            <Reveal disabled={reduceMotion} delay={0.11}>
              <div className="mt-9 flex flex-col gap-8 border-t border-white/16 pt-7 md:flex-row md:items-end md:justify-between">
                <p className="max-w-lg text-base leading-8 text-white/68">
                  {profile.summary} 将项目结论、关键模块和工程部署能力重现为可浏览的面试证据。
                </p>
                <div className="flex flex-wrap gap-3">
                  <Link href="/evidence" className="rounded-full bg-white px-6 py-3 text-sm font-medium text-ink transition hover:bg-[#dbe7ff]">
                    项目证据
                  </Link>
                  <Link href="/evidence" className="rounded-full border border-white/26 px-6 py-3 text-sm font-medium text-white transition hover:border-white/60">
                    Live Modules
                  </Link>
                </div>
              </div>
            </Reveal>
          </div>
        </motion.div>
        <div className="absolute bottom-8 right-8 z-10 hidden items-center gap-3 font-mono text-[10px] uppercase tracking-[0.22em] text-white/45 lg:flex">
          <ArrowDown size={13} className="text-[#82aaff]" />
          Scroll
        </div>
      </section>

      <section className="border-b border-line bg-white">
        <div className="mx-auto grid max-w-7xl px-5 py-16 md:grid-cols-3 md:px-8 md:py-24">
          {impact.map((item, index) => (
            <Reveal key={item.title} disabled={reduceMotion} delay={index * 0.06}>
              <article className="border-line py-8 md:border-l md:px-9 md:py-0 first:md:border-l-0 first:md:pl-0">
                <p className="metric-number">{item.value}</p>
                <p className="mt-6 text-lg font-medium">{item.title}</p>
                <p className="mt-3 max-w-xs text-sm leading-7 text-graphite">{item.copy}</p>
              </article>
            </Reveal>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-5 py-20 md:px-8 md:py-32">
        <Reveal disabled={reduceMotion}>
          <div className="mb-20 flex flex-col justify-between gap-8 border-b border-line pb-10 md:flex-row md:items-end">
            <div>
              <p className="eyebrow">Selected Evidence</p>
              <h2 className="display mt-7 max-w-3xl text-5xl leading-[1.02] md:text-7xl">项目不是列表，<br />是证明路径。</h2>
            </div>
            <p className="max-w-xs text-sm leading-7 text-graphite">
              两项真实实践，从业务问题进入技术选择，再连接到可展示模块。
            </p>
          </div>
        </Reveal>

        <div>
          {featuredProjects.map((project, index) => (
            <ProjectExhibit key={project.slug} project={project} disabled={reduceMotion} index={index} />
          ))}
        </div>
      </section>

      <section ref={labRef} className="technical-night console-grid relative overflow-hidden">
        <motion.p
          aria-hidden="true"
          className="scroll-transform pointer-events-none absolute -right-6 top-12 hidden select-none text-[15rem] font-semibold leading-none text-white/[0.025] lg:block"
          style={reduceMotion ? undefined : { y: labBackgroundY }}
        >
          RUNTIME
        </motion.p>
        <div className="relative mx-auto max-w-7xl px-5 py-20 md:px-8 md:py-28">
          <Reveal disabled={reduceMotion}>
            <div className="flex flex-col justify-between gap-8 md:flex-row md:items-end">
              <div>
                <p className="eyebrow technical-label">Runtime Lab</p>
                <h2 className="display mt-7 max-w-4xl text-5xl leading-[1.03] text-white md:text-7xl">
                  不描述工作，<br />直接运行它。
                </h2>
              </div>
              <Link href="/evidence" className="rounded-full bg-white px-6 py-3 text-sm font-medium text-ink">
                打开证据系统
              </Link>
            </div>
          </Reveal>
          <div className="mt-16 grid gap-3 md:grid-cols-5">
            {moduleDemos.map((module, index) => {
              const Icon = labIcons[index];
              return (
                <Reveal key={module.slug} disabled={reduceMotion} delay={index * 0.05}>
                  <Link href={`/lab/${module.slug}`} className="console-card group block min-h-[208px] p-5 transition hover:border-[#82aaff]/55">
                    <Icon size={18} className="technical-label" />
                    <div className="mt-9 flex items-center gap-2">
                      <span className="runtime-dot" />
                      <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-[#82aaff]">{module.demoType}</p>
                    </div>
                    <h3 className="mt-4 text-base font-medium text-white">{module.name}</h3>
                    <p className="mt-2 text-xs leading-6 text-white/55">{module.title}</p>
                  </Link>
                </Reveal>
              );
            })}
          </div>
        </div>
      </section>

      <section className="mx-auto grid max-w-7xl gap-14 px-5 py-20 md:px-8 md:py-32 lg:grid-cols-[0.82fr_1.18fr]">
        <Reveal disabled={reduceMotion}>
          <div>
            <p className="eyebrow">Adaptive Interview</p>
            <h2 className="display mt-7 text-5xl leading-[1.04] md:text-7xl">针对岗位，<br />重排证据。</h2>
            <p className="mt-7 max-w-sm text-base leading-8 text-graphite">
              粘贴岗位 JD，系统将根据真实经历重组个人介绍、项目顺序和建议演示模块。
            </p>
            <Link href="/interview-kit" className="primary-action mt-9 px-6 py-3 text-sm font-medium">
              进入面试助手 <ArrowUpRight size={16} />
            </Link>
          </div>
        </Reveal>
        <Reveal disabled={reduceMotion} delay={0.08}>
          <div className="surface p-6 md:p-8">
            <div className="flex items-center justify-between border-b border-line pb-5">
              <span className="font-mono text-[11px] uppercase tracking-[0.2em] text-accent">Matching Preview</span>
              <span className="metric-pill">86 / 100</span>
            </div>
            <p className="mt-8 text-sm text-graphite">输入岗位 JD</p>
            <div className="mt-3 min-h-32 border border-line bg-stonepaper p-4 text-sm leading-7 text-graphite">
              Java 后端开发 · Spring Boot · Redis · 微服务 · AI 应用工程化...
            </div>
            <div className="mt-7 grid gap-3 sm:grid-cols-3">
              {["Redis 优化", "微服务项目", "Agent Demo"].map((item) => (
                <div key={item} className="border border-line p-4 text-sm font-medium">{item}</div>
              ))}
            </div>
          </div>
        </Reveal>
      </section>

      <section className="border-t border-line px-5 py-16 md:px-8">
        <div className="mx-auto flex max-w-7xl flex-col justify-between gap-7 md:flex-row md:items-end">
          <h2 className="display-light max-w-3xl text-4xl leading-tight md:text-6xl">查看完整履历，或从技术博客继续深入。</h2>
          <div className="flex gap-3">
            <Link href="/resume" className="secondary-action px-6 py-3 text-sm font-medium">在线履历</Link>
            <Link href="/blog" className="primary-action px-6 py-3 text-sm font-medium">技术博客</Link>
          </div>
        </div>
      </section>
    </>
  );
}

function ProjectExhibit({ project, disabled, index }: { project: Project; disabled: boolean; index: number }) {
  const ref = useRef<HTMLElement>(null);
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"],
  });
  const smoothProgress = useSpring(scrollYProgress, { stiffness: 105, damping: 25, mass: 0.34 });
  const contentY = useTransform(smoothProgress, [0, 0.43, 1], [42, 0, -22]);
  const contentOpacity = useTransform(smoothProgress, [0, 0.18, 0.78, 1], [0, 1, 1, 0.62]);
  const labelX = useTransform(smoothProgress, [0, 0.42], [-16, 0]);
  const evidenceLine = useTransform(smoothProgress, [0, 0.38, 0.86], [0, 1, 0.72]);

  return (
    <article ref={ref} className="relative grid gap-8 border-b border-line py-14 md:py-20 lg:grid-cols-[0.72fr_1.28fr]">
      <motion.span
        aria-hidden="true"
        className="absolute bottom-[-1px] left-0 h-[2px] w-40 origin-left bg-accent"
        style={disabled ? undefined : { scaleX: evidenceLine }}
      />
      <motion.div
        className="scroll-transform flex items-start justify-between lg:block"
        style={disabled ? undefined : { opacity: contentOpacity, x: labelX }}
      >
        <p className="font-mono text-xs uppercase tracking-[0.22em] text-accent">Case / {project.index}</p>
        <p className="mt-0 text-sm text-graphite lg:mt-7">{project.role}</p>
      </motion.div>
      <motion.div
        className="scroll-transform"
        style={disabled ? undefined : { opacity: contentOpacity, y: contentY }}
        transition={{ delay: index * 0.02 }}
      >
        <Link href={`/projects/${project.slug}`} className="group">
          <h3 className="display-light max-w-4xl text-4xl leading-tight transition group-hover:text-accent md:text-6xl">
            {project.name}
          </h3>
        </Link>
        <p className="mt-6 max-w-2xl text-base leading-8 text-graphite">{project.summary}</p>
        <div className="mt-10 flex flex-col gap-6 border-t border-line pt-7 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-wrap gap-2">
            {project.tech.map((tech) => (
              <span key={tech} className="chapter-tag">{tech}</span>
            ))}
          </div>
          <div className="flex shrink-0 flex-wrap gap-2">
            {project.metrics.map((metric) => (
              <span key={metric} className="metric-pill">{metric}</span>
            ))}
          </div>
        </div>
        <Link href={`/projects/${project.slug}`} className="text-link mt-8 inline-flex items-center gap-2 text-sm font-medium">
          展开工程证据 <ArrowUpRight size={16} />
        </Link>
      </motion.div>
    </article>
  );
}

function Reveal({ children, disabled, delay = 0 }: { children: ReactNode; disabled: boolean; delay?: number }) {
  return (
    <motion.div
      initial={disabled ? false : { opacity: 0, y: 46 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.18 }}
      transition={{ duration: disabled ? 0 : 0.62, delay: disabled ? 0 : delay, ease: [0.22, 1, 0.36, 1] }}
    >
      {children}
    </motion.div>
  );
}
