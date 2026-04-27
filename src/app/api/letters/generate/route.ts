import { NextResponse } from "next/server";
import { MissingApiKeyError } from "@/lib/anthropic";
import { generateLetter } from "@/lib/lettersGenerate";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

// Letters are slow Opus calls — give them more headroom than the default.
export const maxDuration = 120;

export async function POST() {
  try {
    const letter = await generateLetter();
    return NextResponse.json({ letter });
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
