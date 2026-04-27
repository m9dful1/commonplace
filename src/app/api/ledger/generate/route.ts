import { NextResponse } from "next/server";
import { MissingApiKeyError } from "@/lib/anthropic";
import { generateLedgerEntry } from "@/lib/ledgerGenerate";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST() {
  try {
    const result = await generateLedgerEntry();
    if (result.status === "pass") {
      return NextResponse.json({ status: "pass" });
    }
    return NextResponse.json({
      status: "created",
      entry: result.entry,
    });
  } catch (err) {
    if (err instanceof MissingApiKeyError) {
      return NextResponse.json(
        { error: "Anthropic API key is not configured." },
        { status: 412 }
      );
    }
    const message = err instanceof Error ? err.message : "Unknown error.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
