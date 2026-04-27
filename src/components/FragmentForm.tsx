"use client";

import { useRef, useState, useTransition } from "react";
import { addFragmentAction } from "@/app/actions";

export function FragmentForm() {
  const formRef = useRef<HTMLFormElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const [showMeta, setShowMeta] = useState(false);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(formData: FormData) {
    const body = String(formData.get("body") ?? "").trim();
    if (!body) return;
    startTransition(async () => {
      await addFragmentAction(formData);
      formRef.current?.reset();
      setShowMeta(false);
      textareaRef.current?.focus();
    });
  }

  return (
    <form
      ref={formRef}
      action={handleSubmit}
      className="mb-12 border-b border-rule pb-8"
    >
      <textarea
        ref={textareaRef}
        name="body"
        required
        rows={3}
        placeholder="A fragment…"
        className="w-full resize-none border-none bg-transparent font-serif text-lg leading-relaxed placeholder:text-muted/70"
        onKeyDown={(e) => {
          if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
            e.preventDefault();
            formRef.current?.requestSubmit();
          }
        }}
      />

      {showMeta && (
        <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
          <input
            name="source"
            type="text"
            placeholder="source (optional)"
            className="border-b border-rule pb-1 font-mono text-xs tracking-wide text-ink placeholder:text-muted/70"
          />
          <input
            name="tags"
            type="text"
            placeholder="tags, comma separated"
            className="border-b border-rule pb-1 font-mono text-xs tracking-wide text-ink placeholder:text-muted/70"
          />
        </div>
      )}

      <div className="mt-4 flex items-center justify-between font-mono text-xs uppercase tracking-widest text-muted">
        <button
          type="button"
          onClick={() => setShowMeta((v) => !v)}
          className="hover:text-ink"
        >
          {showMeta ? "− metadata" : "+ source / tags"}
        </button>
        <button
          type="submit"
          disabled={isPending}
          className="text-accent hover:text-ink disabled:opacity-40"
        >
          {isPending ? "saving…" : "keep ⌘↵"}
        </button>
      </div>
    </form>
  );
}
