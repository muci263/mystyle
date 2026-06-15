"use client";

import { Line, OrbitControls, Stars, Text } from "@react-three/drei";
import { Canvas, useFrame, useThree } from "@react-three/fiber";
import { useReducedMotion } from "framer-motion";
import { useEffect, useMemo, useRef, useState } from "react";
import type { RefObject } from "react";
import type { Camera, Group } from "three";
import type { KnowledgeGraphNode, KnowledgeGraphView } from "@/lib/api";

type NodeVisualType = "core" | "section" | "skill" | "project" | "module" | "blog";

type SceneNode = KnowledgeGraphNode & {
  id: string;
  visualType: NodeVisualType;
  position: [number, number, number];
};

export type KnowledgeGraphFocusedNode = KnowledgeGraphNode & {
  relatedCount: number;
};

const typeColors: Record<NodeVisualType, string> = {
  core: "#ffffff",
  section: "#9fc5ff",
  skill: "#6ea8ff",
  project: "#2dd4bf",
  module: "#7dd3fc",
  blog: "#c4b5fd",
};

const GRAPH_MIN_DISTANCE = 6.4;
const GRAPH_MAX_DISTANCE = 13.8;
const SCROLL_RELEASE_EPSILON = 0.04;
const WHEEL_ZOOM_DISTANCE_FACTOR = 0.006;

export function KnowledgeGraphScene({
  graph,
  onNodeFocus,
  onNodeBlur,
}: {
  graph: KnowledgeGraphView;
  onNodeFocus?: (node: KnowledgeGraphFocusedNode) => void;
  onNodeBlur?: () => void;
}) {
  const sceneGraph = useMemo(() => normalizeGraph(graph), [graph]);
  const [hoveredId, setHoveredId] = useState<string | null>(null);
  const [activeId, setActiveId] = useState("me");
  const cameraDistanceRef = useRef(GRAPH_MAX_DISTANCE);
  const cameraRef = useRef<Camera | null>(null);
  const shellRef = useRef<HTMLDivElement>(null);
  const reduceMotion = Boolean(useReducedMotion());
  const nodeById = useMemo(() => new Map(sceneGraph.nodes.map((node) => [node.id, node])), [sceneGraph.nodes]);
  const activeNode = nodeById.get(activeId) ?? nodeById.get("me") ?? sceneGraph.nodes[0];
  const hoveredNode = hoveredId ? nodeById.get(hoveredId) : null;
  const focusId = hoveredNode?.id ?? activeNode?.id ?? "me";
  const relatedIds = useMemo(
      () => relatedNodeIds(sceneGraph.edges, focusId),
      [sceneGraph.edges, focusId],
  );
  function notifyFocus(nodeId: string) {
    const node = nodeById.get(nodeId);
    if (!node || !onNodeFocus) return;
    const related = relatedNodeIds(sceneGraph.edges, node.id);
    onNodeFocus({
      ...node,
      relatedCount: related.size,
    });
  }

  useEffect(() => {
    const shell = shellRef.current;
    if (!shell) return;
    const wheelTarget = shell.closest<HTMLElement>(".hero-stage") ?? shell;

    wheelTarget.addEventListener("wheel", handleGraphWheel, { capture: true, passive: false });
    return () => {
      wheelTarget.removeEventListener("wheel", handleGraphWheel, { capture: true });
    };
  }, []);

  function handleGraphWheel(event: WheelEvent) {
    const camera = cameraRef.current;
    if (!camera) {
      return;
    }

    if (event.cancelable) {
      event.preventDefault();
    }
    event.stopPropagation();

    const currentDistance = camera.position.length();
    const wheelDelta = normalizedWheelDelta(event);
    if (wheelDelta < 0) {
      setCameraDistance(camera, Math.max(GRAPH_MIN_DISTANCE, currentDistance + wheelDelta * WHEEL_ZOOM_DISTANCE_FACTOR));
      return;
    }

    if (currentDistance >= GRAPH_MAX_DISTANCE - SCROLL_RELEASE_EPSILON) {
      scrollPage(wheelDelta);
      return;
    }

    const requestedZoomOut = wheelDelta * WHEEL_ZOOM_DISTANCE_FACTOR;
    const availableZoomOut = GRAPH_MAX_DISTANCE - currentDistance;
    const appliedZoomOut = Math.min(availableZoomOut, requestedZoomOut);
    setCameraDistance(camera, currentDistance + appliedZoomOut);

    const unusedWheelDelta = (requestedZoomOut - appliedZoomOut) / WHEEL_ZOOM_DISTANCE_FACTOR;
    if (unusedWheelDelta > 0) {
      scrollPage(unusedWheelDelta);
    }
  }

function scrollPage(deltaY: number) {
    const scroller = document.scrollingElement ?? document.documentElement;
    scroller.scrollTo({
      top: scroller.scrollTop + deltaY,
      behavior: "auto",
    });
  }

  return (
    <div ref={shellRef} className="knowledge-graph-shell">
      <Canvas camera={{ position: [0, 0.15, 10.6], fov: 42 }} dpr={[1, 1.55]}>
        <CameraDistanceProbe cameraRef={cameraRef} distanceRef={cameraDistanceRef} />
        <ambientLight intensity={0.58} />
        <pointLight position={[2.8, 4.2, 5]} intensity={3.1} color="#dbe7ff" />
        <pointLight position={[-4, -2, 3]} intensity={2.2} color="#2563eb" />
        <Stars radius={54} depth={34} count={420} factor={2.1} saturation={0} fade speed={reduceMotion ? 0 : 0.16} />
        <KnowledgeGraphMesh
          graph={sceneGraph}
          activeId={activeNode?.id ?? "me"}
          focusId={focusId}
          revealEdges={Boolean(hoveredId)}
          relatedIds={relatedIds}
          reduceMotion={reduceMotion}
          onHover={setHoveredId}
          onSelect={(nodeId) => {
            setActiveId(nodeId);
            notifyFocus(nodeId);
          }}
          onFocus={notifyFocus}
          onBlur={() => onNodeBlur?.()}
        />
        <OrbitControls
          enablePan={false}
          enableDamping
          dampingFactor={0.12}
          minDistance={GRAPH_MIN_DISTANCE}
          maxDistance={GRAPH_MAX_DISTANCE}
          zoomSpeed={0.28}
          enableZoom={false}
          autoRotate={!reduceMotion && hoveredId === null}
          autoRotateSpeed={0.16}
          rotateSpeed={0.38}
        />
      </Canvas>
    </div>
  );
}

