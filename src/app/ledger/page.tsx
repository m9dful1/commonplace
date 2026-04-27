import { LedgerView } from "@/components/LedgerView";
import { listLedgerEntries } from "@/lib/ledger";
import { getAnthropicKey } from "@/lib/settings";

export const dynamic = "force-dynamic";

export default function LedgerPage() {
  const entries = listLedgerEntries();
  const hasApiKey = Boolean(getAnthropicKey());
  return (
    <article className="space-y-12">
      <p className="font-mono text-xs lowercase tracking-widest text-muted">
        what you&rsquo;ve been gathering — the collection&rsquo;s own journal
      </p>
      <LedgerView entries={entries} hasApiKey={hasApiKey} />
    </article>
  );
}
