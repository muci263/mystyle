"use client";

import { Line, OrbitControls, Stars, Text } from "@react-three/drei";
import { Canvas, useFrame } from "@react-three/fiber";
import { useReducedMotion } from "framer-motion";
import { useMemo, useRef, useState } from "react";
import type { Group } from "three";
import type { KnowledgeGraphNode, KnowledgeGraphView } from "@/lib/api";

type NodeVisualType = "core" | "section" | "skill" | "project" | "module" | "blog";

type SceneNode = KnowledgeGraphNode & {
  id: string;
  visualType: NodeVisualType;
  position: [number, number, number];
};

const typeColors: Record<NodeVisualType, string> = {
  core: "#ffffff",
  section: "#9fc5ff",
  skill: "#6ea8ff",
  project: "#2dd4bf",
  module: "#7dd3fc",
  blog: "#c4b5fd",
};

export function KnowledgeGraphScene({ graph }: { graph: KnowledgeGraphView }) {
  const sceneGraph = useMemo(() => normalizeGraph(graph), [graph]);
  const [hoveredId, setHoveredId] = useState<string | null>(null);
  const [activeId, setActiveId] = useState("me");
  const reduceMotion = Boolean(useReducedMotion());
  const nodeById = useMemo(() => new Map(sceneGraph.nodes.map((node) => [node.id, node])), [sceneGraph.nodes]);
  const activeNode = nodeById.get(activeId) ?? nodeById.get("me") ?? sceneGraph.nodes[0];
  const hoveredNode = hoveredId ? nodeById.get(hoveredId) : null;
  const relatedIds = useMemo(
      () => relatedNodeIds(sceneGraph.edges, hoveredNode?.id ?? activeNode?.id ?? "me"),
      [sceneGraph.edges, hoveredNode?.id, activeNode?.id],
  );
  const visibleDetail = hoveredNode ?? (activeNode?.id !== "me" ? activeNode : null);

  return (
    <div className="knowledge-graph-shell">
      <Canvas camera={{ position: [0, 0.15, 10.6], fov: 42 }} dpr={[1, 1.55]}>
        <ambientLight intensity={0.58} />
        <pointLight position={[2.8, 4.2, 5]} intensity={3.1} color="#dbe7ff" />
        <pointLight position={[-4, -2, 3]} intensity={2.2} color="#2563eb" />
        <Stars radius={54} depth={34} count={420} factor={2.1} saturation={0} fade speed={reduceMotion ? 0 : 0.16} />
        <KnowledgeGraphMesh
          graph={sceneGraph}
          activeId={activeNode?.id ?? "me"}
          hoveredId={hoveredId}
          relatedIds={relatedIds}
          reduceMotion={reduceMotion}
          onHover={setHoveredId}
          onSelect={setActiveId}
        />
        <OrbitControls
          enableZoom={false}
          enablePan={false}
          enableDamping
          dampingFactor={0.08}
          autoRotate={!reduceMotion && hoveredId === null}
          autoRotateSpeed={0.24}
          rotateSpeed={0.38}
        />
      </Canvas>

      {visibleDetail ? (
        <aside className="knowledge-node-popover" aria-live="polite">
          <div className="flex items-center justify-between gap-4">
            <span>{visibleDetail.nodeType}</span>
            <span>{String(relatedIds.size).padStart(2, "0")} links</span>
          </div>
          <h2>{visibleDetail.label}</h2>
          <p>{visibleDetail.content || visibleDetail.summary}</p>
          {visibleDetail.tags.length ? (
            <div className="flex flex-wrap gap-2">
              {visibleDetail.tags.slice(0, 4).map((tag) => (
                <span key={tag}>{tag}</span>
              ))}
            </div>
          ) : null}
        </aside>
      ) : null}
    </div>
  );
}

