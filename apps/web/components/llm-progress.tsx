"use client";

import { useCallback, useState } from "react";
import { AlertCircle, CheckCircle2, Circle, Loader2, X } from "lucide-react";

export type LlmProgressStep = {
  id: string;
  label: string;
  detail: string;
  status: "pending" | "active" | "done" | "error";
};

export type LlmProgressState = {
  title: string;
  summary: string;
  active: boolean;
  steps: LlmProgressStep[];
};

export function useLlmProgress() {
  const [progress, setProgress] = useState<LlmProgressState | null>(null);

  const start = useCallback((title: string, steps: Array<Omit<LlmProgressStep, "status">>) => {
    setProgress({
      title,
      summary: "准备 LLM 输入契约",
      active: true,
      steps: steps.map((step, index) => ({ ...step, status: index === 0 ? "active" : "pending" })),
    });
  }, []);

  const setStep = useCallback((id: string, status: LlmProgressStep["status"], detail?: string) => {
    setProgress((current) => current ? {
      ...current,
      steps: current.steps.map((step) => step.id === id ? { ...step, status, detail: detail ?? step.detail } : step),
    } : current);
  }, []);

  const activate = useCallback((id: string, detail?: string) => {
    setProgress((current) => current ? {
      ...current,
      summary: detail ?? current.steps.find((step) => step.id === id)?.detail ?? current.summary,
      steps: current.steps.map((step) => {
        if (step.id === id) return { ...step, status: "active", detail: detail ?? step.detail };
        return step.status === "active" ? { ...step, status: "done" } : step;
      }),
    } : current);
  }, []);

  const complete = useCallback((summary: string) => {
    setProgress((current) => current ? {
      ...current,
      active: false,
      summary,
      steps: current.steps.map((step) => step.status === "error" ? step : { ...step, status: "done" }),
    } : current);
  }, []);

  const fail = useCallback((summary: string) => {
    setProgress((current) => current ? {
      ...current,
      active: false,
      summary,
      steps: current.steps.map((step) => step.status === "active" ? { ...step, status: "error" } : step),
    } : current);
  }, []);

  const reset = useCallback(() => setProgress(null), []);

  return { progress, start, setStep, activate, complete, fail, reset };
}

export function LlmProgressPanel({ state, onDismiss }: { state: LlmProgressState | null; onDismiss?: () => void }) {
  if (!state) return null;
  const weightedDone = state.steps.reduce((total, step) => total + (step.status === "done" ? 1 : step.status === "active" ? 0.45 : 0), 0);
  const percent = Math.round((weightedDone / Math.max(state.steps.length, 1)) * 100);

  return (
    <section className="llm-progress-panel" aria-live="polite" aria-label="LLM 任务进度">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-accent">LLM Runbook</p>
          <h3 className="mt-2 text-lg font-semibold text-ink">{state.title}</h3>
          <p className="mt-1 text-sm leading-6 text-graphite">{state.summary}</p>
        </div>
        {onDismiss ? (
          <button type="button" className="llm-progress-close" onClick={onDismiss} aria-label="关闭进度悬窗">
            <X size={15} />
          </button>
        ) : null}
      </div>
      <div className="llm-progress-meter-row">
        <div className="llm-progress-meter">
          <div className="llm-progress-meter-fill" style={{ width: `${percent}%` }} />
        </div>
        <span>{percent}%</span>
      </div>
      <ol className="llm-progress-steps">
        {state.steps.map((step) => (
          <li key={step.id} className={`llm-progress-step is-${step.status}`}>
            <span className="llm-progress-icon">
              {step.status === "done" ? <CheckCircle2 size={15} /> : null}
              {step.status === "active" ? <Loader2 className="animate-spin" size={15} /> : null}
              {step.status === "error" ? <AlertCircle size={15} /> : null}
              {step.status === "pending" ? <Circle size={15} /> : null}
            </span>
            <span>
              <span className="block text-sm font-medium text-ink">{step.label}</span>
              <span className="block text-xs leading-5 text-graphite">{step.detail}</span>
            </span>
          </li>
        ))}
      </ol>
    </section>
  );
}
