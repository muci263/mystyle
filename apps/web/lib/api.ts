export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type Profile = {
  name: string;
  title: string;
  summary: string;
  email: string;
  education: string;
  tags: string[];
};

export type SkillGroup = {
  category: string;
  items: string[];
};

export type Experience = {
  company: string;
  position: string;
  period: string;
  highlights: string[];
};

export type Evidence = {
  problem: string;
  solution: string;
  result: string;
};

export type Project = {
  index: string;
  slug: string;
  name: string;
  summary: string;
  role: string;
  tech: string[];
  metrics: string[];
  responsibilities: string[];
  evidence: Evidence[];
};

export type ModuleDemo = {
  slug: string;
  name: string;
  title: string;
  demoType: string;
  project: string;
  summary: string;
  tech: string[];
  apiBase: string;
  talkingPoints: string[];
};

export type InterviewGuide = {
  shortIntro: string;
  projectOrder: string[];
  questions: string[];
  openLinks: string[];
};

export type TimelineItem = {
  type: string;
  title: string;
  period: string;
  summary: string;
  tags: string[];
};

export type BlogPost = {
  slug: string;
  title: string;
  excerpt: string;
  content: string;
  category: string;
  tags: string[];
  publishedAt: string;
  readMinutes: number;
  likeCount: number;
  commentCount: number;
  annotationCount: number;
};

export type BlogCategory = {
  name: string;
  slug: string;
  code: string;
  postCount: number;
};

export type BlogComment = {
  id: number;
  author: string;
  content: string;
  createdAt: string;
};

export type BlogAnnotation = {
  id: number;
  anchorText: string;
  note: string;
  createdAt: string;
};

export type BlogInteractionSummary = {
  likeCount: number;
  commentCount: number;
  annotationCount: number;
};

export type KnowledgeGraphNode = {
  nodeKey: string;
  label: string;
  nodeType: "CORE" | "SECTION" | "SKILL" | "PROJECT" | "MODULE" | "BLOG" | string;
  level: number;
  summary: string;
  content: string;
  tags: string[];
  href: string;
  sourceType: string;
  sourceSlug: string;
  x: number;
  y: number;
  z: number;
  visible: boolean;
  sortOrder: number;
};

export type KnowledgeGraphEdge = {
  id: number;
  fromNodeKey: string;
  toNodeKey: string;
  relationType: string;
  visible: boolean;
  sortOrder: number;
};

export type KnowledgeGraphView = {
  nodes: KnowledgeGraphNode[];
  edges: KnowledgeGraphEdge[];
};

export type KnowledgeGraphNodePayload = {
  nodeKey: string;
  label: string;
  nodeType: string;
  level: number;
  summary: string;
  content: string;
  tags: string[];
  href: string;
  sourceType: string;
  sourceSlug: string;
  x: number;
  y: number;
  z: number;
  visible: boolean;
  sortOrder: number;
};

export type KnowledgeGraphEdgePayload = {
  fromNodeKey: string;
  toNodeKey: string;
  relationType: string;
  visible: boolean;
  sortOrder: number;
};

export type KnowledgeGraphSmartNodePayload = {
  nodeKey?: string;
  label: string;
  nodeType: string;
  summary: string;
  content?: string;
  tags: string[];
  href?: string;
  sourceType?: string;
  sourceSlug?: string;
  level?: number;
  x?: number;
  y?: number;
  z?: number;
  visible?: boolean;
  sortOrder?: number;
};

export type KnowledgeRelationSuggestion = {
  fromNodeKey: string;
  toNodeKey: string;
  relationType: string;
  reason: string;
};

export type KnowledgeGraphAutoRelateResponse = {
  provider: string;
  node: KnowledgeGraphNode;
  suggestions: KnowledgeRelationSuggestion[];
  createdEdges: KnowledgeGraphEdge[];
  notes: string[];
};

