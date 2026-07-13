import Link from "next/link";
import type React from "react";

export type MarkdownBlock =
  | { type: "heading"; depth: number; text: string }
  | { type: "paragraph"; text: string }
  | { type: "quote"; text: string }
  | { type: "list"; ordered: boolean; items: string[] }
  | { type: "code"; language: string; code: string }
  | { type: "rule" };

export function MarkdownRenderer({ content, className = "" }: { content: string; className?: string }) {
  return (
    <div className={`markdown-body ${className}`}>
      {parseMarkdownBlocks(content).map((block, index) => {
        if (block.type === "heading") {
          const Heading = block.depth <= 2 ? "h2" : block.depth === 3 ? "h3" : "h4";
          return <Heading key={index}>{renderMarkdownInline(block.text)}</Heading>;
        }
        if (block.type === "quote") {
          return <blockquote key={index}>{renderMarkdownInline(block.text)}</blockquote>;
        }
        if (block.type === "list") {
          const List = block.ordered ? "ol" : "ul";
          return (
            <List key={index}>
              {block.items.map((item, itemIndex) => (
                <li key={`${item}-${itemIndex}`}>{renderMarkdownInline(item)}</li>
              ))}
            </List>
          );
        }
        if (block.type === "code") {
          return (
            <pre key={index}>
              <code>{block.code}</code>
            </pre>
          );
        }
        if (block.type === "rule") {
          return <hr key={index} />;
        }
        return <p key={index}>{renderMarkdownInline(block.text)}</p>;
      })}
    </div>
  );
}

export function parseMarkdownBlocks(content: string): MarkdownBlock[] {
  const lines = normalizeMarkdown(content).split("\n");
  const blocks: MarkdownBlock[] = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim()) {
      index++;
      continue;
    }

    const fence = line.match(/^```(\w+)?\s*$/);
    if (fence) {
      const language = fence[1] ?? "";
      const codeLines: string[] = [];
      index++;
      while (index < lines.length && !/^```\s*$/.test(lines[index])) {
        codeLines.push(lines[index]);
        index++;
      }
      blocks.push({ type: "code", language, code: codeLines.join("\n") });
      index += index < lines.length ? 1 : 0;
      continue;
    }

    const heading = line.match(/^(#{1,4})\s+(.+)$/);
    if (heading) {
      blocks.push({ type: "heading", depth: heading[1].length, text: heading[2].trim() });
      index++;
      continue;
    }

    if (/^ {0,3}([-*_])(?:\s*\1){2,}\s*$/.test(line)) {
      blocks.push({ type: "rule" });
      index++;
      continue;
    }

    if (/^>\s+/.test(line)) {
      const quoteLines: string[] = [];
      while (index < lines.length && /^>\s?/.test(lines[index])) {
        quoteLines.push(lines[index].replace(/^>\s?/, "").trim());
        index++;
      }
      blocks.push({ type: "quote", text: quoteLines.join(" ") });
      continue;
    }

    const listMatch = line.match(/^(\d+\.\s+|[-*]\s+)(.+)$/);
    if (listMatch) {
      const ordered = /^\d+\./.test(listMatch[1]);
      const items: string[] = [];
      while (index < lines.length) {
        const item = lines[index].match(/^(\d+\.\s+|[-*]\s+)(.+)$/);
        if (!item || /^\d+\./.test(item[1]) !== ordered) break;
        items.push(item[2].trim());
        index++;
      }
      blocks.push({ type: "list", ordered, items });
      continue;
    }

    const paragraphLines = [line.trim()];
    index++;
    while (
      index < lines.length
      && lines[index].trim()
      && !/^(#{1,4})\s+/.test(lines[index])
      && !/^ {0,3}([-*_])(?:\s*\1){2,}\s*$/.test(lines[index])
      && !/^```/.test(lines[index])
      && !/^>\s?/.test(lines[index])
      && !/^(\d+\.\s+|[-*]\s+)/.test(lines[index])
    ) {
      paragraphLines.push(lines[index].trim());
      index++;
    }
    blocks.push({ type: "paragraph", text: paragraphLines.join(" ") });
  }

  return blocks;
}

export function renderMarkdownInline(text: string) {
  const nodes: React.ReactNode[] = [];
  const pattern = /(\*\*[\s\S]+?\*\*|`[^`]+`|\[[^\]]+\]\([^)]+\))/g;
  let cursor = 0;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > cursor) {
      nodes.push(text.slice(cursor, match.index));
    }
    const token = match[0];
    if (token.startsWith("**")) {
      nodes.push(<strong key={`${token}-${match.index}`}>{token.slice(2, -2)}</strong>);
    } else if (token.startsWith("`")) {
      nodes.push(<code key={`${token}-${match.index}`}>{token.slice(1, -1)}</code>);
    } else {
      const link = token.match(/^\[([^\]]+)\]\(([^)]+)\)$/);
      if (link) {
        nodes.push(
          <Link key={`${token}-${match.index}`} href={link[2]} className="text-link">
            {link[1]}
          </Link>,
        );
      }
    }
    cursor = match.index + token.length;
  }
  if (cursor < text.length) {
    nodes.push(text.slice(cursor));
  }
  return nodes.length > 0 ? nodes : text;
}

function normalizeMarkdown(content: string) {
  return (content || "").replaceAll("\\n", "\n").replace(/\r/g, "").trim();
}
