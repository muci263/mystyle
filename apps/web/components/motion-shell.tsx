"use client";

import { motion, useReducedMotion } from "framer-motion";

const moduleEase = [0.22, 1, 0.36, 1] as const;

export function PageTransition({ children }: { children: React.ReactNode }) {
  return <div className="page-transition-shell">{children}</div>;
}

export function FadeIn({
  children,
  delay = 0,
  className = "",
}: {
  children: React.ReactNode;
  delay?: number;
  className?: string;
}) {
  const reduceMotion = Boolean(useReducedMotion());

  return (
    <motion.div
      initial={reduceMotion ? false : { opacity: 0, y: 28 }}
      whileInView={reduceMotion ? { opacity: 1 } : { opacity: 1, y: 0 }}
      exit={reduceMotion ? { opacity: 0 } : { opacity: 0, y: -16 }}
      viewport={{ once: true, amount: 0.12, margin: "-80px" }}
      transition={{ duration: reduceMotion ? 0 : 0.64, delay: reduceMotion ? 0 : delay, ease: moduleEase }}
      className={className}
    >
      {children}
    </motion.div>
  );
}

export function MotionSurface({
  children,
  className = "",
  id,
}: {
  children: React.ReactNode;
  className?: string;
  id?: string;
}) {
  const reduceMotion = Boolean(useReducedMotion());

  return (
    <motion.article
      id={id}
      initial={reduceMotion ? false : { opacity: 0, y: 30, scale: 0.99 }}
      whileInView={reduceMotion ? { opacity: 1 } : { opacity: 1, y: 0, scale: 1 }}
      exit={reduceMotion ? { opacity: 0 } : { opacity: 0, y: -14, scale: 0.995 }}
      whileHover={reduceMotion ? undefined : { y: -3 }}
      viewport={{ once: true, amount: 0.12 }}
      transition={{ duration: reduceMotion ? 0 : 0.58, ease: moduleEase }}
      className={`surface enhanced-surface p-6 md:p-7 ${className}`}
    >
      {children}
    </motion.article>
  );
}
