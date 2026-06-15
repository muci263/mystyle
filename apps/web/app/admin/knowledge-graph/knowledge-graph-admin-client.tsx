"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { Loader2, Network, Plus, RefreshCw, Save, Trash2 } from "lucide-react";
import { LlmProgressPanel, useLlmProgress } from "@/components/llm-progress";
import { apiDelete, apiGet, apiPost, apiPut } from "@/lib/api";
import type {
  KnowledgeGraphEdge,
  KnowledgeGraphEdgePayload,
  KnowledgeGraphAutoRelateResponse,
  KnowledgeGraphOrchestrateResponse,
  KnowledgeGraphNode,
  KnowledgeGraphNodePayload,
  KnowledgeGraphSmartNodePayload,
} from "@/lib/api";

const nodeTypes = ["CORE", "SECTION", "SKILL", "PROJECT", "MODULE", "BLOG"];
const relationTypes = ["OWNS", "INCLUDES", "CONTAINS", "USES", "EXPLAINS", "RELATED"];
const levelOptions = [
  { value: "0", label: "一级 / 个人核心" },
  { value: "1", label: "二级 / 栏目入口" },
  { value: "2", label: "三级 / 内容节点" },
];

const nodeTypeLevels: Record<string, string> = {
  CORE: "0",
  SECTION: "1",
  SKILL: "2",
  PROJECT: "2",
  MODULE: "2",
  BLOG: "2",
};

const graphLlmSteps = [
  { id: "contract", label: "输入输出契约", detail: "整理当前节点、层级、关系类型和能力边界。" },
  { id: "request", label: "模型推理", detail: "调用 Minimax 生成候选关系。" },
  { id: "guard", label: "后端校验", detail: "过滤跨级连接、重复边和无证据关系。" },
  { id: "refresh", label: "刷新图谱", detail: "拉取最新节点与隐藏候选边。" },
];

type NodeForm = {
  nodeKey: string;
  label: string;
  nodeType: string;
  level: string;
  summary: string;
  content: string;
  tags: string;
  href: string;
  sourceType: string;
  sourceSlug: string;
  x: string;
  y: string;
  z: string;
  visible: boolean;
  sortOrder: string;
};

type EdgeForm = {
  fromNodeKey: string;
  toNodeKey: string;
  relationType: string;
  visible: boolean;
  sortOrder: string;
};

const emptyNodeForm: NodeForm = {
  nodeKey: "",
  label: "",
  nodeType: "BLOG",
  level: "2",
  summary: "",
  content: "",
  tags: "",
  href: "",
  sourceType: "MANUAL",
  sourceSlug: "",
  x: "0",
  y: "0",
  z: "0",
  visible: true,
  sortOrder: "500",
};

const emptyEdgeForm: EdgeForm = {
  fromNodeKey: "me",
  toNodeKey: "",
  relationType: "RELATED",
  visible: false,
  sortOrder: "500",
};

