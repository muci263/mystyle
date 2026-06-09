"use client";

import Link from "next/link";
import { motion, useMotionTemplate, useMotionValue, useReducedMotion, useScroll, useSpring, useTransform } from "framer-motion";
import { ArrowDown, ArrowUpRight, Blocks, BrainCircuit, Database, GitBranch, PencilLine, PlayCircle } from "lucide-react";
import { PointerEvent as ReactPointerEvent, ReactNode, useCallback, useEffect, useRef, useState } from "react";
import { KnowledgeGraphScene } from "@/components/knowledge-graph-scene";
import type { KnowledgeGraphFocusedNode } from "@/components/knowledge-graph-scene";
import type { HomeView, Project } from "@/lib/api";

const labIcons = [PlayCircle, Database, BrainCircuit, Blocks, GitBranch];

export function ScrollPortfolio({ home }: { home: HomeView }) {
  const reduceMotion = Boolean(useReducedMotion());
  const { featuredProjects, moduleDemos, profile, skills } = home;
  const [focusedNode, setFocusedNode] = useState<KnowledgeGraphFocusedNode | null>(null);
  const heroRef = useRef<HTMLElement>(null);
  const labRef = useRef<HTMLElement>(null);
  const hideNodeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const { scrollYProgress: pageScroll } = useScroll();
  const smoothPageProgress = useSpring(pageScroll, { stiffness: 120, damping: 26, mass: 0.28 });
  const { scrollYProgress: heroScroll } = useScroll({
    target: heroRef,
    offset: ["start start", "end start"],
  });
  const smoothHero = useSpring(heroScroll, { stiffness: 100, damping: 25, mass: 0.35 });
  const heroContentY = useTransform(smoothHero, [0, 1], [0, -72]);
  const heroContentOpacity = useTransform(smoothHero, [0, 0.66, 1], [1, 0.82, 0]);
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
  const heroTags = Array.from(new Set([...profile.tags, ...skills.flatMap((group) => group.items)])).slice(0, 4);
  const activeNode = focusedNode;
  const activeLinks = focusedNode?.relatedCount ?? 0;

  const handleNodeFocus = useCallback((node: KnowledgeGraphFocusedNode) => {
    if (hideNodeTimerRef.current) {
      clearTimeout(hideNodeTimerRef.current);
      hideNodeTimerRef.current = null;
    }
    setFocusedNode(node);
  }, []);

  const handleNodeBlur = useCallback(() => {
    if (hideNodeTimerRef.current) {
      clearTimeout(hideNodeTimerRef.current);
    }
    hideNodeTimerRef.current = setTimeout(() => setFocusedNode(null), 3000);
  }, []);

  useEffect(() => {
    return () => {
      if (hideNodeTimerRef.current) {
        clearTimeout(hideNodeTimerRef.current);
      }
    };
  }, []);

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
        className="hero-stage relative isolate min-h-screen overflow-hidden"
        onPointerMove={moveHeroSpotlight}
        onPointerLeave={resetHeroSpotlight}
      >
        <div className="knowledge-graph-backdrop">
          <KnowledgeGraphScene graph={home.knowledgeGraph} onNodeFocus={handleNodeFocus} onNodeBlur={handleNodeBlur} />
        </div>
        {!reduceMotion && (
          <motion.div aria-hidden="true" className="hero-spotlight pointer-events-none absolute inset-0 hidden md:block" style={{ background: spotlight }} />
        )}
        <div aria-hidden="true" className="hero-scan pointer-events-none absolute inset-0 hidden md:block" />
        <motion.div
          className="knowledge-hero-overlay scroll-transform pointer-events-none relative z-10 mx-auto flex min-h-screen max-w-[100rem] flex-col px-5 pb-12 pt-28 md:px-8"
          style={reduceMotion ? undefined : { opacity: heroContentOpacity, y: heroContentY }}
        >
          <Reveal disabled={reduceMotion}>
            <div className="flex items-center justify-between text-[11px] uppercase tracking-[0.23em] text-white/56">
              <span>Knowledge Graph</span>
              <span className="hidden md:block">Shanxi University / Software Engineering</span>
            </div>
          </Reveal>

          <div className="knowledge-hero-layout">
            <Reveal disabled={reduceMotion} className="knowledge-side-panel knowledge-profile-panel pointer-events-auto">
              <p className="knowledge-panel-kicker">Personal Core</p>
              <h1 className="display knowledge-panel-title">{profile.name}</h1>
              <div className="knowledge-panel-lines">
                <p>{profile.title}</p>
                <p>{profile.education}</p>
              </div>
              <div className="mt-5 flex flex-wrap gap-2">
                {heroTags.map((tag) => (
                  <span key={tag} className="knowledge-hero-tag">
                    {tag}
                  </span>
                ))}
              </div>
            </Reveal>

            {activeNode ? (
              <Reveal disabled={reduceMotion} className="knowledge-side-panel knowledge-detail-panel pointer-events-auto">
                <div className="flex items-center justify-between gap-4">
                  <p className="knowledge-panel-kicker">{activeNode.nodeType}</p>
                  <span className="knowledge-link-count">{String(activeLinks).padStart(2, "0")} links</span>
                </div>
                <h2 className="display knowledge-detail-title">{activeNode.label}</h2>
                <p className="knowledge-detail-copy">{activeNode.summary || activeNode.content}</p>
                {activeNode.tags.length ? (
                  <div className="mt-5 flex flex-wrap gap-2">
                    {activeNode.tags.slice(0, 4).map((tag) => (
                      <span key={tag} className="knowledge-hero-tag">
                        {tag}
                      </span>
                    ))}
                  </div>
                ) : null}
                <Link
                  href={`/admin/knowledge-graph?nodeId=${encodeURIComponent(activeNode.nodeKey)}`}
                  className="knowledge-manage-link"
                >
                  <PencilLine size={14} />
                  管理节点
                </Link>
              </Reveal>
            ) : null}
          </div>
        </motion.div>
        <div className="absolute bottom-8 right-8 z-10 hidden items-center gap-3 font-mono text-[10px] uppercase tracking-[0.22em] text-white/45 lg:flex">
          <ArrowDown size={13} className="text-[#82aaff]" />
          Scroll
        </div>
      </section>

      <section className="home-evidence-section mx-auto max-w-7xl px-5 py-20 md:px-8 md:py-28">
        <Reveal disabled={reduceMotion}>
          <div className="mb-12 flex flex-col justify-between gap-8 border-b border-line pb-10 md:flex-row md:items-end">
            <div>
              <p className="eyebrow">Selected Evidence</p>
              <h2 className="display mt-7 max-w-3xl text-5xl leading-[1.02] md:text-7xl">项目证据</h2>
            </div>
            <Link href="/projects" className="text-link inline-flex items-center gap-2 text-sm font-medium">
              全部项目 <ArrowUpRight size={16} />
            </Link>
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
                  经典功能复现
                </h2>
              </div>
              <Link href="/lab" className="rounded-full bg-white px-6 py-3 text-sm font-medium text-ink">
                进入实验室
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
                  </Link>
                </Reveal>
              );
            })}
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

function Reveal({
  children,
  disabled,
  delay = 0,
  className,
}: {
  children: ReactNode;
  disabled: boolean;
  delay?: number;
  className?: string;
}) {
  return (
    <motion.div
      className={className}
      initial={disabled ? false : { opacity: 0, y: 46 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.18 }}
      transition={{ duration: disabled ? 0 : 0.62, delay: disabled ? 0 : delay, ease: [0.22, 1, 0.36, 1] }}
    >
      {children}
    </motion.div>
  );
}
