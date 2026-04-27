"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { deleteLetterAction } from "@/app/letters/actions";
import type { Letter } from "@/lib/letters";

type ComposeState =
  | { kind: "idle" }
  | { kind: "writing" }
  | { kind: "error"; message: string };

export function LettersView({
  letters,
  hasApiKey,
}: {
  letters: Letter[];
  hasApiKey: boolean;
}) {
  const router = useRouter();
  const [compose, setCompose] = useState<ComposeState>({ kind: "idle" });

  async function generate() {
    setCompose({ kind: "writing" });
    try {
      const res = await fetch("/api/letters/generate", { method: "POST" });
      if (res.status === 412) {
        setCompose({
          kind: "error",
          message: "no API key set — visit Settings to add one.",
        });
        return;
      }
      if (!res.ok) {
        const data = (await res.json().catch(() => ({}))) as {
          error?: string;
        };
        setCompose({
          kind: "error",
          message: data.error ?? `request failed (${res.status}).`,
        });
        return;
      }
      setCompose({ kind: "idle" });
      router.refresh();
    } catch (err) {
      setCompose({
        kind: "error",
        message: err instanceof Error ? err.message : "unknown error.",
      });
    }
  }

  return (
    <div className="space-y-12">
      <ComposeAffordance
        state={compose}
        hasApiKey={hasApiKey}
        onCompose={generate}
      />

      {letters.length === 0 ? (
        compose.kind === "idle" || compose.kind === "error" ? (
          <p className="font-serif italic text-muted">
            No Letters yet. The first one is a click away.
          </p>
        ) : null
      ) : (
        <ol className="space-y-16">
          {letters.map((l) => (
            <li key={l.id}>
              <LetterRow letter={l} />
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

function ComposeAffordance({
  state,
  hasApiKey,
  onCompose,
}: {
  state: ComposeState;
  hasApiKey: boolean;
  onCompose: () => void;
}) {
  if (state.kind === "writing") {
    return (
      <p className="font-serif text-base italic text-muted" role="status">
        Claude is writing&hellip;
      </p>
    );
  }
  if (state.kind === "error") {
    return (
      <div className="space-y-2">
        <p className="font-serif text-sm italic text-muted">
          couldn&rsquo;t reach Claude — {state.message}
        </p>
        <button
          type="button"
          onClick={onCompose}
          className="font-mono text-xs uppercase tracking-widest text-accent hover:text-ink"
        >
          try again
        </button>
      </div>
    );
  }
  if (!hasApiKey) {
    return (
      <p className="font-serif text-sm italic text-muted">
        Add an Anthropic API key in{" "}
        <a
          href="/settings"
          className="text-accent underline-offset-4 hover:underline"
        >
          Settings
        </a>{" "}
        to compose a Letter.
      </p>
    );
  }
  return (
    <button
      type="button"
      onClick={onCompose}
      className="font-mono text-xs uppercase tracking-widest text-accent hover:text-ink"
    >
      compose a letter
    </button>
  );
}

function LetterRow({ letter }: { letter: Letter }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();

  function remove() {
    if (
      typeof window !== "undefined" &&
      !window.confirm("Delete this Letter? This can't be undone.")
    )
      return;
    startTransition(async () => {
      await deleteLetterAction(letter.id);
      router.refresh();
    });
  }

  const date = letter.createdAt.slice(0, 10);

  return (
    <div className="group space-y-4">
      <div className="flex items-baseline gap-x-3 font-mono text-xs uppercase tracking-widest text-muted">
        <time dateTime={letter.createdAt}>{date}</time>
        <span className="ml-auto opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
          <button
            type="button"
            onClick={remove}
            disabled={isPending}
            className="hover:text-ink disabled:opacity-40"
          >
            delete
          </button>
        </span>
      </div>
      <div className="space-y-4 font-serif text-base leading-relaxed text-ink">
        {letter.body
          .trim()
          .split(/\n{2,}/)
          .map((para, i) => (
            <p key={i} className="whitespace-pre-wrap">
              {para}
            </p>
          ))}
      </div>
    </div>
  );
}