function normalizedWheelDelta(event: WheelEvent) {
  if (event.deltaMode === WheelEvent.DOM_DELTA_LINE) {
    return event.deltaY * 16;
  }
  if (event.deltaMode === WheelEvent.DOM_DELTA_PAGE) {
    return event.deltaY * window.innerHeight;
  }
  return event.deltaY;
}

function CameraDistanceProbe({
  cameraRef,
  distanceRef,
}: {
  cameraRef: RefObject<Camera | null>;
  distanceRef: RefObject<number>;
}) {
  const { camera } = useThree();

  useFrame(() => {
    cameraRef.current = camera;
    distanceRef.current = camera.position.length();
  });

  return null;
}

function setCameraDistance(camera: Camera, distance: number) {
  const nextDistance = clamp(distance, GRAPH_MIN_DISTANCE, GRAPH_MAX_DISTANCE);
  camera.position.setLength(nextDistance);
  camera.updateMatrixWorld();
}

function KnowledgeGraphMesh({
  graph,
  activeId,
  focusId,
  revealEdges,
  relatedIds,
  reduceMotion,
  onHover,
  onSelect,
  onFocus,
  onBlur,
}: {
  graph: { nodes: SceneNode[]; edges: KnowledgeGraphView["edges"] };
  activeId: string;
  focusId: string;
  revealEdges: boolean;
  relatedIds: Set<string>;
  reduceMotion: boolean;
  onHover: (id: string | null) => void;
  onSelect: (id: string) => void;
  onFocus: (id: string) => void;
  onBlur: () => void;
}) {
  const groupRef = useRef<Group>(null);
  const nodeById = useMemo(() => new Map(graph.nodes.map((node) => [node.id, node])), [graph.nodes]);

  useFrame((_, delta) => {
    if (!groupRef.current || reduceMotion || revealEdges) {
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
        const highlighted =
          revealEdges && (edge.fromNodeKey === focusId || edge.toNodeKey === focusId);
        if (!edge.visible && !highlighted) return null;
        return (
          <Line
            key={edge.id || `${edge.fromNodeKey}-${edge.toNodeKey}-${edge.relationType}`}
            points={[from.position, to.position]}
            color={highlighted ? "#9fc5ff" : "#263550"}
            lineWidth={highlighted ? 1.65 : 0.56}
            transparent
            opacity={highlighted ? 0.9 : 0.24}
          />
        );
      })}

      {graph.nodes.map((node) => {
        const active = activeId === node.id;
        const highlighted = relatedIds.has(node.id);
        const color = typeColors[node.visualType];
        const radius = nodeRadius(node);
        const showLabel = node.level <= 1 || active || highlighted;

        return (
          <group key={node.id} position={node.position}>
            <mesh
              onPointerOver={(event) => {
                event.stopPropagation();
                onHover(node.id);
                onFocus(node.id);
                document.body.style.cursor = "pointer";
              }}
              onPointerOut={() => {
                onHover(null);
                onBlur();
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
            {showLabel ? (
              <Text
                position={[0, radius + 0.24, 0]}
                fontSize={node.visualType === "core" ? 0.26 : node.level <= 1 ? 0.16 : 0.125}
                color={highlighted ? "#ffffff" : "#8b9bb6"}
                anchorX="center"
                anchorY="middle"
                maxWidth={node.level <= 1 ? 2.2 : 1.45}
                textAlign="center"
              >
                {node.label}
              </Text>
            ) : null}
          </group>
        );
      })}
    </group>
  );
}

function normalizeGraph(graph: KnowledgeGraphView) {
  const baseNodes: SceneNode[] = graph.nodes.map((node) => ({
    ...node,
    id: node.nodeKey,
    visualType: visualType(node.nodeType),
    position: [node.x, node.y, node.z],
  }));
  const nodeIds = new Set(baseNodes.map((node) => node.id));
  const edges = graph.edges.filter((edge) => nodeIds.has(edge.fromNodeKey) && nodeIds.has(edge.toNodeKey));
  const nodes = balanceNodePositions(baseNodes, edges);
  return { nodes, edges };
}

function balanceNodePositions(nodes: SceneNode[], edges: KnowledgeGraphView["edges"]) {
  const nodeById = new Map(nodes.map((node) => [node.id, node]));
  const sectionNodes = nodes
    .filter((node) => node.visualType === "section" || node.level === 1)
    .sort((a, b) => a.sortOrder - b.sortOrder);
  const sectionAngles = new Map<string, number>();
  const sectionCount = Math.max(sectionNodes.length, 1);
  sectionNodes.forEach((node, index) => {
    sectionAngles.set(node.id, -Math.PI + ((index + 0.5) / sectionCount) * Math.PI * 2);
  });

  const primaryParentByChild = new Map<string, string>();
  edges
    .filter((edge) => ["OWNS", "INCLUDES", "CONTAINS"].includes(edge.relationType))
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .forEach((edge) => {
      if (!primaryParentByChild.has(edge.toNodeKey)) {
        primaryParentByChild.set(edge.toNodeKey, edge.fromNodeKey);
      }
    });

  const childrenByParent = new Map<string, SceneNode[]>();
  nodes.forEach((node) => {
    const parentId = primaryParentByChild.get(node.id);
    if (!parentId || !nodeById.has(parentId)) return;
    const children = childrenByParent.get(parentId) ?? [];
    children.push(node);
    childrenByParent.set(parentId, children);
  });
  childrenByParent.forEach((children) => children.sort((a, b) => a.sortOrder - b.sortOrder));

  const initialNodes = nodes.map((node) => {
    if (node.visualType === "core") {
      return { ...node, position: [0, 0, 0] as [number, number, number] };
    }

    if (sectionAngles.has(node.id)) {
      const angle = sectionAngles.get(node.id) ?? 0;
      return {
        ...node,
        position: [Math.cos(angle) * 2.05, Math.sin(angle) * 1.58, node.z * 0.36] as [number, number, number],
      };
    }

    const parentId = primaryParentByChild.get(node.id);
    const parentAngle = parentId ? sectionAngles.get(parentId) : undefined;
    if (parentAngle !== undefined) {
      const siblings = childrenByParent.get(parentId ?? "") ?? [node];
      const index = Math.max(0, siblings.findIndex((item) => item.id === node.id));
      const spread = Math.min(1.15, Math.max(0.45, siblings.length * 0.18));
      const offset = siblings.length <= 1 ? 0 : -spread / 2 + (spread * index) / (siblings.length - 1);
      const angle = parentAngle + offset;
      const radius = node.visualType === "blog" ? 3.2 : node.visualType === "project" ? 3.05 : 2.88;
      return {
        ...node,
        position: [Math.cos(angle) * radius, Math.sin(angle) * 2.15, node.z * 0.42] as [number, number, number],
      };
    }

    return {
      ...node,
      position: [node.x * 0.58, node.y * 0.68, node.z * 0.42] as [number, number, number],
    };
  });

  return forceLayout(initialNodes, edges);
}

function forceLayout(nodes: SceneNode[], edges: KnowledgeGraphView["edges"]) {
  const positions = new Map(nodes.map((node) => [node.id, { x: node.position[0], y: node.position[1], z: node.position[2] }]));
  const anchors = new Map(nodes.map((node) => [node.id, { x: node.position[0], y: node.position[1], z: node.position[2] }]));
  const nodeById = new Map(nodes.map((node) => [node.id, node]));
  const edgePairs = edges
    .filter((edge) => nodeById.has(edge.fromNodeKey) && nodeById.has(edge.toNodeKey))
    .map((edge) => [edge.fromNodeKey, edge.toNodeKey] as const);

  for (let step = 0; step < 110; step++) {
    const deltas = new Map(nodes.map((node) => [node.id, { x: 0, y: 0, z: 0 }]));

    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        const left = nodes[i];
        const right = nodes[j];
        const a = positions.get(left.id)!;
        const b = positions.get(right.id)!;
        let dx = a.x - b.x;
        let dy = a.y - b.y;
        let dz = a.z - b.z;
        const distance = Math.max(0.18, Math.sqrt(dx * dx + dy * dy + dz * dz));
        dx /= distance;
        dy /= distance;
        dz /= distance;
        const minDistance = collisionRadius(left) + collisionRadius(right) + (left.level <= 1 || right.level <= 1 ? 0.58 : 0.38);
        const repel = distance < minDistance ? (minDistance - distance) * 0.08 : 0.018 / (distance * distance);
        deltas.get(left.id)!.x += dx * repel;
        deltas.get(left.id)!.y += dy * repel;
        deltas.get(left.id)!.z += dz * repel * 0.35;
        deltas.get(right.id)!.x -= dx * repel;
        deltas.get(right.id)!.y -= dy * repel;
        deltas.get(right.id)!.z -= dz * repel * 0.35;
      }
    }

    edgePairs.forEach(([fromId, toId]) => {
      const from = positions.get(fromId)!;
      const to = positions.get(toId)!;
      const fromNode = nodeById.get(fromId)!;
      const toNode = nodeById.get(toId)!;
      const dx = to.x - from.x;
      const dy = to.y - from.y;
      const dz = to.z - from.z;
      const distance = Math.max(0.2, Math.sqrt(dx * dx + dy * dy + dz * dz));
      const target = fromNode.level === 0 || toNode.level === 0 ? 1.82 : 1.28;
      const pull = (distance - target) * 0.018;
      const nx = dx / distance;
      const ny = dy / distance;
      const nz = dz / distance;
      if (fromNode.visualType !== "core") {
        deltas.get(fromId)!.x += nx * pull;
        deltas.get(fromId)!.y += ny * pull;
        deltas.get(fromId)!.z += nz * pull * 0.4;
      }
      if (toNode.visualType !== "core") {
        deltas.get(toId)!.x -= nx * pull;
        deltas.get(toId)!.y -= ny * pull;
        deltas.get(toId)!.z -= nz * pull * 0.4;
      }
    });

    nodes.forEach((node) => {
      const position = positions.get(node.id)!;
      const anchor = anchors.get(node.id)!;
      const delta = deltas.get(node.id)!;
      if (node.visualType === "core") {
        positions.set(node.id, { x: 0, y: 0, z: 0 });
        return;
      }
      const anchorPull = node.level <= 1 ? 0.03 : 0.018;
      delta.x += (anchor.x - position.x) * anchorPull;
      delta.y += (anchor.y - position.y) * anchorPull;
      delta.z += (anchor.z - position.z) * anchorPull * 0.4;
      position.x = clamp(position.x + delta.x, -3.45, 3.45);
      position.y = clamp(position.y + delta.y, -2.38, 2.38);
      position.z = clamp(position.z + delta.z, -0.86, 0.86);
    });
  }

  return nodes.map((node) => {
    const position = positions.get(node.id)!;
    return { ...node, position: [position.x, position.y, position.z] as [number, number, number] };
  });
}

function collisionRadius(node: SceneNode) {
  if (node.visualType === "core") return 0.78;
  if (node.level <= 1) return 0.52;
  return 0.38;
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
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