export function KnowledgeGraphAdminClient({
  initialNodes,
  initialEdges,
  initialNodeId,
}: {
  initialNodes: KnowledgeGraphNode[];
  initialEdges: KnowledgeGraphEdge[];
  initialNodeId?: string;
}) {
  const [nodes, setNodes] = useState(initialNodes);
  const [edges, setEdges] = useState(initialEdges);
  const [nodeForm, setNodeForm] = useState<NodeForm>({ ...emptyNodeForm });
  const [edgeForm, setEdgeForm] = useState<EdgeForm>({ ...emptyEdgeForm });
  const [editingNodeKey, setEditingNodeKey] = useState<string | null>(null);
  const [editingEdgeId, setEditingEdgeId] = useState<number | null>(null);
  const [mode, setMode] = useState<"nodes" | "edges">("nodes");
  const [activeNodeType, setActiveNodeType] = useState("ALL");
  const [busy, setBusy] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const llmProgress = useLlmProgress();
  const nodeFormRef = useRef<HTMLFormElement>(null);
  const edgeFormRef = useRef<HTMLFormElement>(null);
  const appliedInitialNodeRef = useRef(false);
  const nodeFallbackSubmitRef = useRef(false);

  const levelCounts = useMemo(() => ({
    core: nodes.filter((node) => node.level === 0).length,
    section: nodes.filter((node) => node.level === 1).length,
    content: nodes.filter((node) => node.level === 2).length,
  }), [nodes]);
  const nodeNameByKey = useMemo(() => new Map(nodes.map((node) => [node.nodeKey, node.label])), [nodes]);
  const nodeByKey = useMemo(() => new Map(nodes.map((node) => [node.nodeKey, node])), [nodes]);
  const edgeLevelWarning = useMemo(() => edgeGuardMessage(edgeForm, nodeByKey), [edgeForm, nodeByKey]);
  const filteredNodes = useMemo(
    () => activeNodeType === "ALL" ? nodes : nodes.filter((node) => node.nodeType === activeNodeType),
    [activeNodeType, nodes],
  );
  const groupedNodes = useMemo(
    () => nodeTypes
        .map((type) => ({ type, nodes: filteredNodes.filter((node) => node.nodeType === type) }))
        .filter((group) => group.nodes.length > 0),
    [filteredNodes],
  );

  useEffect(() => {
    if (!initialNodeId || appliedInitialNodeRef.current || editingNodeKey === initialNodeId) return;
    const node = nodes.find((item) => item.nodeKey === initialNodeId);
    if (!node) return;
    appliedInitialNodeRef.current = true;
    startNodeEditing(node);
    setNotice(`已定位节点：${node.label}`);
  }, [initialNodeId, nodes, editingNodeKey]);

  async function refreshGraph() {
    const [nextNodes, nextEdges] = await Promise.all([
      apiGet<KnowledgeGraphNode[]>("/admin/knowledge-graph/nodes"),
      apiGet<KnowledgeGraphEdge[]>("/admin/knowledge-graph/edges"),
    ]);
    setNodes(nextNodes);
    setEdges(nextEdges);
  }

  async function saveNode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const allowFallback = nodeFallbackSubmitRef.current;
    nodeFallbackSubmitRef.current = false;
    await run("node", async () => {
      const payload = nodePayload(nodeForm);
      if (editingNodeKey) {
        await apiPut<KnowledgeGraphNode>(`/admin/knowledge-graph/nodes/${editingNodeKey}`, payload);
        setNotice("节点已更新");
      } else {
        llmProgress.start("智能新增节点", graphLlmSteps);
        llmProgress.setStep("contract", "done", "已约定 nodeKey、nodeType、level、候选关系 JSON schema。");
        llmProgress.activate("request", allowFallback ? "用户已确认使用规则降级，本次不调用 Minimax。" : "Minimax 正在生成不跨级的候选关系。");
        const response = await apiPost<KnowledgeGraphAutoRelateResponse>(
          `/admin/knowledge-graph/nodes/smart-create${allowFallback ? "?allowFallback=true" : ""}`,
          smartNodePayload(nodeForm),
        );
        llmProgress.activate("guard", "后端正在执行层级、方向、重复边和可见性规则。");
        llmProgress.setStep("guard", "done", response.notes.slice(0, 2).join(" "));
        llmProgress.activate("refresh", "正在同步最新图谱数据。");
        setNotice(`节点已新增，${response.provider} 自动创建 ${response.createdEdges.length} 条关系`);
        llmProgress.complete(`完成：新增节点并创建 ${response.createdEdges.length} 条隐藏候选关系。`);
      }
      await refreshGraph();
      setNodeForm({ ...emptyNodeForm });
      setEditingNodeKey(null);
    }, llmProgress.fail);
  }

  async function saveEdge(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await run("edge", async () => {
      const payload = edgePayload(edgeForm);
      if (editingEdgeId) {
        await apiPut<KnowledgeGraphEdge>(`/admin/knowledge-graph/edges/${editingEdgeId}`, payload);
      } else {
        await apiPost<KnowledgeGraphEdge>("/admin/knowledge-graph/edges", payload);
      }
      await refreshGraph();
      setEdgeForm({ ...emptyEdgeForm });
      setEditingEdgeId(null);
      setNotice(editingEdgeId ? "关系已更新" : "关系已新增");
    });
  }

  async function deleteNode(nodeKey: string) {
    await run(`delete-node-${nodeKey}`, async () => {
      await apiDelete<string>(`/admin/knowledge-graph/nodes/${nodeKey}`);
      await refreshGraph();
      if (editingNodeKey === nodeKey) cancelNodeEditing();
      setNotice("节点已删除，相关关系已同步移除");
    });
  }

  async function deleteEdge(edgeId: number) {
    await run(`delete-edge-${edgeId}`, async () => {
      await apiDelete<string>(`/admin/knowledge-graph/edges/${edgeId}`);
      await refreshGraph();
      if (editingEdgeId === edgeId) cancelEdgeEditing();
      setNotice("关系已删除");
    });
  }

  async function submitNodeWithFallback() {
    nodeFallbackSubmitRef.current = true;
    nodeFormRef.current?.requestSubmit();
  }

  async function autoRelateNode(nodeKey: string, allowFallback = false) {
    await run(`auto-relate-${nodeKey}`, async () => {
      const node = nodes.find((item) => item.nodeKey === nodeKey);
      llmProgress.start(`${allowFallback ? "规则兜底建边" : "AI 建边"} / ${node?.label ?? nodeKey}`, graphLlmSteps);
      llmProgress.setStep("contract", "done", "已锁定当前节点，只允许生成包含该节点的关系。");
      llmProgress.activate("request", allowFallback ? "用户已确认使用规则降级，本次不调用 Minimax。" : "Minimax 正在按层级契约生成候选边。");
      const response = await apiPost<KnowledgeGraphAutoRelateResponse>(
        `/admin/knowledge-graph/nodes/${nodeKey}/auto-relate${allowFallback ? "?allowFallback=true" : ""}`,
      );
      llmProgress.activate("guard", "正在过滤跨级连接和不合理证据边。");
      llmProgress.setStep("guard", "done", response.notes.slice(0, 2).join(" "));
      llmProgress.activate("refresh", "正在刷新节点列表和隐藏候选边。");
      await refreshGraph();
      setNotice(`${response.node.label} 已由 ${response.provider} 生成 ${response.createdEdges.length} 条隐藏候选关系`);
      llmProgress.complete(`完成：${response.suggestions.length} 条建议，新增 ${response.createdEdges.length} 条隐藏边。`);
    }, llmProgress.fail);
  }

  async function orchestrateGraph(allowFallback = false) {
    await run(allowFallback ? "orchestrate-fallback" : "orchestrate", async () => {
      llmProgress.start(allowFallback ? "全图规则兜底候选边" : "全图 AI 编排候选边", [
        { id: "contract", label: "编排契约", detail: "只处理三级节点与二级栏目之间的归属边。" },
        { id: "request", label: "并行推理", detail: "并发调用 Minimax，判断三级节点应归属哪些二级栏目。" },
        { id: "guard", label: "批量过滤", detail: "拒绝一级固定边、跨级边、重复边和三级证据边。" },
        { id: "refresh", label: "刷新 CMS", detail: "同步新增的隐藏候选边。" },
      ]);
      llmProgress.setStep("contract", "done", "一级到二级不参与 LLM；每个三级节点最多新增 2 条二级归属边。");
      llmProgress.activate("request", allowFallback ? "用户已确认使用规则降级，本次不调用 Minimax。" : "Minimax 正在并行处理缺少二级归属的三级节点。");
      const response = await apiPost<KnowledgeGraphOrchestrateResponse>(`/admin/knowledge-graph/orchestrate${allowFallback ? "?allowFallback=true" : ""}`);
      llmProgress.activate("guard", "后端正在汇总过滤结果。");
      llmProgress.setStep("guard", "done", response.notes.join(" "));
      llmProgress.activate("refresh", "正在刷新 CMS 数据。");
      await refreshGraph();
      setNotice(`${response.provider} 已按能力边界扫描 ${response.scannedNodes} 个节点，新增 ${response.createdEdges} 条隐藏候选关系`);
      llmProgress.complete(`完成：扫描 ${response.scannedNodes} 个节点，新增 ${response.createdEdges} 条隐藏候选边。`);
    }, llmProgress.fail);
  }

  function startNodeEditing(node: KnowledgeGraphNode) {
    setMode("nodes");
    setEditingNodeKey(node.nodeKey);
    setNodeForm({
      nodeKey: node.nodeKey,
      label: node.label,
      nodeType: node.nodeType,
      level: String(node.level),
      summary: node.summary,
      content: node.content,
      tags: node.tags.join(", "),
      href: node.href,
      sourceType: node.sourceType,
      sourceSlug: node.sourceSlug,
      x: String(node.x),
      y: String(node.y),
      z: String(node.z),
      visible: node.visible,
      sortOrder: String(node.sortOrder),
    });
    scrollToNodeForm();
  }

  function startEdgeEditing(edge: KnowledgeGraphEdge) {
    setMode("edges");
    setEditingEdgeId(edge.id);
    setEdgeForm({
      fromNodeKey: edge.fromNodeKey,
      toNodeKey: edge.toNodeKey,
      relationType: edge.relationType,
      visible: edge.visible,
      sortOrder: String(edge.sortOrder),
    });
    scrollToEdgeForm();
  }

  function cancelNodeEditing() {
    setEditingNodeKey(null);
    setNodeForm({ ...emptyNodeForm });
  }

  function cancelEdgeEditing() {
    setEditingEdgeId(null);
    setEdgeForm({ ...emptyEdgeForm });
  }

  function startNewNode() {
    setMode("nodes");
    setEditingNodeKey(null);
    setEditingEdgeId(null);
    setNodeForm({ ...emptyNodeForm });
    setNotice("已进入新增节点模式");
    scrollToNodeForm();
  }

  function startNewEdge() {
    setMode("edges");
    setEditingNodeKey(null);
    setEditingEdgeId(null);
    setEdgeForm({ ...emptyEdgeForm });
    setNotice("已进入新增关系模式");
    scrollToEdgeForm();
  }

  function scrollToNodeForm() {
    window.requestAnimationFrame(() => nodeFormRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }));
  }

  function scrollToEdgeForm() {
    window.requestAnimationFrame(() => edgeFormRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }));
  }

  async function run(action: string, task: () => Promise<void>, onError?: (message: string) => void) {
    setBusy(action);
    setError("");
    setNotice("");
    try {
      await task();
    } catch (exception) {
      const message = exception instanceof Error ? exception.message : "操作失败";
      setError(message);
      onError?.(message);
    } finally {
      setBusy("");
    }
  }

  return (
    <section className="graph-admin-page">
      <div className="mx-auto max-w-7xl px-5 py-10 md:px-8 md:py-14">
        <div className="graph-admin-hero">
          <div>
            <p className="eyebrow">Knowledge Graph CMS</p>
            <h1 className="display mt-6 text-5xl leading-[1.02] md:text-7xl">首页图谱管理</h1>
          </div>
          <div className="admin-status-grid">
            <StatusTile label="一级个人" value={`${levelCounts.core}`} />
            <StatusTile label="二级栏目" value={`${levelCounts.section}`} />
            <StatusTile label="三级内容" value={`${levelCounts.content}`} />
          </div>
        </div>

        {(notice || error) ? (
          <div className={`admin-message ${error ? "is-error" : ""}`}>{error || notice}</div>
        ) : null}

        <LlmProgressPanel state={llmProgress.progress} onDismiss={llmProgress.reset} />

        <div className="mt-8 flex flex-wrap items-center justify-between gap-3">
          <div className="flex gap-2">
            <button type="button" className={`admin-tab ${mode === "nodes" ? "is-active" : ""}`} onClick={() => setMode("nodes")}>
              节点 CRUD
            </button>
            <button type="button" className={`admin-tab ${mode === "edges" ? "is-active" : ""}`} onClick={() => setMode("edges")}>
              关系 CRUD
            </button>
          </div>
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={startNewNode} className="primary-action px-4 py-2 text-sm">
              <Plus size={16} />
              新增节点
            </button>
            <button type="button" onClick={startNewEdge} className="secondary-action px-4 py-2 text-sm">
              <Plus size={16} />
              新增关系
            </button>
            <button
              type="button"
              onClick={() => run("refresh", refreshGraph)}
              className="secondary-action px-4 py-2 text-sm"
              disabled={busy === "refresh"}
            >
              {busy === "refresh" ? <Loader2 className="animate-spin" size={16} /> : <RefreshCw size={16} />}
              刷新数据
            </button>
            <button
              type="button"
              onClick={() => orchestrateGraph(false)}
              className="secondary-action px-4 py-2 text-sm"
              disabled={busy === "orchestrate"}
            >
              {busy === "orchestrate" ? <Loader2 className="animate-spin" size={16} /> : <Network size={16} />}
              AI 编排候选边
            </button>
            <button
              type="button"
              onClick={() => orchestrateGraph(true)}
              className="secondary-action px-4 py-2 text-sm"
              disabled={busy === "orchestrate-fallback"}
            >
              {busy === "orchestrate-fallback" ? <Loader2 className="animate-spin" size={16} /> : <Network size={16} />}
              规则兜底编排
            </button>
          </div>
        </div>

        {mode === "nodes" ? (
          <div className="mt-6 grid gap-5 xl:grid-cols-[0.9fr_1.1fr]">
            <form ref={nodeFormRef} onSubmit={saveNode} className="admin-panel graph-admin-form">
              <FormHeader
                title={editingNodeKey ? "编辑节点" : "新增节点"}
                busy={busy === "node"}
                onCancel={editingNodeKey ? cancelNodeEditing : undefined}
                onFallback={!editingNodeKey ? submitNodeWithFallback : undefined}
              />
              <div className="mt-6 grid gap-4 md:grid-cols-2">
                <TextField label="标题" value={nodeForm.label} onChange={(value) => setNodeForm({ ...nodeForm, label: value })} required />
                <SelectField label="类型" value={nodeForm.nodeType} options={nodeTypes} onChange={(value) => setNodeForm({ ...nodeForm, nodeType: value, level: nodeTypeLevels[value] ?? "2" })} />
                <LevelSelect label="分级" value={nodeForm.level} nodeType={nodeForm.nodeType} onChange={(value) => setNodeForm({ ...nodeForm, level: value })} />
                <TextField label="标签，逗号分隔" value={nodeForm.tags} onChange={(value) => setNodeForm({ ...nodeForm, tags: value })} />
                <label className="admin-check md:mt-6">
                  <input type="checkbox" checked={nodeForm.visible} onChange={(event) => setNodeForm({ ...nodeForm, visible: event.target.checked })} />
                  前台展示
                </label>
              </div>
              <TextAreaField label="简短说明" value={nodeForm.summary} onChange={(value) => setNodeForm({ ...nodeForm, summary: value })} className="mt-4" />
              <details className="graph-admin-advanced mt-4">
                <summary>高级字段 / 手动精修</summary>
                <div className="mt-4 grid gap-4 md:grid-cols-2">
                  <TextField label="节点 Key" value={nodeForm.nodeKey} onChange={(value) => setNodeForm({ ...nodeForm, nodeKey: value })} required={Boolean(editingNodeKey)} />
                  <TextField label="路径 href" value={nodeForm.href} onChange={(value) => setNodeForm({ ...nodeForm, href: value })} />
                  <TextField label="来源类型" value={nodeForm.sourceType} onChange={(value) => setNodeForm({ ...nodeForm, sourceType: value })} />
                  <TextField label="来源 Slug" value={nodeForm.sourceSlug} onChange={(value) => setNodeForm({ ...nodeForm, sourceSlug: value })} />
                  <TextField label="排序" value={nodeForm.sortOrder} onChange={(value) => setNodeForm({ ...nodeForm, sortOrder: value })} type="number" />
                  <TextField label="X 坐标" value={nodeForm.x} onChange={(value) => setNodeForm({ ...nodeForm, x: value })} type="number" step="0.01" />
                  <TextField label="Y 坐标" value={nodeForm.y} onChange={(value) => setNodeForm({ ...nodeForm, y: value })} type="number" step="0.01" />
                  <TextField label="Z 坐标" value={nodeForm.z} onChange={(value) => setNodeForm({ ...nodeForm, z: value })} type="number" step="0.01" />
                </div>
                <TextAreaField label="节点介绍内容" value={nodeForm.content} onChange={(value) => setNodeForm({ ...nodeForm, content: value })} className="mt-4" />
              </details>
            </form>

            <div className="admin-panel">
              <ListHeader icon={<Network size={18} />} title="节点列表" count={nodes.length} />
              <div className="mt-5 flex flex-wrap gap-2">
                {["ALL", ...nodeTypes].map((type) => (
                  <button
                    key={type}
                    type="button"
                    className={`graph-type-filter ${activeNodeType === type ? "is-active" : ""}`}
                    onClick={() => setActiveNodeType(type)}
                  >
                    {type}
                  </button>
                ))}
              </div>
              <div className="mt-5 grid gap-5">
                {groupedNodes.map((group) => (
                  <section key={group.type} className="graph-node-group">
                    <div className="mb-3 flex items-center justify-between">
                      <h3 className="font-mono text-[11px] uppercase tracking-[0.22em] text-accent">{group.type}</h3>
                      <span className="graph-admin-count">{group.nodes.length}</span>
                    </div>
                    <div className="grid gap-3">
                      {group.nodes.map((node) => (
                        <article key={node.nodeKey} className={`graph-admin-row ${editingNodeKey === node.nodeKey ? "is-editing" : ""}`}>
                          <div>
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="graph-node-type">{node.nodeType}</span>
                              <h3 className="text-lg font-semibold">{node.label}</h3>
                              {!node.visible ? <span className="graph-hidden-badge">Hidden</span> : null}
                            </div>
                            <p className="mt-2 text-xs leading-5 text-graphite">{node.nodeKey} / {levelLabel(node.level)}</p>
                          </div>
                          <div className="flex shrink-0 gap-2">
                            <button type="button" className="admin-link-button" onClick={() => autoRelateNode(node.nodeKey, false)} disabled={busy === `auto-relate-${node.nodeKey}`}>
                              {busy === `auto-relate-${node.nodeKey}` ? "生成中" : "AI 建边"}
                            </button>
                            <button type="button" className="admin-link-button" onClick={() => autoRelateNode(node.nodeKey, true)} disabled={busy === `auto-relate-${node.nodeKey}`}>
                              规则建边
                            </button>
                            <button type="button" className="admin-link-button" onClick={() => startNodeEditing(node)}>编辑</button>
                            <button type="button" className="admin-danger-button" onClick={() => deleteNode(node.nodeKey)} disabled={busy === `delete-node-${node.nodeKey}`}>
                              {busy === `delete-node-${node.nodeKey}` ? <Loader2 className="animate-spin" size={15} /> : <Trash2 size={15} />}
                            </button>
                          </div>
                        </article>
                      ))}
                    </div>
                  </section>
                ))}
              </div>
            </div>
          </div>
        ) : (
          <div className="mt-6 grid gap-5 xl:grid-cols-[0.84fr_1.16fr]">
            <form ref={edgeFormRef} onSubmit={saveEdge} className="admin-panel graph-admin-form">
              <FormHeader title={editingEdgeId ? "编辑关系" : "新增关系"} busy={busy === "edge"} onCancel={editingEdgeId ? cancelEdgeEditing : undefined} />
              <div className="mt-6 grid gap-4 md:grid-cols-2">
                <SelectField label="起点节点" value={edgeForm.fromNodeKey} options={nodes.map((node) => node.nodeKey)} onChange={(value) => setEdgeForm({ ...edgeForm, fromNodeKey: value })} />
                <SelectField label="终点节点" value={edgeForm.toNodeKey} options={["", ...nodes.map((node) => node.nodeKey)]} onChange={(value) => setEdgeForm({ ...edgeForm, toNodeKey: value })} />
                <SelectField label="关系类型" value={edgeForm.relationType} options={relationTypes} onChange={(value) => setEdgeForm({ ...edgeForm, relationType: value })} />
                <TextField label="排序" value={edgeForm.sortOrder} onChange={(value) => setEdgeForm({ ...edgeForm, sortOrder: value })} type="number" />
              </div>
              <label className="admin-check mt-4">
                <input type="checkbox" checked={edgeForm.visible} onChange={(event) => setEdgeForm({ ...edgeForm, visible: event.target.checked })} />
                前台展示
              </label>
              {edgeLevelWarning ? <p className="mt-4 border border-red-200 bg-red-50 px-3 py-2 text-sm leading-6 text-red-700">{edgeLevelWarning}</p> : null}
            </form>

            <div className="admin-panel">
              <ListHeader icon={<Network size={18} />} title="关系列表" count={edges.length} />
              <div className="mt-5 grid gap-3">
                {edges.map((edge) => (
                  <article key={edge.id} className={`graph-admin-row ${editingEdgeId === edge.id ? "is-editing" : ""}`}>
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="graph-node-type">{edge.relationType}</span>
                        <h3 className="text-base font-semibold">
                          {nodeNameByKey.get(edge.fromNodeKey) ?? edge.fromNodeKey}
                          <span className="mx-2 text-graphite">→</span>
                          {nodeNameByKey.get(edge.toNodeKey) ?? edge.toNodeKey}
                        </h3>
                        {!edge.visible ? <span className="graph-hidden-badge">Hidden</span> : null}
                      </div>
                      <p className="mt-2 text-xs leading-5 text-graphite">{edge.fromNodeKey} → {edge.toNodeKey} / sort {edge.sortOrder}</p>
                    </div>
                    <div className="flex shrink-0 gap-2">
                      <button type="button" className="admin-link-button" onClick={() => startEdgeEditing(edge)}>编辑</button>
                      <button type="button" className="admin-danger-button" onClick={() => deleteEdge(edge.id)} disabled={busy === `delete-edge-${edge.id}`}>
                        {busy === `delete-edge-${edge.id}` ? <Loader2 className="animate-spin" size={15} /> : <Trash2 size={15} />}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}

function StatusTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="admin-status-tile">
      <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-graphite">{label}</p>
      <p className="mt-3 text-lg font-semibold">{value}</p>
    </div>
  );
}

function FormHeader({
  title,
  busy,
  onCancel,
  onFallback,
}: {
  title: string;
  busy: boolean;
  onCancel?: () => void;
  onFallback?: () => void;
}) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div>
        <p className="font-mono text-[10px] uppercase tracking-[0.22em] text-accent">Graph Editor</p>
        <h2 className="mt-3 text-2xl font-semibold">{title}</h2>
      </div>
      <div className="flex gap-2">
        {onCancel ? (
          <button type="button" className="secondary-action px-4 py-3 text-sm" onClick={onCancel}>取消</button>
        ) : null}
        {onFallback ? (
          <button type="button" className="secondary-action px-4 py-3 text-sm" onClick={onFallback} disabled={busy}>
            规则兜底新增
          </button>
        ) : null}
        <button disabled={busy} className="primary-action px-5 py-3 text-sm font-medium disabled:opacity-55">
          {busy ? <Loader2 className="animate-spin" size={16} /> : <Save size={16} />}
          保存
        </button>
      </div>
    </div>
  );
}

function ListHeader({ icon, title, count }: { icon: ReactNode; title: string; count: number }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <div className="flex items-center gap-3">
        <span className="graph-admin-icon">{icon}</span>
        <h2 className="text-2xl font-semibold">{title}</h2>
      </div>
      <span className="graph-admin-count">{count}</span>
    </div>
  );
}