function KnowledgeGraphMesh({
  graph,
  activeId,
  hoveredId,
  relatedIds,
  reduceMotion,
  onHover,
  onSelect,
}: {
  graph: { nodes: SceneNode[]; edges: KnowledgeGraphView["edges"] };
  activeId: string;
  hoveredId: string | null;
  relatedIds: Set<string>;
  reduceMotion: boolean;
  onHover: (id: string | null) => void;
  onSelect: (id: string) => void;
}) {
  const groupRef = useRef<Group>(null);
  const nodeById = useMemo(() => new Map(graph.nodes.map((node) => [node.id, node])), [graph.nodes]);

  useFrame((_, delta) => {
    if (!groupRef.current || reduceMotion || hoveredId) {
      return;
    }
    groupRef.current.rotation.y += delta * 0.038;
    groupRef.current.rotation.x = Math.sin(Date.now() * 0.0003) * 0.03;
  });

  return (
    <group ref={groupRef}>
      {graph.edges.map((edge) => {
        const from = nodeById.get(edge.fromNodeKey);
        const to = nodeById.get(edge.toNodeKey);
        if (!from || !to) return null;
        const highlighted = relatedIds.has(edge.fromNodeKey) && relatedIds.has(edge.toNodeKey);
        return (
          <Line
            key={edge.id || `${edge.fromNodeKey}-${edge.toNodeKey}-${edge.relationType}`}
            points={[from.position, to.position]}
            color={highlighted ? "#9fc5ff" : "#263550"}
            lineWidth={highlighted ? 1.65 : 0.56}
            transparent
            opacity={highlighted ? 0.86 : 0.28}
          />
        );
      })}

      {graph.nodes.map((node) => {
        const active = activeId === node.id;
        const highlighted = relatedIds.has(node.id);
        const color = typeColors[node.visualType];
        const radius = nodeRadius(node);

        return (
          <group key={node.id} position={node.position}>
            <mesh
              onPointerOver={(event) => {
                event.stopPropagation();
                onHover(node.id);
                document.body.style.cursor = "pointer";
              }}
              onPointerOut={() => {
                onHover(null);
                document.body.style.cursor = "";
              }}
              onClick={(event) => {
                event.stopPropagation();
                onSelect(node.id);
              }}
              scale={active ? 1.22 : highlighted ? 1.07 : 0.86}
            >
              <sphereGeometry args={[radius, 28, 28]} />
              <meshStandardMaterial
                color={color}
                emissive={color}
                emissiveIntensity={active ? 1.22 : highlighted ? 0.72 : 0.22}
                roughness={0.24}
                metalness={0.42}
                transparent
                opacity={highlighted ? 1 : 0.62}
              />
            </mesh>
            <Text
              position={[0, radius + 0.24, 0]}
              fontSize={node.visualType === "core" ? 0.28 : node.level <= 1 ? 0.17 : 0.14}
              color={highlighted ? "#ffffff" : "#8b9bb6"}
              anchorX="center"
              anchorY="middle"
              maxWidth={node.level <= 1 ? 2.2 : 1.7}
              textAlign="center"
            >
              {node.label}
            </Text>
          </group>
        );
      })}
    </group>
  );
}

function normalizeGraph(graph: KnowledgeGraphView) {
  const nodes: SceneNode[] = graph.nodes.map((node) => ({
    ...node,
    id: node.nodeKey,
    visualType: visualType(node.nodeType),
    position: [node.x, node.y, node.z],
  }));
  const nodeIds = new Set(nodes.map((node) => node.id));
  const edges = graph.edges.filter((edge) => nodeIds.has(edge.fromNodeKey) && nodeIds.has(edge.toNodeKey));
  return { nodes, edges };
}

function relatedNodeIds(edges: KnowledgeGraphView["edges"], nodeId: string) {
  const ids = new Set([nodeId]);
  edges.forEach((edge) => {
    if (edge.fromNodeKey === nodeId) ids.add(edge.toNodeKey);
    if (edge.toNodeKey === nodeId) ids.add(edge.fromNodeKey);
  });
  return ids;
}

function visualType(type: string): NodeVisualType {
  const normalized = type.toLowerCase();
  if (normalized === "core") return "core";
  if (normalized === "section") return "section";
  if (normalized === "project") return "project";
  if (normalized === "module") return "module";
  if (normalized === "blog") return "blog";
  return "skill";
}

function nodeRadius(node: SceneNode) {
  if (node.visualType === "core") return 0.38;
  if (node.level <= 1) return 0.22;
  if (node.visualType === "blog") return 0.15;
  return 0.16;
}
