import { LettersView } from "@/components/LettersView";
import { listLetters } from "@/lib/letters";
import { getAnthropicKey } from "@/lib/settings";

export const dynamic = "force-dynamic";

export default function LettersPage() {
  const letters = listLetters();
  const hasApiKey = Boolean(getAnthropicKey());
  return (
    <article className="space-y-12">
      <p className="font-mono text-xs lowercase tracking-widest text-muted">
        letters from Claude — written on request, kept whole
      </p>
      <LettersView letters={letters} hasApiKey={hasApiKey} />
    </article>
  );
}
