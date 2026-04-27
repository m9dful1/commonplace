"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import {
  deleteLedgerEntryAction,
  updateLedgerEntryAction,
} from "@/app/ledger/actions";
import type { LedgerEntry } from "@/lib/ledger";
import { renderLightMarkdown } from "@/lib/lightMarkdown";

type GenState =
  | { kind: "idle" }
  | { kind: "generating" }
  | { kind: "passed" }
  | { kind: "error"; message: string };

export function LedgerView({
  entries,
  hasApiKey,
}: {
  entries: LedgerEntry[];
  hasApiKey: boolean;
}) {
  const router = useRouter();
  const [genState, setGenState] = useState<GenState>({ kind: "idle" });

  async function generate() {
    setGenState({ kind: "generating" });
    try {
      const res = await fetch("/api/ledger/generate", { method: "POST" });
      if (res.status === 412) {
        setGenState({
          kind: "error",
          message: "no API key set — visit Settings to add one.",
        });
        return;
      }
      if (!res.ok) {
        const data = (await res.json().catch(() => ({}))) as {
          error?: string;
        };
        setGenState({
          kind: "error",
          message: data.error ?? `request failed (${res.status}).`,
        });
        return;
      }
      const data = (await res.json()) as
        | { status: "pass" }
        | { status: "created"; entry: LedgerEntry };
      if (data.status === "pass") {
        setGenState({ kind: "passed" });
        return;
      }
      setGenState({ kind: "idle" });
      router.refresh();
    } catch (err) {
      setGenState({
        kind: "error",
        message: err instanceof Error ? err.message : "unknown error.",
      });
    }
  }

  return (
    <div className="space-y-12">
      {entries.length === 0 ? (
        <p className="font-serif italic text-muted">
          The Ledger is empty. After five fragments, Claude may begin a first
          entry — or you can ask for one now.
        </p>
      ) : (
        <ol className="space-y-12">
          {entries.map((e) => (
            <li key={e.id}>
              <LedgerEntryRow entry={e} />
            </li>
          ))}
        </ol>
      )}

      <div className="space-y-3 border-t border-rule pt-6">
        <GenerateAffordance
          state={genState}
          hasApiKey={hasApiKey}
          onGenerate={generate}
          onDismiss={() => setGenState({ kind: "idle" })}
        />
      </div>
    </div>
  );
}

function GenerateAffordance({
  state,
  hasApiKey,
  onGenerate,
  onDismiss,
}: {
  state: GenState;
  hasApiKey: boolean;
  onGenerate: () => void;
  onDismiss: () => void;
}) {
  if (state.kind === "generating") {
    return <PulsingDots />;
  }
  if (state.kind === "passed") {
    return (
      <div className="space-y-2">
        <p className="font-serif text-sm italic text-muted">
          pass — nothing has changed enough to warrant a new entry.
        </p>
        <button
          type="button"
          onClick={onDismiss}
          className="font-mono text-xs uppercase tracking-widest text-muted hover:text-ink"
        >
          dismiss
        </button>
      </div>
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
          onClick={onGenerate}
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
        to update the Ledger.
      </p>
    );
  }
  return (
    <button
      type="button"
      onClick={onGenerate}
      className="font-mono text-xs uppercase tracking-widest text-accent hover:text-ink"
    >
      update the ledger
    </button>
  );
}

function LedgerEntryRow({ entry }: { entry: LedgerEntry }) {
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(entry.body);
  const [isPending, startTransition] = useTransition();

  function save() {
    const next = draft.trim();
    if (!next || next === entry.body.trim()) {
      setEditing(false);
      setDraft(entry.body);
      return;
    }
    startTransition(async () => {
      await updateLedgerEntryAction(entry.id, next);
      setEditing(false);
      router.refresh();
    });
  }

  function cancel() {
    setDraft(entry.body);
    setEditing(false);
  }

  function remove() {
    if (
      typeof window !== "undefined" &&
      !window.confirm("Delete this Ledger entry?")
    )
      return;
    startTransition(async () => {
      await deleteLedgerEntryAction(entry.id);
      router.refresh();
    });
  }

  const date = entry.createdAt.slice(0, 10);

  return (
    <div className="group space-y-3">
      <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1 font-mono text-xs uppercase tracking-widest text-muted">
        <time dateTime={entry.createdAt}>{date}</time>
        <span className="text-accent">{entry.author}</span>
        {!editing && (
          <span className="ml-auto flex gap-3 opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
            <button
              type="button"
              onClick={() => setEditing(true)}
              className="hover:text-ink"
            >
              edit
            </button>
            <button
              type="button"
              onClick={remove}
              disabled={isPending}
              className="hover:text-ink disabled:opacity-40"
            >
              delete
            </button>
          </span>
        )}
      </div>
      {editing ? (
        <textarea
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={save}
          onKeyDown={(e) => {
            if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) {
              e.preventDefault();
              save();
            } else if (e.key === "Escape") {
              e.preventDefault();
              cancel();
            }
          }}
          rows={Math.max(3, Math.ceil(draft.length / 70))}
          autoFocus
          disabled={isPending}
          className="block w-full resize-none border-b border-rule bg-transparent font-serif text-base leading-relaxed text-ink focus:border-accent focus:outline-none disabled:opacity-50"
        />
      ) : (
        <p className="whitespace-pre-wrap font-serif text-base leading-relaxed text-ink">
          {renderLightMarkdown(entry.body)}
        </p>
      )}
    </div>
  );
}

function PulsingDots() {
  return (
    <div
      className="flex items-center gap-1.5"
      role="status"
      aria-label="Claude is writing"
    >
      <Dot delay="0ms" />
      <Dot delay="180ms" />
      <Dot delay="360ms" />
    </div>
  );
}

function Dot({ delay }: { delay: string }) {
  return (
    <span
      className="inline-block h-1.5 w-1.5 rounded-full bg-accent"
      style={{
        animationName: "cp-pulse",
        animationDuration: "1.4s",
        animationIterationCount: "infinite",
        animationTimingFunction: "ease-in-out",
        animationDelay: delay,
      }}
    />
  );
}
