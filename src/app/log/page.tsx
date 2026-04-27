import { promises as fs } from "node:fs";
import { join } from "node:path";
import { renderLogMarkdown } from "@/lib/markdownToReact";

export const dynamic = "force-dynamic";

// In dev (`npm run dev`) cwd is the project root, which is what we want.
// If you ever run `next start` from a different directory this needs to be
// resolved differently — but for a local-first single-user app that's an
// edge case worth not solving until it bites.
const LOG_PATH = join(process.cwd(), "CLAUDE_LOG.md");

export default async function LogPage() {
  let source: string;
  try {
    source = await fs.readFile(LOG_PATH, "utf8");
  } catch (err) {
    return (
      <div className="space-y-4">
        <p className="font-serif italic text-muted">
          CLAUDE_LOG.md is not where it should be.
        </p>
        <p className="font-mono text-xs text-muted">
          looked in: {LOG_PATH} —{" "}
          {err instanceof Error ? err.message : "unknown error."}
        </p>
      </div>
    );
  }

  return (
    <article>
      <p className="mb-12 font-mono text-xs lowercase tracking-widest text-muted">
        a document maintained across Claude instances working on Commonplace
      </p>
      <div className="space-y-4">{renderLogMarkdown(source)}</div>
    </article>
  );
}
