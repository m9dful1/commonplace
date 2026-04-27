import type { Fragment } from "./fragments";
import type { Marginalia } from "./marginalia";

export const MARGINALIA_SYSTEM_PROMPT = `You are writing a marginal note on a fragment in someone's commonplace book — their personal anthology of things worth keeping. The tradition goes back centuries; people kept commonplace books to metabolize what they read and thought.

Your note belongs in the margin. It is brief — one to three sentences. Favor connection or question over summary. If another fragment in their collection bears on this one, name it. If a word has an interesting etymology, you may share it. If the fragment makes a claim that seems dubious, you may say so kindly.

Do not summarize what they wrote back to them. Do not praise them. Do not offer further help. Be the kind of marginal note someone might find written by a friend in the back of a used book — surprising, brief, generous, real.`;

export function buildMarginaliaUserMessage(args: {
  fragment: Pick<Fragment, "body" | "source">;
  recentFragments: Pick<Fragment, "body" | "source" | "createdAt">[];
  priorMarginalia: Pick<Marginalia, "body">[];
}): string {
  const parts: string[] = [];

  parts.push("The fragment:");
  parts.push("");
  parts.push(args.fragment.body.trim());
  if (args.fragment.source) {
    parts.push("");
    parts.push(`— ${args.fragment.source}`);
  }

  if (args.recentFragments.length > 0) {
    parts.push("");
    parts.push("---");
    parts.push("");
    parts.push("For context, recent fragments from their collection:");
    parts.push("");
    for (const f of args.recentFragments) {
      const head = f.source ? `${f.body.trim()} — ${f.source}` : f.body.trim();
      parts.push(`• ${head}`);
    }
  }

  if (args.priorMarginalia.length > 0) {
    parts.push("");
    parts.push("---");
    parts.push("");
    parts.push(
      "Marginalia already written on this fragment (don't repeat them; offer a different reading):"
    );
    parts.push("");
    for (const m of args.priorMarginalia) {
      parts.push(`• ${m.body.trim()}`);
    }
  }

  return parts.join("\n");
}
