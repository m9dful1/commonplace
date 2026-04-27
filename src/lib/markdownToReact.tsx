import { marked, type Tokens } from "marked";
import { Fragment, type ReactNode } from "react";

// Server-only renderer for CLAUDE_LOG.md. Walks marked's token AST and emits
// React directly, so we never touch dangerouslySetInnerHTML and the typography
// is fully under our control.

const HEADING_DATE_RE = /^(\d{4}-\d{2}-\d{2})\s+—\s+(.+)$/;

function renderInline(tokens: Tokens.Generic[] | undefined, keyBase: string): ReactNode {
  if (!tokens) return null;
  return tokens.map((tok, i) => renderToken(tok, `${keyBase}.${i}`));
}

function renderToken(token: Tokens.Generic, key: string): ReactNode {
  switch (token.type) {
    case "text": {
      const t = token as Tokens.Text;
      if (t.tokens && t.tokens.length) return renderInline(t.tokens, key);
      return <Fragment key={key}>{t.text}</Fragment>;
    }
    case "escape":
      return <Fragment key={key}>{(token as Tokens.Escape).text}</Fragment>;
    case "strong":
      return (
        <strong key={key} className="font-normal text-ink">
          {renderInline((token as Tokens.Strong).tokens, key)}
        </strong>
      );
    case "em":
      return (
        <em key={key}>{renderInline((token as Tokens.Em).tokens, key)}</em>
      );
    case "del":
      return (
        <s key={key}>{renderInline((token as Tokens.Del).tokens, key)}</s>
      );
    case "codespan":
      return (
        <code
          key={key}
          className="rounded bg-rule/40 px-1 py-px font-mono text-[0.85em] text-ink"
        >
          {(token as Tokens.Codespan).text}
        </code>
      );
    case "br":
      return <br key={key} />;
    case "link": {
      const l = token as Tokens.Link;
      return (
        <a
          key={key}
          href={l.href}
          className="text-accent underline-offset-4 hover:underline"
        >
          {renderInline(l.tokens, key)}
        </a>
      );
    }
    default: {
      // Inline fallthrough for unhandled token types — show their raw text.
      const maybeText = (token as unknown as { text?: unknown }).text;
      if (typeof maybeText === "string") {
        return <Fragment key={key}>{maybeText}</Fragment>;
      }
      return null;
    }
  }
}

function renderBlock(token: Tokens.Generic, key: string): ReactNode {
  switch (token.type) {
    case "heading": {
      const h = token as Tokens.Heading;
      if (h.depth === 1) return null; // skip the file's own H1 title
      if (h.depth === 3) {
        const m = h.text.match(HEADING_DATE_RE);
        if (m) {
          return (
            <h3
              key={key}
              className="mt-16 flex flex-wrap items-baseline gap-x-3 gap-y-1 font-serif text-base"
            >
              <span className="font-mono text-xs uppercase tracking-widest text-muted">
                {m[1]}
              </span>
              <span className="text-accent">{m[2]}</span>
            </h3>
          );
        }
        return (
          <h3 key={key} className="mt-12 font-serif text-base text-ink">
            {renderInline(h.tokens, key)}
          </h3>
        );
      }
      return (
        <h2 key={key} className="mt-12 font-serif text-lg text-ink">
          {renderInline(h.tokens, key)}
        </h2>
      );
    }
    case "paragraph":
      return (
        <p key={key} className="font-serif text-base leading-relaxed text-ink">
          {renderInline((token as Tokens.Paragraph).tokens, key)}
        </p>
      );
    case "blockquote": {
      const bq = token as Tokens.Blockquote;
      return (
        <blockquote
          key={key}
          className="border-l-2 border-accent/30 pl-4 font-serif italic text-accent/90"
        >
          {bq.tokens.map((t, i) => renderBlock(t, `${key}.${i}`))}
        </blockquote>
      );
    }
    case "list": {
      const list = token as Tokens.List;
      const Tag = list.ordered ? "ol" : "ul";
      return (
        <Tag
          key={key}
          className={
            list.ordered
              ? "list-decimal space-y-1 pl-6 font-serif text-base text-ink marker:font-mono marker:text-xs marker:text-muted"
              : "list-disc space-y-1 pl-6 font-serif text-base text-ink marker:text-muted"
          }
        >
          {list.items.map((item, i) => (
            <li key={`${key}.${i}`} className="leading-relaxed">
              {item.tokens.map((t, j) =>
                t.type === "text"
                  ? renderInline((t as Tokens.Text).tokens, `${key}.${i}.${j}`)
                  : renderBlock(t, `${key}.${i}.${j}`)
              )}
            </li>
          ))}
        </Tag>
      );
    }
    case "code": {
      const c = token as Tokens.Code;
      return (
        <pre
          key={key}
          className="overflow-x-auto rounded bg-rule/40 p-3 font-mono text-xs leading-relaxed text-ink"
        >
          <code>{c.text}</code>
        </pre>
      );
    }
    case "hr":
      // The design says: render the --- separator as generous whitespace,
      // not a literal hairline. The mt-16 on the next H3 carries most of the
      // gap; this just adds a touch more so it reads as a section break.
      return <div key={key} aria-hidden className="h-6" />;
    case "space":
      return null;
    default:
      return null;
  }
}

export function renderLogMarkdown(source: string): ReactNode {
  // Skip the file's preamble — everything up to and including the first ---
  // line. The page provides its own one-line tagline as design chrome.
  const split = source.split(/^---\s*$/m);
  const body = split.length > 1 ? split.slice(1).join("---") : source;
  const tokens = marked.lexer(body);
  return tokens.map((t, i) => renderBlock(t as Tokens.Generic, String(i)));
}
