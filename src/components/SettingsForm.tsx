"use client";

import { useState, useTransition } from "react";
import { saveAnthropicKeyAction } from "@/app/settings/actions";

type TestResult =
  | { state: "idle" }
  | { state: "testing" }
  | { state: "ok"; model: string }
  | { state: "err"; error: string };

export function SettingsForm({
  initialMaskedKey,
  hasKey,
}: {
  initialMaskedKey: string;
  hasKey: boolean;
}) {
  const [key, setKey] = useState("");
  const [show, setShow] = useState(false);
  const [test, setTest] = useState<TestResult>({ state: "idle" });
  const [isPending, startTransition] = useTransition();
  const [savedFlash, setSavedFlash] = useState(false);

  async function runTest() {
    const candidate = key.trim();
    if (!candidate) return;
    setTest({ state: "testing" });
    try {
      const res = await fetch("/api/settings/test", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ key: candidate }),
      });
      const data = (await res.json()) as
        | { ok: true; model: string }
        | { ok: false; error: string };
      if (data.ok) setTest({ state: "ok", model: data.model });
      else setTest({ state: "err", error: data.error });
    } catch (err) {
      setTest({
        state: "err",
        error: err instanceof Error ? err.message : "Network error.",
      });
    }
  }

  function save(formData: FormData) {
    startTransition(async () => {
      await saveAnthropicKeyAction(formData);
      setKey("");
      setSavedFlash(true);
      setTimeout(() => setSavedFlash(false), 2000);
    });
  }

  return (
    <form action={save} className="space-y-6">
      <div>
        <label
          htmlFor="key"
          className="block font-mono text-xs uppercase tracking-widest text-muted"
        >
          Anthropic API key
        </label>
        {hasKey && (
          <p className="mt-1 font-mono text-xs text-muted">
            current: {initialMaskedKey}
          </p>
        )}
        <div className="mt-2 flex items-baseline gap-3 border-b border-rule pb-1">
          <input
            id="key"
            name="key"
            type={show ? "text" : "password"}
            autoComplete="off"
            spellCheck={false}
            placeholder={hasKey ? "enter a new key to replace" : "sk-ant-…"}
            value={key}
            onChange={(e) => setKey(e.target.value)}
            className="flex-1 font-mono text-sm tracking-wide text-ink placeholder:text-muted/70"
          />
          <button
            type="button"
            onClick={() => setShow((v) => !v)}
            className="font-mono text-xs uppercase tracking-widest text-muted hover:text-ink"
          >
            {show ? "hide" : "show"}
          </button>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-x-6 gap-y-3 font-mono text-xs uppercase tracking-widest">
        <button
          type="button"
          onClick={runTest}
          disabled={!key.trim() || test.state === "testing"}
          className="text-accent hover:text-ink disabled:opacity-40"
        >
          {test.state === "testing" ? "testing…" : "test key"}
        </button>
        <button
          type="submit"
          disabled={isPending || !key.trim()}
          className="text-accent hover:text-ink disabled:opacity-40"
        >
          {isPending ? "saving…" : "save"}
        </button>
        {savedFlash && (
          <span className="normal-case tracking-normal text-muted">saved.</span>
        )}
      </div>

      {test.state === "ok" && (
        <p className="font-serif text-sm text-accent">
          ✓ key is valid · <span className="font-mono text-xs">{test.model}</span>
        </p>
      )}
      {test.state === "err" && (
        <p className="font-serif text-sm italic text-muted">
          couldn&rsquo;t reach Claude — {test.error}
        </p>
      )}
    </form>
  );
}