export type KnowledgeGraphOrchestrateResponse = {
  provider: string;
  scannedNodes: number;
  createdEdges: number;
  results: KnowledgeGraphAutoRelateResponse[];
  notes: string[];
};

export type LlmProviderStatus = {
  provider: string;
  configured: boolean;
  textModel: string;
  visionModel: string;
  mode: string;
};

export type HomeView = {
  profile: Profile;
  skills: SkillGroup[];
  featuredProjects: Project[];
  moduleDemos: ModuleDemo[];
  interviewGuide: InterviewGuide;
  knowledgeGraph: KnowledgeGraphView;
};

export type ResumeView = {
  profile: Profile;
  skills: SkillGroup[];
  experiences: Experience[];
  projects: Project[];
};

export type ResumeSectionType = "SKILL" | "AWARD" | "INTERNSHIP" | "PROJECT" | "ADVANTAGE";

export type ResumeVersion = {
  id: number;
  versionName: string;
  status: string;
  sourceTaskId: number | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ResumeBasicInfo = {
  id: number;
  versionId: number;
  name: string;
  title: string;
  summary: string;
  email: string;
  phone: string;
  location: string;
  education: string;
  githubUrl: string;
  websiteUrl: string;
  updatedAt: string;
};

export type ResumeSectionItem = {
  id: number;
  versionId: number;
  sectionType: ResumeSectionType;
  title: string;
  subtitle: string;
  period: string;
  summary: string;
  detail: string;
  tags: string[];
  visible: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
};

export type ResumeDraftView = {
  version: ResumeVersion;
  basicInfo: ResumeBasicInfo;
  sections: Record<ResumeSectionType, ResumeSectionItem[]>;
};

export type ResumeUploadTask = {
  id: number;
  filename: string;
  contentType: string;
  status: string;
  rawText: string;
  parsedJson: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
};

export type JdAnalysisResponse = {
  analysisId: number;
  provider: string;
  role: string;
  keywords: string[];
  matchScore: number;
  summary: string;
  projectRecommendations: Array<{
    slug: string;
    name: string;
    emphasis: string;
    supportedBy: string[];
  }>;
  moduleRecommendations: Array<{
    slug: string;
    title: string;
    reason: string;
  }>;
  resumeOptimizations: string[];
  interviewTalkingPoints: string[];
  riskNotes: string[];
};

export type ResumeOptimizeResponse = {
  provider: string;
  role: string;
  summary: string;
  rewrittenSummary: string;
  highlights: string[];
  sectionSuggestions: string[];
  riskNotes: string[];
};

export type VideoLearningSnapshot = {
  redisRecord: Record<string, unknown>;
  mysqlRecord: Record<string, unknown>;
  learningStatus: string;
  writeCount: number;
  syncCount: number;
  logs: Array<{
    event: string;
    message: string;
    time: string;
  }>;
};

export type AgentWorkflowRun = {
  runId: string;
  status: string;
  question: string;
  answer: string;
  steps: Array<{
    node: string;
    status: string;
    detail: string;
  }>;
};

function apiBase() {
  if (typeof window !== "undefined") {
    return process.env.NEXT_PUBLIC_API_BASE_URL ?? "/api";
  }
  return process.env.API_INTERNAL_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBase()}${path}`, {
    cache: "no-store",
  });
  return unwrapResponse<T>(response);
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${apiBase()}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    cache: "no-store",
  });
  return unwrapResponse<T>(response);
}

export async function apiPut<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${apiBase()}${path}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    cache: "no-store",
  });
  return unwrapResponse<T>(response);
}

export async function apiDelete<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBase()}${path}`, {
    method: "DELETE",
    cache: "no-store",
  });
  return unwrapResponse<T>(response);
}

async function unwrapResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`API request failed: ${response.status} ${response.statusText}`);
  }
  const payload = (await response.json()) as ApiResponse<T>;
  if (payload.code !== 0) {
    throw new Error(payload.message || "API request failed");
  }
  return payload.data;
}