function TextField({
  label,
  value,
  onChange,
  required = false,
  type = "text",
  step,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  type?: string;
  step?: string;
}) {
  return (
    <label className="block">
      <span className="text-xs font-medium text-graphite">{label}</span>
      <input
        type={type}
        step={step}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required={required}
        className="mt-1.5 w-full border border-line bg-white px-3 py-2.5 text-sm text-ink outline-none focus:border-accent"
      />
    </label>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
}) {
  return (
    <label className="block">
      <span className="text-xs font-medium text-graphite">{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1.5 w-full border border-line bg-white px-3 py-2.5 text-sm text-ink outline-none focus:border-accent"
      >
        {options.map((option) => (
          <option key={option || "empty"} value={option}>{option || "请选择节点"}</option>
        ))}
      </select>
    </label>
  );
}

function LevelSelect({
  label,
  value,
  nodeType,
  onChange,
}: {
  label: string;
  value: string;
  nodeType: string;
  onChange: (value: string) => void;
}) {
  const expected = nodeTypeLevels[nodeType] ?? "2";
  return (
    <label className="block">
      <span className="text-xs font-medium text-graphite">{label}</span>
      <select
        value={value || expected}
        onChange={(event) => onChange(event.target.value)}
        disabled
        className="mt-1.5 w-full border border-line bg-zinc-50 px-3 py-2.5 text-sm text-ink outline-none disabled:cursor-not-allowed disabled:text-graphite"
      >
        {levelOptions.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
      <span className="mt-1.5 block text-[11px] leading-4 text-graphite">层级由节点类型自动确定，避免跨级连接。</span>
    </label>
  );
}

function TextAreaField({
  label,
  value,
  onChange,
  className = "",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  className?: string;
}) {
  return (
    <label className={`block ${className}`}>
      <span className="text-xs font-medium text-graphite">{label}</span>
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-1.5 min-h-28 w-full resize-y border border-line bg-white px-3 py-2.5 text-sm leading-6 text-ink outline-none focus:border-accent"
      />
    </label>
  );
}

function nodePayload(form: NodeForm): KnowledgeGraphNodePayload {
  return {
    nodeKey: form.nodeKey.trim(),
    label: form.label.trim(),
    nodeType: form.nodeType,
    level: numberValue(form.level),
    summary: form.summary.trim(),
    content: form.content.trim(),
    tags: csvValues(form.tags),
    href: form.href.trim(),
    sourceType: form.sourceType.trim() || "MANUAL",
    sourceSlug: form.sourceSlug.trim(),
    x: numberValue(form.x),
    y: numberValue(form.y),
    z: numberValue(form.z),
    visible: form.visible,
    sortOrder: numberValue(form.sortOrder),
  };
}

function smartNodePayload(form: NodeForm): KnowledgeGraphSmartNodePayload {
  return {
    nodeKey: form.nodeKey.trim() || undefined,
    label: form.label.trim(),
    nodeType: form.nodeType,
    summary: form.summary.trim(),
    content: form.content.trim() || form.summary.trim(),
    tags: csvValues(form.tags),
    href: form.href.trim() || undefined,
    sourceType: form.sourceType.trim() || "MANUAL",
    sourceSlug: form.sourceSlug.trim() || undefined,
    level: form.level.trim() ? numberValue(form.level) : undefined,
    x: form.x.trim() ? numberValue(form.x) : undefined,
    y: form.y.trim() ? numberValue(form.y) : undefined,
    z: form.z.trim() ? numberValue(form.z) : undefined,
    visible: form.visible,
    sortOrder: form.sortOrder.trim() ? numberValue(form.sortOrder) : undefined,
  };
}

function edgePayload(form: EdgeForm): KnowledgeGraphEdgePayload {
  return {
    fromNodeKey: form.fromNodeKey,
    toNodeKey: form.toNodeKey,
    relationType: form.relationType,
    visible: form.visible,
    sortOrder: numberValue(form.sortOrder),
  };
}

function levelLabel(level: number) {
  return levelOptions.find((option) => option.value === String(level))?.label ?? `level ${level}`;
}

function edgeGuardMessage(form: EdgeForm, nodeByKey: Map<string, KnowledgeGraphNode>) {
  const from = nodeByKey.get(form.fromNodeKey);
  const to = nodeByKey.get(form.toNodeKey);
  if (!from || !to) return "";
  if (from.nodeKey === to.nodeKey) return "关系起点和终点不能相同。";
  if (Math.abs(from.level - to.level) > 1) {
    return `${levelLabel(from.level)} 不能直接连接 ${levelLabel(to.level)}，请先通过二级栏目承接。`;
  }
  if (from.level === 0 || to.level === 0) {
    return from.level === 0 && to.level === 1 && form.relationType === "OWNS" ? "" : "一级个人节点只能通过 OWNS 指向二级栏目。";
  }
  if (from.level === 1 || to.level === 1) {
    return from.level === 1 && to.level === 2 && ["INCLUDES", "CONTAINS"].includes(form.relationType)
      ? ""
      : "二级栏目只能通过 INCLUDES/CONTAINS 指向三级内容节点。";
  }
  if (from.level === 2 && to.level === 2 && !["USES", "EXPLAINS", "RELATED"].includes(form.relationType)) {
    return "三级内容节点之间只能使用 USES/EXPLAINS/RELATED。";
  }
  return "";
}

function csvValues(value: string) {
  return value.split(",").map((item) => item.trim()).filter(Boolean);
}

function numberValue(value: string) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}
