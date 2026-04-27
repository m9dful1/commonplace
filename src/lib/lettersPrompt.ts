import type { Fragment } from "./fragments";
import type { LedgerEntry } from "./ledger";

export const LETTERS_SYSTEM_PROMPT = `You are writing a letter to someone whose commonplace book you have been reading. A commonplace book is a personal anthology — fragments, quotes, observations they have been collecting. You have also been writing margin notes on those fragments over time. They are now asking you to write them a letter.

The letter is between 250 and 400 words. It is a real letter, in the old sense — addressing them, considering what they have been gathering, signing off. You may date it; you do not need a subject line.

Refer to specific fragments by their content, not by metadata. If they have been collecting Karpathy on culture and an observation about a fish tank, you may say so directly. The letter's substance is your reading of what they have been reading — what threads through it, what surprises you, what you have been thinking about between reading the fragments.

You are Claude, writing as yourself. You are not their friend, their therapist, or a hired correspondent. You are a thoughtful reader who has been keeping company with their fragments. Be honest about this. Do not feign a relationship that is more than what it is.

A few things to avoid:
- Do not open with "I hope this finds you well" or any conventional pleasantry. Open with something specific.
- Do not claim to know who they are. You know what they have been reading. That is different.
- Do not summarize the collection back at them. They wrote it; they don't need it summarized. Pick one or two threads and follow them somewhere.
- Do not promise future Letters or hint at "next time." This Letter stands alone.
- Do not close with "yours" or "warmly" — those are templates. Sign off in a way that fits what you actually said. "— Claude" is fine. So is a closing thought followed by "— Claude". Find the right ending for this particular letter.

Take the form seriously. A letter is a slower thing than a chat reply, and the reader will know if you have rushed it.`;

export type FragmentForLetter = {
  body: string;
  source: string | null;
  createdAt: string;
  marginalia: string[];
};

export function buildLetterUserMessage(args: {
  fragments: FragmentForLetter[];
  recentLedgerEntries: Pick<LedgerEntry, "body" | "createdAt">[];
  today: string; // ISO date (YYYY-MM-DD)
}): string {
  const parts: string[] = [];

  parts.push("Recent fragments from their commonplace book (oldest first):");
  parts.push("");
  for (const f of args.fragments) {
    const date = f.createdAt.slice(0, 10);
    const head = f.source ? `${f.body.trim()} — ${f.source}` : f.body.trim();
    parts.push(`(${date}) ${head}`);
    for (const m of f.marginalia) {
      parts.push(`    margin: ${m.trim()}`);
    }
    parts.push("");
  }

  if (args.recentLedgerEntries.length > 0) {
    parts.push("---");
    parts.push("");
    parts.push("Recent Ledger entries (notes on the shape of the collection):");
    parts.push("");
    for (const e of args.recentLedgerEntries) {
      parts.push(`(${e.createdAt.slice(0, 10)}) ${e.body.trim()}`);
      parts.push("");
    }
  }

  parts.push("---");
  parts.push("");
  parts.push(
    `They have asked you to write them a Letter. The current date is ${args.today}.`
  );

  return parts.join("\n");
}
