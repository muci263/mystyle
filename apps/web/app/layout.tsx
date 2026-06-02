import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "赵豪然 | Engineering Evidence",
  description: "Java 后端工程作品集：项目证据、工作模块复现、JD 适配与工程化部署展示。",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <body suppressHydrationWarning>{children}</body>
    </html>
  );
}
