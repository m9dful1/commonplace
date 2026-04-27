"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { MarginaliaBlock } from "./MarginaliaBlock";

const ERROR_SENTINEL = "\n\n__error__:";

type State =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "streaming"; text: string }
  | { kind: "error"; message: string; partial?: string };

export function MarginaliaRequester({
  fragmentId,
  hasExisting,
  hasApiKey,
}: {
  fragmentId: string;
  hasExisting: boolean;
  hasApiKey: boolean;
}) {
  const router = useRouter();
  const [state, setState] = useState<State>({ kind: "idle" });

  async function request() {
    setState({ kind: "loading" });
    try {
      const res = await fetch(`/api/fragments/${fragmentId}/marginalia`, {
        method: "POST",
      });

      if (res.status === 412) {
        setState({
          kind: "error",
          message:
            "no API key set — visit Settings to add one.",
        });
        return;
      }
      if (!res.ok || !res.body) {
        const errText = await res.text().catch(() => "");
        setState({
          kind: "error",
          message: errText || `request failed (${res.status}).`,
        });
        return;
      }

      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let acc = "";
      let started = false;
      let errored = false;

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        acc += decoder.decode(value, { stream: true });

        const errIdx = acc.indexOf(ERROR_SENTINEL);
        if (errIdx >= 0) {
          const partial = acc.slice(0, errIdx).trim();
          const message = acc.slice(errIdx + ERROR_SENTINEL.length).trim() || "stream interrupted.";
          setState({ kind: "error", message, partial: partial || undefined });
          errored = true;
          break;
        }

        if (!started) {
          started = true;
        }
        setState({ kind: "streaming", text: acc });
      }

      if (errored) return;

      // Refresh server-rendered list so the new marginalia replaces our streamed copy.
      setState({ kind: "idle" });
      router.refresh();
    } catch (err) {
      setState({
        kind: "error",
        message: err instanceof Error ? err.message : "unknown error.",
      });
    }
  }

  if (state.kind === "loading") {
    return <PulsingDots />;
  }
  if (state.kind === "streaming") {
    return <MarginaliaBlock body={state.text} />;
  }
  if (state.kind === "error") {
    return (
      <div className="space-y-3">
        {state.partial && <MarginaliaBlock body={state.partial} />}
        <p className="ml-6 font-serif text-sm italic text-muted">
          couldn&rsquo;t reach Claude — {state.message}
        </p>
        <button
          type="button"
          onClick={request}
          className="ml-6 font-mono text-xs uppercase tracking-widest text-accent hover:text-ink"
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
        <a href="/settings" className="text-accent underline-offset-4 hover:underline">
          Settings
        </a>{" "}
        to request a marginal note.
      </p>
    );
  }

  return (
    <button
      type="button"
      onClick={request}
      className="font-mono text-xs uppercase tracking-widest text-accent hover:text-ink"
    >
      {hasExisting ? "another reading" : "request a marginal note"}
    </button>
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
