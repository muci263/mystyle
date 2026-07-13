"use client";

import { ChangeEvent, FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { ArrowRight, CheckCircle2, Clipboard, FileText, Loader2, Mic, Save, Send, Upload, X } from "lucide-react";
import { MarkdownRenderer } from "@/components/markdown-renderer";
import { Surface } from "@/components/site-shell";
import { apiGet, apiPost, apiPostNdjson } from "@/lib/api";
import type {
  InterviewAnswer,
  InterviewFinalizeResponse,
  InterviewTurnResponse,
  JdAnalysisResponse,
  AgentWorkflowRun,
  Project,
  ResumeOptimizeResponse,
  ResumeDraftView,
  ResumeExtractedText,
  ResumeUploadTask,
} from "@/lib/api";

const defaultJd = "Java 后端实习，要求 Spring Boot、Redis、MySQL、微服务，有 AI 或 RAG 经验加分";
const maxResumeChars = 12000;
const maxResumeFileBytes = 5 * 1024 * 1024;
const memoryStorageKey = "mystyle.interview-kit.memory.v1";
const initialAssistantMessages: ChatMessage[] = [
  { id: "assistant-intro", role: "system", text: "语言助手已就绪。可以先自由提问，后续可接入知识库。", status: "active" },
];
const initialInterviewMessages: ChatMessage[] = [
  { id: "interview-intro", role: "system", text: "模拟面试待启动。左侧填写 JD 与简历后开始。", status: "active" },
];

type InterviewPhase = "setup" | "interviewing" | "generating" | "done";
type ResultTab = "summary" | "resume" | "feedback";
type ResumeOptimizeStatus = "idle" | "running" | "done" | "error";
type ChatChannel = "assistant" | "interview";
type ChatMessage = {
  id: string;
  role: "system" | "assistant" | "user" | "result";
  text: string;
  status?: "active" | "done" | "error";
};

type StoredInterviewKitMemory = {
  jd: string;
  resumeText: string;
  resumeSource: string;
  analysis: JdAnalysisResponse | null;
  phase: InterviewPhase;
  currentQuestion: InterviewTurnResponse | null;
  history: InterviewAnswer[];
  result: InterviewFinalizeResponse | null;
  resumeOptimization: ResumeOptimizeResponse | null;
  resumeOptimizeStatus: ResumeOptimizeStatus;
  activeChat: ChatChannel;
  assistantMessages: ChatMessage[];
  interviewMessages: ChatMessage[];
};

export function JdAdapterClient() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [jd, setJd] = useState(defaultJd);
  const [resumeText, setResumeText] = useState("");
  const [resumeSource, setResumeSource] = useState("未选择");
  const [analysis, setAnalysis] = useState<JdAnalysisResponse | null>(null);
  const [phase, setPhase] = useState<InterviewPhase>("setup");
  const [currentQuestion, setCurrentQuestion] = useState<InterviewTurnResponse | null>(null);
  const [answerDraft, setAnswerDraft] = useState("");
  const [assistantDraft, setAssistantDraft] = useState("");
  const [assistantLoading, setAssistantLoading] = useState(false);
  const [voiceListening, setVoiceListening] = useState(false);
  const [history, setHistory] = useState<InterviewAnswer[]>([]);
  const [result, setResult] = useState<InterviewFinalizeResponse | null>(null);
  const [resumeOptimization, setResumeOptimization] = useState<ResumeOptimizeResponse | null>(null);
  const [resumeOptimizeStatus, setResumeOptimizeStatus] = useState<ResumeOptimizeStatus>("idle");
  const [resumeOptimizeError, setResumeOptimizeError] = useState("");
  const [resultOpen, setResultOpen] = useState(false);
  const [resultTab, setResultTab] = useState<ResultTab>("summary");
  const [assetLoading, setAssetLoading] = useState(true);
  const [assetError, setAssetError] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [copyMessage, setCopyMessage] = useState("");
  const [saveMessage, setSaveMessage] = useState("");
  const [activeChat, setActiveChat] = useState<ChatChannel>("assistant");
  const [assistantMessages, setAssistantMessages] = useState<ChatMessage[]>(initialAssistantMessages);
  const [interviewMessages, setInterviewMessages] = useState<ChatMessage[]>(initialInterviewMessages);
  const [memoryReady, setMemoryReady] = useState(false);
  const messageCounterRef = useRef(0);
  const dialogStreamRef = useRef<HTMLDivElement | null>(null);
  const recognitionRef = useRef<any>(null);

  useEffect(() => {
    const stored = readInterviewKitMemory();
    if (stored) {
      setJd(stored.jd || defaultJd);
      setResumeText(stored.resumeText || "");
      setResumeSource(stored.resumeSource || "未选择");
      setAnalysis(stored.analysis);
      setPhase(stored.phase === "generating" ? "interviewing" : stored.phase);
      setCurrentQuestion(stored.currentQuestion);
      setHistory(stored.history);
      setResult(stored.result);
      setResumeOptimization(stored.resumeOptimization);
      setResumeOptimizeStatus(stored.resumeOptimizeStatus === "running" ? "idle" : stored.resumeOptimizeStatus);
      setActiveChat(stored.activeChat);
      setAssistantMessages(stored.assistantMessages.length ? stored.assistantMessages : initialAssistantMessages);
      setInterviewMessages(stored.interviewMessages.length ? stored.interviewMessages : initialInterviewMessages);
    }
    setMemoryReady(true);
  }, []);

  useEffect(() => {
    if (!memoryReady) return;
    const payload: StoredInterviewKitMemory = {
      jd,
      resumeText,
      resumeSource,
      analysis,
      phase,
      currentQuestion,
      history,
      result,
      resumeOptimization,
      resumeOptimizeStatus,
      activeChat,
      assistantMessages: normalizeMessagesForStorage(assistantMessages),
      interviewMessages: normalizeMessagesForStorage(interviewMessages),
    };
    try {
      window.localStorage.setItem(memoryStorageKey, JSON.stringify(payload));
    } catch {
      // Local memory is a convenience layer; storage failures should not block the assistant.
    }
  }, [
    activeChat,
    analysis,
    assistantMessages,
    currentQuestion,
    history,
    interviewMessages,
    jd,
    memoryReady,
    phase,
    result,
    resumeOptimization,
    resumeOptimizeStatus,
    resumeSource,
    resumeText,
  ]);

  useEffect(() => {
    let active = true;

    async function loadAssets() {
      setAssetLoading(true);
      setAssetError("");
      try {
        const projectAssets = await apiGet<Project[]>("/public/projects");
        if (!active) return;
        setProjects(projectAssets);
      } catch (exception) {
        if (!active) return;
        setAssetError(exception instanceof Error ? exception.message : "经历资产加载失败");
      } finally {
        if (active) {
          setAssetLoading(false);
        }
      }
    }

    loadAssets();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const stream = dialogStreamRef.current;
    if (!stream) return;
    stream.scrollTop = stream.scrollHeight;
  }, [activeChat, assistantMessages, interviewMessages]);

  const assetHints = useMemo(() => {
    if (!analysis) return [];
    return [
      ...analysis.projectRecommendations.slice(0, 3).map((item) => item.name),
      ...analysis.moduleRecommendations.slice(0, 2).map((item) => item.title),
    ];
  }, [analysis]);

  const orderedProjects = useMemo(() => {
    if (!analysis) return projects.slice(0, 3);
    return analysis.projectRecommendations
      .map((recommendation) => projects.find((project) => project.slug === recommendation.slug))
      .filter((project): project is Project => Boolean(project));
  }, [analysis, projects]);

  const dialogResult = useMemo(
    () => result ?? resultFromResumeOptimization(resumeOptimization, history.length),
    [history.length, result, resumeOptimization],
  );
  const activeMessages = activeChat === "assistant" ? assistantMessages : interviewMessages;

  async function readResumeFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setError("");
    try {
      if (file.size > maxResumeFileBytes) {
        setError("简历文件不能超过 5MB。");
        return;
      }
      const content = await resumeTextFromFile(file);
      if (content.trim().length > maxResumeChars) {
        setError(`简历文本超过 ${maxResumeChars} 字符，请压缩后再上传。`);
        return;
      }
      setResumeText(content);
      setResumeSource(file.name);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "简历文件读取失败");
    } finally {
      event.target.value = "";
    }
  }

  async function useDraftResume() {
    setError("");
    try {
      const draft = await apiGet<ResumeDraftView>("/admin/resume/draft");
      const draftText = resumeTextFromDraft(draft);
      if (!draftText.trim()) {
        setError("当前草稿为空，请上传或粘贴简历内容。");
        return "";
      }
      setResumeText(draftText);
      setResumeSource("当前简历草稿");
      return draftText;
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "当前草稿读取失败";
      setError(message);
      return "";
    }
  }

  async function startInterview(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    setLoading(true);
    setError("");
    setResult(null);
    setResumeOptimization(null);
    setResumeOptimizeStatus("idle");
    setResumeOptimizeError("");
    setResultOpen(false);
    setHistory([]);
    setAnswerDraft("");
    setInterviewMessages([]);
    setActiveChat("interview");
    try {
      pushMessage("interview", "system", "确认简历来源。", "active");
      let finalResumeText = resumeText.trim();
      if (!finalResumeText) {
        finalResumeText = await useDraftResume();
      }
      if (!finalResumeText) {
        throw new Error("请先上传、粘贴简历，或使用当前简历草稿。");
      }
      pushMessage("interview", "system", "简历文本已准备。", "done");

      pushMessage("interview", "system", "正在分析岗位 JD。", "active");
      const nextAnalysis = await apiPost<JdAnalysisResponse>("/jd/analyze", {
        jd,
        variantName: "面试助手会话版本",
      });
      setAnalysis(nextAnalysis);
      pushMessage("interview", "system", `${nextAnalysis.role}，匹配度 ${nextAnalysis.matchScore} / 100。`, "done");
      void optimizeResumeInBackground(finalResumeText, nextAnalysis);

      pushMessage("interview", "system", "正在生成第一道面试问题。", "active");
      const question = await apiPost<InterviewTurnResponse>("/llm/interview/session/question", {
        jdText: jd,
        resumeText: finalResumeText,
        targetRole: nextAnalysis.role,
        assetHints: [
          ...nextAnalysis.projectRecommendations.slice(0, 3).map((item) => item.name),
          ...nextAnalysis.moduleRecommendations.slice(0, 2).map((item) => item.title),
        ],
        history: [],
        round: 1,
      });
      setCurrentQuestion(question);
      setPhase("interviewing");
      await streamAssistantQuestion(question);
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "模拟面试启动失败";
      setError(message);
      pushMessage("interview", "system", message, "error");
      setPhase("setup");
    } finally {
      setLoading(false);
    }
  }

  async function submitAnswerAndContinue() {
    const nextHistory = appendCurrentAnswer();
    if (!nextHistory) return;
    setLoading(true);
    setError("");
    pushMessage("interview", "user", nextHistory[nextHistory.length - 1].answer, "done");
    try {
      pushMessage("interview", "system", "正在生成下一道追问。", "active");
      const question = await apiPost<InterviewTurnResponse>("/llm/interview/session/question", {
        jdText: jd,
        resumeText,
        targetRole: analysis?.role,
        assetHints,
        history: nextHistory,
        round: nextHistory.length + 1,
      });
      setCurrentQuestion(question);
      setAnswerDraft("");
      await streamAssistantQuestion(question);
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "下一题生成失败";
      setError(message);
      pushMessage("interview", "system", message, "error");
    } finally {
      setLoading(false);
    }
  }

  async function finalizeInterview() {
    const nextHistory = answerDraft.trim() ? appendCurrentAnswer() : history;
    if (!nextHistory || nextHistory.length === 0) {
      setError("请至少完成一轮回答后再生成结果。");
      return;
    }
    setLoading(true);
    setError("");
    setPhase("generating");
    if (answerDraft.trim()) {
      pushMessage("interview", "user", answerDraft.trim(), "done");
    }
    pushMessage("interview", "system", "正在生成面试结果与适配简历。", "active");
    try {
      const finalResult = await apiPostNdjson<InterviewFinalizeResponse>(
        "/llm/interview/session/finalize/stream",
        {
          jdText: jd,
          resumeText,
          targetRole: analysis?.role,
          assetHints,
          history: nextHistory,
        },
        (event) => {
          if (event.type === "progress") {
            pushMessage("interview", "system", event.message, "active");
          }
        },
      );
      setResult(finalResult);
      setResultOpen(false);
      setPhase("done");
      pushMessage("interview", "result", `已生成 ${finalResult.questionCount} 轮复盘和 JD 适配简历。`, "done");
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "结果生成失败";
      setError(message);
      pushMessage("interview", "system", message, "error");
      setPhase("interviewing");
    } finally {
      setLoading(false);
      setAnswerDraft("");
    }
  }

  async function askLanguageAssistant() {
    const question = assistantDraft.trim();
    if (!question || assistantLoading) return;
    setAssistantLoading(true);
    setError("");
    setAssistantDraft("");
    pushMessage("assistant", "user", question, "done");
    const answerId = pushMessage("assistant", "assistant", "", "active");
    let streamedAnswer = "";
    try {
      const run = await apiPostNdjson<AgentWorkflowRun>(
        "/lab/agent-workflow/run/stream",
        {
          question,
          templateId: "rag-question",
          memory: buildAssistantMemory({
            assistantMessages,
            interviewMessages,
            history,
            analysis,
            jd,
            resumeSource,
          }),
        },
        async (event) => {
          if (event.type !== "delta") return;
          streamedAnswer += event.message;
          replaceMessage("assistant", answerId, {
            text: cleanAssistantAnswer(streamedAnswer),
            status: "active",
          });
          await nextPaint();
        },
      );
      const answer = cleanAssistantAnswer(run.answer || streamedAnswer);
      replaceMessage("assistant", answerId, { text: answer, status: "done" });
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "语言助手请求失败";
      setError(message);
      replaceMessage("assistant", answerId, { text: message, status: "error" });
    } finally {
      setAssistantLoading(false);
    }
  }

  function startVoiceInput() {
    if (voiceListening) {
      recognitionRef.current?.stop?.();
      setVoiceListening(false);
      return;
    }
    const SpeechRecognitionClass = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognitionClass) {
      pushMessage("assistant", "system", "当前浏览器不支持语音输入。", "error");
      return;
    }
    const recognition = new SpeechRecognitionClass();
    recognition.lang = "zh-CN";
    recognition.interimResults = true;
    recognition.continuous = false;
    recognition.onstart = () => setVoiceListening(true);
    recognition.onerror = () => {
      setVoiceListening(false);
      pushMessage("assistant", "system", "语音输入失败，请改用文字输入。", "error");
    };
    recognition.onend = () => setVoiceListening(false);
    recognition.onresult = (event: any) => {
      const text = Array.from(event.results)
        .map((result: any) => result[0]?.transcript ?? "")
        .join("")
        .trim();
      setAssistantDraft(text);
    };
    recognitionRef.current = recognition;
    recognition.start();
  }

  async function optimizeResumeInBackground(finalResumeText: string, nextAnalysis: JdAnalysisResponse) {
    setResumeOptimizeStatus("running");
    setResumeOptimizeError("");
    pushMessage("interview", "system", "后台同步生成 JD 适配简历。", "active");
    try {
      const optimized = await apiPost<ResumeOptimizeResponse>("/llm/resume/optimize", {
        resumeText: finalResumeText,
        jdText: jd,
        targetRole: nextAnalysis.role,
        assetHints: [
          ...nextAnalysis.projectRecommendations.slice(0, 3).map((item) => item.name),
          ...nextAnalysis.moduleRecommendations.slice(0, 2).map((item) => item.title),
        ],
        allowFallback: false,
      });
      setResumeOptimization(optimized);
      setResumeOptimizeStatus("done");
      pushMessage("interview", "result", "JD 适配版简历已生成，可在右侧打开。", "done");
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "JD 适配简历生成失败";
      setResumeOptimizeStatus("error");
      setResumeOptimizeError(message);
      pushMessage("interview", "system", `JD 适配简历生成失败：${message}`, "error");
    }
  }

  function nextMessageId(prefix: string) {
    messageCounterRef.current += 1;
    return `${prefix}-${Date.now()}-${messageCounterRef.current}`;
  }

  function setChannelMessages(channel: ChatChannel, updater: (current: ChatMessage[]) => ChatMessage[]) {
    if (channel === "assistant") {
      setAssistantMessages(updater);
      return;
    }
    setInterviewMessages(updater);
  }

  function pushMessage(channel: ChatChannel, role: ChatMessage["role"], text: string, status?: ChatMessage["status"]) {
    const message: ChatMessage = {
      id: nextMessageId(`${channel}-${role}`),
      role,
      text,
      status,
    };
    setChannelMessages(channel, (current) => [
      ...current.map((item) => item.status === "active" ? { ...item, status: "done" as const } : item),
      message,
    ]);
    return message.id;
  }

  function replaceMessage(channel: ChatChannel, id: string, patch: Partial<ChatMessage>) {
    setChannelMessages(channel, (current) => current.map((message) => message.id === id ? { ...message, ...patch } : message));
  }

  async function streamAssistantQuestion(question: InterviewTurnResponse) {
    const text = [
      question.question,
      question.intent ? `考察意图：${question.intent}` : "",
      question.scoreFocus.length ? `关注点：${question.scoreFocus.join(" / ")}` : "",
    ].filter(Boolean).join("\n\n");
    const id = pushMessage("interview", "assistant", "", "active");
    for (let index = 0; index < text.length; index += 4) {
      replaceMessage("interview", id, { text: text.slice(0, index + 4), status: "active" });
      await wait(18);
    }
    replaceMessage("interview", id, { text, status: "done" });
  }

  async function streamAssistantText(text: string) {
    const id = pushMessage("assistant", "assistant", "", "active");
    for (let index = 0; index < text.length; index += 5) {
      replaceMessage("assistant", id, { text: text.slice(0, index + 5), status: "active" });
      await wait(16);
    }
    replaceMessage("assistant", id, { text, status: "done" });
  }

  function appendCurrentAnswer() {
    if (!currentQuestion) {
      setError("当前没有可回答的问题。");
      return null;
    }
    if (!answerDraft.trim()) {
      setError("请先填写本轮回答。");
      return null;
    }
    const nextAnswer: InterviewAnswer = {
      question: currentQuestion.question,
      answer: answerDraft.trim(),
      intent: currentQuestion.intent,
      scoreFocus: currentQuestion.scoreFocus,
    };
    const nextHistory = [...history, nextAnswer];
    setHistory(nextHistory);
    return nextHistory;
  }

  async function copyGeneratedResume() {
    if (!dialogResult?.generatedResumeMarkdown) return;
    await navigator.clipboard.writeText(dialogResult.generatedResumeMarkdown);
    setCopyMessage("已复制");
    window.setTimeout(() => setCopyMessage(""), 1600);
  }

  async function saveResultToDraft() {
    if (!dialogResult?.generatedResumeMarkdown) return;
    setSaveMessage("正在扫描 Markdown 并写入草稿...");
    setError("");
    try {
      const task = await apiPost<ResumeUploadTask>("/admin/resume/uploads/parse", {
        filename: `jd-adapted-${Date.now()}.md`,
        contentType: "text/markdown",
        content: dialogResult.generatedResumeMarkdown,
      });
      await apiPost<ResumeDraftView>(`/admin/resume/uploads/${task.id}/confirm`);
      setSaveMessage("已保存到后台草稿简历");
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "保存到草稿失败";
      setSaveMessage("");
      setError(message);
    }
  }

  return (
    <section className="interview-kit-stage mx-auto grid max-w-[92rem] gap-5 px-5 pb-20 pt-10 md:px-8 xl:grid-cols-[minmax(18rem,0.78fr)_minmax(34rem,1.36fr)_minmax(15rem,0.58fr)]">
      <Surface className="interview-workbench-panel interview-setup-panel">
        <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Setup</p>
        <form onSubmit={startInterview} className="mt-5">
          <label className="block text-sm font-medium text-ink" htmlFor="jd">
            岗位 JD
          </label>
          <textarea
            id="jd"
            value={jd}
            onChange={(event) => setJd(event.target.value)}
            className="interview-field mt-3 min-h-36 w-full resize-none p-4 text-sm leading-7"
            placeholder="粘贴 Java 后端、AI 应用或全栈岗位 JD..."
          />

          <div className="mt-5 flex items-center justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-ink">简历来源</p>
              <p className="mt-1 text-xs text-graphite">{resumeSource}</p>
            </div>
            <div className="flex flex-wrap justify-end gap-2">
              <label className="secondary-action inline-flex cursor-pointer items-center gap-2 px-4 py-2 text-sm font-medium">
                <Upload size={15} />
                上传
                <input
                  type="file"
                  accept=".txt,.md,.markdown,.pdf,.doc,.docx,text/plain,text/markdown,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  className="sr-only"
                  onChange={readResumeFile}
                />
              </label>
              <button type="button" onClick={useDraftResume} className="secondary-action inline-flex items-center gap-2 px-4 py-2 text-sm font-medium">
                <FileText size={15} />
                草稿
              </button>
            </div>
          </div>

          <textarea
            value={resumeText}
            onChange={(event) => {
              setResumeText(event.target.value);
              setResumeSource(event.target.value.trim() ? "手动粘贴" : "未选择");
            }}
            className="interview-field mt-3 min-h-48 w-full resize-y p-4 text-sm leading-7"
            placeholder="上传 PDF、DOC、DOCX、TXT、Markdown 简历，或直接粘贴简历文本。"
          />
          <p className="mt-2 text-xs text-graphite">
            {resumeText.trim().length}/{maxResumeChars} 字符
          </p>

          <button disabled={loading || !jd.trim()} className="primary-action mt-5 w-full px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55">
            {loading && phase === "setup" ? <Loader2 className="animate-spin" size={16} /> : <ArrowRight size={16} />}
            开始模拟面试
          </button>
        </form>

        <div className="interview-finalize-dock">
          <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-accent">Result Action</p>
          <p className="mt-2 text-sm leading-6 text-graphite">完成至少一轮回答后，在这里生成面试反馈与 JD 适配简历。</p>
          <button
            type="button"
            onClick={finalizeInterview}
            disabled={loading || (!answerDraft.trim() && history.length === 0)}
            className="primary-action mt-4 w-full px-6 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55"
          >
            {phase === "generating" ? <Loader2 className="animate-spin" size={16} /> : <CheckCircle2 size={16} />}
            生成方案
          </button>
        </div>
        {error ? <p className="mt-4 border border-red-200 bg-red-50 px-4 py-3 text-sm leading-6 text-red-700">{error}</p> : null}
      </Surface>

      <Surface className="interview-workbench-panel interview-main-panel">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Conversation</p>
            <h2 className="display-light mt-4 text-4xl leading-none text-ink">
              {activeChat === "assistant" ? "语言助手" : analysis?.role ?? "模拟面试"}
            </h2>
          </div>
          <span className="metric-pill">{activeChat === "assistant" ? `${assistantMessages.length} 条消息` : `${history.length} 轮回答`}</span>
        </div>

        <div className="chat-mode-tabs mt-6" role="tablist" aria-label="对话模式">
          <button
            type="button"
            className={activeChat === "assistant" ? "is-active" : ""}
            onClick={() => setActiveChat("assistant")}
          >
            语言助手
            {assistantLoading ? <span>RUNNING</span> : null}
          </button>
          <button
            type="button"
            className={activeChat === "interview" ? "is-active" : ""}
            onClick={() => setActiveChat("interview")}
          >
            模拟面试
            {loading || phase === "generating" || resumeOptimizeStatus === "running" ? <span>RUNNING</span> : null}
          </button>
        </div>

        <div className="interview-dialog-shell mt-7">
          <div ref={dialogStreamRef} className="interview-dialog-stream">
            {activeMessages.map((message) => (
              <article key={message.id} className={`interview-message is-${message.role} ${message.status ? `is-${message.status}` : ""}`}>
                <span className="interview-message-speaker">
                  {message.role === "user" ? "YOU" : message.role === "assistant" ? "AI" : message.role === "result" ? "DONE" : "STATUS"}
                </span>
                <p>{message.text}</p>
                {message.status === "active" ? <span className="interview-message-cursor" aria-hidden="true" /> : null}
              </article>
            ))}
          </div>

          <div className="interview-composer">
            <textarea
              id="answer"
              value={activeChat === "interview" ? answerDraft : assistantDraft}
              onChange={(event) => activeChat === "interview" ? setAnswerDraft(event.target.value) : setAssistantDraft(event.target.value)}
              onKeyDown={(event) => {
                if (activeChat === "assistant" && (event.metaKey || event.ctrlKey) && event.key === "Enter") {
                  event.preventDefault();
                  void askLanguageAssistant();
                }
              }}
              disabled={activeChat === "interview" ? phase === "generating" || !currentQuestion : assistantLoading}
              className="interview-field min-h-28 w-full resize-none p-4 text-sm leading-7"
              placeholder={activeChat === "interview" ? "输入本轮回答..." : "和语言助手对话，后续可接入知识库。"}
            />
            <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
              <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-graphite">
                {activeChat === "interview"
                  ? phase === "generating" ? "GENERATING RESULT" : currentQuestion ? `QUESTION ${currentQuestion.round}` : "INTERVIEW READY"
                  : voiceListening ? "LISTENING" : "ASSISTANT"}
              </span>
              {activeChat === "interview" ? (
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={submitAnswerAndContinue}
                    disabled={loading || !currentQuestion || !answerDraft.trim() || phase === "generating"}
                    className="secondary-action px-5 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55"
                  >
                    {loading && phase === "interviewing" ? <Loader2 className="animate-spin" size={16} /> : <ArrowRight size={16} />}
                    下一题
                  </button>
                </div>
              ) : (
                <div className="flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={startVoiceInput}
                    disabled={assistantLoading}
                    className={`secondary-action px-5 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55 ${voiceListening ? "is-listening" : ""}`}
                  >
                    <Mic size={16} />
                    {voiceListening ? "停止" : "语音输入"}
                  </button>
                  <button
                    type="button"
                    onClick={askLanguageAssistant}
                    disabled={assistantLoading || !assistantDraft.trim()}
                    className="primary-action px-5 py-3 text-sm font-medium disabled:cursor-not-allowed disabled:opacity-55"
                  >
                    {assistantLoading ? <Loader2 className="animate-spin" size={16} /> : <Send size={16} />}
                    发送
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </Surface>

      <Surface className="interview-workbench-panel interview-result-panel">
        <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">Result Dock</p>
        <h2 className="mt-5 text-2xl font-semibold text-ink">结果</h2>
        <div className="interview-status-strip mt-5">
          <span>STATUS</span>
          <strong>{dialogResult ? "READY" : phase === "generating" || resumeOptimizeStatus === "running" ? "RUNNING" : "STANDBY"}</strong>
          <span>FORMAT</span>
          <strong>MD</strong>
        </div>
        <div className="mt-6 border-t border-line pt-5">
          <p className="text-sm font-medium text-ink">JD 适配简历</p>
          <p className="mt-2 text-sm leading-6 text-graphite">
            {resumeOptimizeStatus === "done"
              ? "后台适配版已生成。"
              : resumeOptimizeStatus === "running"
                ? "后台生成中。"
                : resumeOptimizeStatus === "error"
                  ? "生成失败。"
                  : "启动面试后同步生成。"}
          </p>
          {resumeOptimizeError ? <p className="mt-2 text-xs leading-5 text-red-600">{resumeOptimizeError}</p> : null}
          {dialogResult ? (
            <button
              type="button"
              className="secondary-action mt-4 px-4 py-2 text-sm font-medium"
              onClick={() => {
                setResultTab("resume");
                setResultOpen(true);
              }}
            >
              <FileText size={15} />
              查看适配简历
            </button>
          ) : null}
        </div>
        {assetLoading ? <p className="mt-5 text-sm text-graphite">正在加载经历资产...</p> : null}
        {assetError ? <p className="mt-5 text-sm leading-6 text-red-600">{assetError}</p> : null}
        {analysis ? (
          <div className="mt-6 border-t border-line pt-5">
            <p className="text-sm font-medium text-ink">{analysis.role}</p>
            <p className="mt-2 text-sm leading-6 text-graphite">匹配度 {analysis.matchScore} / 100</p>
            <div className="mt-4 flex flex-wrap gap-2">
              {analysis.keywords.slice(0, 6).map((keyword) => (
                <span key={keyword} className="chapter-tag">{keyword}</span>
              ))}
            </div>
          </div>
        ) : null}
        {orderedProjects.length > 0 ? (
          <div className="mt-6 border-t border-line pt-5">
            <p className="text-sm font-medium text-ink">优先讲解项目</p>
            <ol className="mt-3 space-y-2 text-sm leading-6 text-graphite">
              {orderedProjects.slice(0, 3).map((project) => (
                <li key={project.slug}>{project.name}</li>
              ))}
            </ol>
          </div>
        ) : null}
      </Surface>

      {dialogResult ? (
        <button type="button" className="interview-result-toast" onClick={() => setResultOpen(true)}>
          <span className="font-mono text-[10px] uppercase tracking-[0.2em] text-accent">{result ? "Result Ready" : "Resume Ready"}</span>
          <strong>{dialogResult.role}</strong>
          <small>{result ? `${dialogResult.questionCount} 轮问答 · 点击查看详情` : "JD 适配简历 · 点击查看详情"}</small>
        </button>
      ) : null}

      {resultOpen && dialogResult ? (
        <ResultDialog
          result={dialogResult}
          tab={resultTab}
          onTabChange={setResultTab}
          onClose={() => setResultOpen(false)}
          onCopy={copyGeneratedResume}
          copyMessage={copyMessage}
          onSave={saveResultToDraft}
          saveMessage={saveMessage}
        />
      ) : null}
    </section>
  );
}

function resultFromResumeOptimization(
  optimization: ResumeOptimizeResponse | null,
  questionCount: number,
): InterviewFinalizeResponse | null {
  if (!optimization) return null;
  return {
    provider: optimization.provider,
    role: optimization.role,
    summary: optimization.summary,
    generatedResumeMarkdown: optimization.generatedResumeMarkdown,
    highlights: optimization.highlights,
    resumeSuggestions: optimization.sectionSuggestions,
    interviewFeedback: ["模拟面试仍在进行中，完成后会生成更完整的问答反馈。"],
    nextActions: ["继续完成模拟面试，使用最终结果包复核这版适配简历。"],
    riskNotes: optimization.riskNotes,
    questionCount,
  };
}

function ResultDialog({
  result,
  tab,
  onTabChange,
  onClose,
  onCopy,
  copyMessage,
  onSave,
  saveMessage,
}: {
  result: InterviewFinalizeResponse;
  tab: ResultTab;
  onTabChange: (tab: ResultTab) => void;
  onClose: () => void;
  onCopy: () => void;
  copyMessage: string;
  onSave: () => void;
  saveMessage: string;
}) {
  return (
    <div className="interview-result-dialog" role="dialog" aria-modal="true" aria-label="面试结果详情">
      <div className="interview-result-card">
        <div className="flex flex-wrap items-start justify-between gap-4 border-b border-line pb-5">
          <div>
            <p className="font-mono text-xs uppercase tracking-[0.2em] text-accent">{result.provider}</p>
            <h2 className="display-light mt-3 text-4xl leading-none text-ink">{result.role}</h2>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-graphite">{result.summary}</p>
          </div>
          <button type="button" className="llm-progress-close" onClick={onClose} aria-label="关闭结果详情">
            <X size={16} />
          </button>
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          {(["summary", "resume", "feedback"] as ResultTab[]).map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => onTabChange(item)}
              className={`admin-tab ${tab === item ? "is-active" : ""}`}
            >
              {item === "summary" ? "摘要" : item === "resume" ? "适配简历" : "面试反馈"}
            </button>
          ))}
        </div>

        <div className="mt-5 max-h-[62vh] overflow-auto border border-line bg-stonepaper p-5">
          {tab === "summary" ? (
            <ResultList
              groups={[
                ["亮点", result.highlights],
                ["简历建议", result.resumeSuggestions],
                ["下一步", result.nextActions],
                ["风险", result.riskNotes],
              ]}
            />
          ) : null}
          {tab === "resume" ? (
            <>
              <div className="mb-4 flex flex-wrap gap-2">
                <button type="button" onClick={onCopy} className="secondary-action px-4 py-2 text-sm font-medium">
                  <Clipboard size={15} />
                  {copyMessage || "复制 Markdown"}
                </button>
                <button type="button" onClick={onSave} className="primary-action px-4 py-2 text-sm font-medium">
                  <Save size={15} />
                  保存到草稿
                </button>
              </div>
              {saveMessage ? <p className="mb-4 text-sm leading-6 text-accent">{saveMessage}</p> : null}
              <div className="bg-white p-5">
                <MarkdownRenderer content={result.generatedResumeMarkdown} />
              </div>
            </>
          ) : null}
          {tab === "feedback" ? (
            <ResultList groups={[["面试反馈", result.interviewFeedback], ["下一步", result.nextActions], ["风险", result.riskNotes]]} />
          ) : null}
        </div>
      </div>
    </div>
  );
}

