import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#09090b",
        graphite: "#5c6068",
        stonepaper: "#fafafa",
        line: "rgba(9,9,11,0.11)",
        accent: "#2563eb",
        cobalt: "#2563eb",
      },
      fontFamily: {
        sans: ["SF Pro Text", "Helvetica Neue", "PingFang SC", "sans-serif"],
        display: ["SF Pro Display", "Helvetica Neue", "PingFang SC", "sans-serif"],
        mono: ["JetBrains Mono", "ui-monospace", "SFMono-Regular", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
