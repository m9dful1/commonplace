// Minimal markdown for marginalia text. Handles:
//   *italic* / _italic_   → <em>
//   **bold** / __bold__   → <strong>
//   --                    → em-dash
// Everything else is passed through as escaped text.
//
// This runs on the rendered output, not the raw stream, so no streaming-
// safety concerns. Rules are deliberately conservative — anything fancier
// than this and the marginalia is misbehaving and should be the prompt's
// problem, not the renderer's.

import { Fragment, type ReactNode, createElement } from "react";

type Tag = "strong" | "em";

type Run =
  | { kind: "text"; value: string }
  | { kind: "tag"; tag: Tag; children: Run[] };

function tokenize(input: string): Run[] {
  // Order: bold first (to avoid * eating ** boundaries), then italic.
  // We process the input recursively against a series of regex patterns.
  const patterns: Array<[Tag, RegExp]> = [
    ["strong", /\*\*([^*\n]+?)\*\*|__([^_\n]+?)__/],
    ["em", /\*([^*\n]+?)\*|_([^_\n]+?)_/],
  ];
  return parse(input, 0);

  function parse(text: string, depth: number): Run[] {
    if (depth > patterns.length - 1 || !text) {
      return text ? [{ kind: "text", value: text }] : [];
    }
    const [tag, re] = patterns[depth];
    const out: Run[] = [];
    let cursor = 0;
    while (cursor < text.length) {
      const slice = text.slice(cursor);
      const m = slice.match(re);
      if (!m) {
        out.push(...parse(text.slice(cursor), depth + 1));
        break;
      }
      const matchStart = cursor + (m.index ?? 0);
      const inner = m[1] ?? m[2] ?? "";
      if (matchStart > cursor) {
        out.push(...parse(text.slice(cursor, matchStart), depth + 1));
      }
      out.push({ kind: "tag", tag, children: parse(inner, depth + 1) });
      cursor = matchStart + m[0].length;
    }
    return out;
  }
}

export function renderLightMarkdown(input: string): ReactNode {
  if (!input) return null;
  // Em-dashes: convert -- to — outside any code-like context. We don't have
  // code in marginalia, so just do it on the whole string. Single-pass.
  const dashed = input.replace(/--/g, "—");
  const runs = tokenize(dashed);
  return renderRuns(runs);
}

function renderRuns(runs: Run[]): ReactNode {
  return runs.map((run, i) => {
    if (run.kind === "text") return createElement(Fragment, { key: i }, run.value);
    return createElement(run.tag, { key: i }, renderRuns(run.children));
  });
}