function ResultList({ groups }: { groups: Array<[string, string[]]> }) {
  return (
    <div className="grid gap-5">
      {groups.filter(([, items]) => items.length > 0).map(([title, items]) => (
        <section key={title} className="bg-white p-4">
          <h3 className="text-sm font-semibold text-ink">{title}</h3>
          <ul className="mt-3 space-y-2 text-sm leading-7 text-graphite">
            {items.map((item) => (
              <li key={item}>- {item}</li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

function readInterviewKitMemory(): StoredInterviewKitMemory | null {
  try {
    const raw = window.localStorage.getItem(memoryStorageKey);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<StoredInterviewKitMemory>;
    return {
      jd: typeof parsed.jd === "string" ? parsed.jd : defaultJd,
      resumeText: typeof parsed.resumeText === "string" ? parsed.resumeText : "",
      resumeSource: typeof parsed.resumeSource === "string" ? parsed.resumeSource : "未选择",
      analysis: parsed.analysis ?? null,
      phase: isInterviewPhase(parsed.phase) ? parsed.phase : "setup",
      currentQuestion: parsed.currentQuestion ?? null,
      history: Array.isArray(parsed.history) ? parsed.history : [],
      result: parsed.result ?? null,
      resumeOptimization: parsed.resumeOptimization ?? null,
      resumeOptimizeStatus: isResumeOptimizeStatus(parsed.resumeOptimizeStatus) ? parsed.resumeOptimizeStatus : "idle",
      activeChat: parsed.activeChat === "interview" ? "interview" : "assistant",
      assistantMessages: normalizeMessagesForStorage(Array.isArray(parsed.assistantMessages) ? parsed.assistantMessages : []),
      interviewMessages: normalizeMessagesForStorage(Array.isArray(parsed.interviewMessages) ? parsed.interviewMessages : []),
    };
  } catch {
    return null;
  }
}

function normalizeMessagesForStorage(messages: ChatMessage[]) {
  return messages
    .filter((message) => typeof message.text === "string")
    .slice(-40)
    .map((message) => ({
      ...message,
      status: message.status === "active" ? "done" as const : message.status,
      text: message.text.slice(0, 1600),
    }));
}

function buildAssistantMemory({
  assistantMessages,
  interviewMessages,
  history,
  analysis,
  jd,
  resumeSource,
}: {
  assistantMessages: ChatMessage[];
  interviewMessages: ChatMessage[];
  history: InterviewAnswer[];
  analysis: JdAnalysisResponse | null;
  jd: string;
  resumeSource: string;
}) {
  const assistantTurns = assistantMessages
    .filter((message) => message.role === "user" || message.role === "assistant")
    .slice(-8)
    .map((message) => `${message.role === "user" ? "用户" : "助手"}：${message.text}`)
    .join("\n");
  const interviewTurns = history.slice(-4)
    .map((item, index) => `面试${index + 1}：问：${item.question} 答：${item.answer}`)
    .join("\n");
  const recentInterviewStatus = interviewMessages
    .filter((message) => message.role === "system" || message.role === "result")
    .slice(-4)
    .map((message) => message.text)
    .join("；");
  return [
    `当前 JD：${jd.slice(0, 500)}`,
    `简历来源：${resumeSource}`,
    analysis ? `岗位分析：${analysis.role}，匹配度 ${analysis.matchScore}/100，关键词 ${analysis.keywords.slice(0, 8).join("、")}` : "",
    assistantTurns ? `语言助手最近对话：\n${assistantTurns}` : "",
    interviewTurns ? `模拟面试问答记忆：\n${interviewTurns}` : "",
    recentInterviewStatus ? `面试流程状态：${recentInterviewStatus}` : "",
  ].filter(Boolean).join("\n\n").slice(0, 2600);
}

function isInterviewPhase(value: unknown): value is InterviewPhase {
  return value === "setup" || value === "interviewing" || value === "generating" || value === "done";
}

function isResumeOptimizeStatus(value: unknown): value is ResumeOptimizeStatus {
  return value === "idle" || value === "running" || value === "done" || value === "error";
}

function resumeTextFromDraft(draft: ResumeDraftView) {
  const basic = draft.basicInfo;
  const sectionText = Object.entries(draft.sections)
    .map(([type, items]) => {
      const content = items
        .map((item) => [
          item.title,
          item.subtitle,
          item.period,
          item.summary,
          item.detail,
          item.tags.join("、"),
        ].filter(Boolean).join("\n"))
        .join("\n\n");
      return `${type}\n${content}`;
    })
    .join("\n\n");
  return [
    basic.name,
    basic.title,
    basic.summary,
    basic.email,
    basic.education,
    sectionText,
  ].filter(Boolean).join("\n");
}

async function resumeTextFromFile(file: File) {
  if (isTextResumeFile(file)) {
    return file.text();
  }
  const extracted = await apiPost<ResumeExtractedText>("/admin/resume/uploads/extract-text", {
    filename: file.name,
    contentType: file.type || "application/octet-stream",
    contentBase64: await fileToBase64(file),
  });
  return extracted.rawText;
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function nextPaint() {
  return new Promise((resolve) => window.requestAnimationFrame(resolve));
}

function cleanAssistantAnswer(answer: string) {
  return answer
    .replace(/这是一次基于\s*rag-question\s*工作流模板的受约束回答[，,。；;]*/gi, "")
    .replace(/仅能基于常见技术知识进行解释[，,。；;]*/g, "")
    .replace(/不能声称已经执行真实数据库查询、真实\s*RAG\s*检索或真实外部工具[，,。；;]*/gi, "")
    .replace(/目前缺少真实的\s*RAG\s*检索工具结果[，,。；;]*/gi, "")
    .replace(/未接入真实\s*RAG\s*检索器[，,。；;]*/gi, "")
    .replace(/工作流模板[:：]?\s*rag-question/gi, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function isTextResumeFile(file: File) {
  const name = file.name.toLowerCase();
  return file.type.startsWith("text/")
    || name.endsWith(".txt")
    || name.endsWith(".md")
    || name.endsWith(".markdown");
}

async function fileToBase64(file: File) {
  const buffer = await file.arrayBuffer();
  const bytes = new Uint8Array(buffer);
  let binary = "";
  const chunkSize = 0x8000;
  for (let index = 0; index < bytes.length; index += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(index, index + chunkSize));
  }
  return window.btoa(binary);
}
