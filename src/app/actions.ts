"use server";

import { revalidatePath } from "next/cache";
import { createFragment, parseTagsInput } from "@/lib/fragments";
import { countFragmentsSinceLastLedger } from "@/lib/ledger";
import { generateLedgerEntry } from "@/lib/ledgerGenerate";
import { getAnthropicKey } from "@/lib/settings";

const LEDGER_TRIGGER_THRESHOLD = 5;

export async function addFragmentAction(formData: FormData) {
  const body = String(formData.get("body") ?? "").trim();
  if (!body) return;

  const source = String(formData.get("source") ?? "").trim() || null;
  const tags = parseTagsInput(String(formData.get("tags") ?? ""));

  createFragment({ body, source, tags });
  revalidatePath("/");

  // After every Nth fragment since the last Ledger entry, ask Claude to look.
  // Fire-and-forget — we don't make the user wait, and a missed run is fine
  // (the manual "update the ledger" button on /ledger is the recovery path).
  if (
    getAnthropicKey() &&
    countFragmentsSinceLastLedger() >= LEDGER_TRIGGER_THRESHOLD
  ) {
    void generateLedgerEntry()
      .then((result) => {
        if (result.status === "created") revalidatePath("/ledger");
      })
      .catch((err) => {
        console.error("[ledger autotrigger]", err);
      });
  }
}
