import { SiteShell } from "@/components/site-shell";
import { apiGet } from "@/lib/api";
import type { KnowledgeGraphEdge, KnowledgeGraphNode } from "@/lib/api";
import { KnowledgeGraphAdminClient } from "./knowledge-graph-admin-client";

type KnowledgeGraphAdminPageProps = {
  searchParams: Promise<{ nodeId?: string }>;
};

export default async function KnowledgeGraphAdminPage({ searchParams }: KnowledgeGraphAdminPageProps) {
  const { nodeId } = await searchParams;
  const [nodes, edges] = await Promise.all([
    apiGet<KnowledgeGraphNode[]>("/admin/knowledge-graph/nodes"),
    apiGet<KnowledgeGraphEdge[]>("/admin/knowledge-graph/edges"),
  ]);

  return (
    <SiteShell>
      <KnowledgeGraphAdminClient initialNodes={nodes} initialEdges={edges} initialNodeId={nodeId} />
    </SiteShell>
  );
}
