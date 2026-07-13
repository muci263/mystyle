"use client";

import Link from "next/link";
import { motion, useMotionTemplate, useMotionValue, useReducedMotion, useSpring } from "framer-motion";
import { GraduationCap, Mail, PencilLine, Terminal } from "lucide-react";
import { PointerEvent as ReactPointerEvent, ReactNode, useCallback, useEffect, useRef, useState } from "react";
import { KnowledgeGraphScene } from "@/components/knowledge-graph-scene";
import type { KnowledgeGraphFocusedNode } from "@/components/knowledge-graph-scene";
import type { HomeView } from "@/lib/api";

export function ScrollPortfolio({ home }: { home: HomeView }) {
  const reduceMotion = Boolean(useReducedMotion());
  const { profile, skills } = home;
  const [focusedNode, setFocusedNode] = useState<KnowledgeGraphFocusedNode | null>(null);
  const heroRef = useRef<HTMLElement>(null);
  const hideNodeTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const rawGlowX = useMotionValue(1060);
  const rawGlowY = useMotionValue(360);
  const glowX = useSpring(rawGlowX, { stiffness: 110, damping: 28, mass: 0.28 });
  const glowY = useSpring(rawGlowY, { stiffness: 110, damping: 28, mass: 0.28 });
  const spotlight = useMotionTemplate`radial-gradient(470px circle at ${glowX}px ${glowY}px, rgba(37, 99, 235, 0.27), rgba(37, 99, 235, 0.07) 42%, transparent 72%)`;
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
      <section
        ref={heroRef}
        className="hero-stage relative isolate min-h-screen overflow-hidden"
        onPointerMove={moveHeroSpotlight}
        onPointerLeave={resetHeroSpotlight}
      >
        <div aria-hidden="true" className="knowledge-hero-wordmark">
          <span>Port</span>
          <span>folio</span>
        </div>
        <p className="knowledge-hero-note">Backend evidence that moves with the interview.</p>
        <p className="knowledge-hero-season">New graph system / 2026</p>
        <div className="knowledge-graph-backdrop">
          <KnowledgeGraphScene graph={home.knowledgeGraph} onNodeFocus={handleNodeFocus} onNodeBlur={handleNodeBlur} />
        </div>
        {!reduceMotion && (
          <motion.div aria-hidden="true" className="hero-spotlight pointer-events-none absolute inset-0 hidden md:block" style={{ background: spotlight }} />
        )}
        <div aria-hidden="true" className="hero-scan pointer-events-none absolute inset-0 hidden md:block" />
        <motion.div
          className="knowledge-hero-overlay scroll-transform pointer-events-none relative z-10 mx-auto flex min-h-screen max-w-[100rem] flex-col px-5 pb-12 pt-28 md:px-8"
        >
          <Reveal disabled={reduceMotion}>
            <div className="flex items-center justify-between text-[11px] uppercase tracking-[0.23em] text-white/56">
              <span>Knowledge Graph</span>
              <span className="hidden md:block">Shanxi University / Software Engineering</span>
            </div>
          </Reveal>

          <div className="knowledge-hero-layout">
            <Reveal disabled={reduceMotion} className="knowledge-side-panel knowledge-profile-panel pointer-events-auto">
              <div className="knowledge-profile-head">
                <span className="knowledge-avatar-mark">ZHR</span>
                <div>
                  <p className="knowledge-panel-kicker">Personal Core</p>
                  <h1 className="display knowledge-panel-title">{profile.name}</h1>
                </div>
              </div>
              <p className="knowledge-profile-summary">{profile.summary}</p>
              <div className="knowledge-panel-lines">
                <p>
                  <Terminal size={14} />
                  <span>{profile.title}</span>
                </p>
                <p>
                  <GraduationCap size={14} />
                  <span>{profile.education}</span>
                </p>
                <p>
                  <Mail size={14} />
                  <span>{profile.email}</span>
                </p>
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
      </section>
    </>
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
