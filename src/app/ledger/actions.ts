"use server";

import { revalidatePath } from "next/cache";
import {
  deleteLedgerEntry,
  updateLedgerEntry,
  writeLedgerMirror,
} from "@/lib/ledger";

export async function updateLedgerEntryAction(
  id: string,
  body: string
): Promise<void> {
  const trimmed = body.trim();
  if (!trimmed) return;
  updateLedgerEntry(id, trimmed);
  writeLedgerMirror();
  revalidatePath("/ledger");
}

export async function deleteLedgerEntryAction(id: string): Promise<void> {
  deleteLedgerEntry(id);
  writeLedgerMirror();
  revalidatePath("/ledger");
}
