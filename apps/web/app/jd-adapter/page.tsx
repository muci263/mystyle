import { PageHeader, SiteShell } from "@/components/site-shell";
import { JdAdapterClient } from "./jd-adapter-client";

export default function JdAdapterPage() {
  return (
    <SiteShell>
      <PageHeader
        eyebrow="JD Adapter"
        title="JD 智能适配器"
        description="第一版先明确业务流程：输入岗位 JD，系统基于真实经历资产生成岗位匹配、项目排序、模块推荐和风险提示。真实 LLM 接入会在后续阶段实现。"
      />

      <JdAdapterClient />
    </SiteShell>
  );
}
