import { SiteShell } from "@/components/site-shell";
import { ScrollPortfolio } from "@/components/scroll-portfolio";
import { apiGet } from "@/lib/api";
import type { HomeView } from "@/lib/api";

export default async function HomePage() {
  const home = await apiGet<HomeView>("/public/home");

  return (
    <SiteShell>
      <ScrollPortfolio home={home} />
    </SiteShell>
  );
}
