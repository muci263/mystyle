import { PageHeader, SiteShell } from "@/components/site-shell";
import { JdAdapterClient } from "./jd-adapter-client";

export default function JdAdapterPage() {
  return (
    <SiteShell>
      <PageHeader
        eyebrow="JD Adapter"
        title="JD 智能适配器"
        description="上传或粘贴简历后输入岗位 JD，系统会基于真实经历资产调用 LLM 生成岗位匹配、JD 适配版简历和模拟面试问答。"
      />

      <JdAdapterClient />
    </SiteShell>
  );
}
