import { NextResponse } from "next/server";
import Anthropic from "@anthropic-ai/sdk";
import { MARGINALIA_MODEL } from "@/lib/models";

export const runtime = "nodejs";

export async function POST(req: Request) {
  let body: { key?: string };
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ ok: false, error: "Invalid request body." }, { status: 400 });
  }

  const apiKey = body.key?.trim();
  if (!apiKey) {
    return NextResponse.json({ ok: false, error: "Missing key." }, { status: 400 });
  }

  try {
    const client = new Anthropic({ apiKey });
    const result = await client.messages.create({
      model: MARGINALIA_MODEL,
      max_tokens: 1,
      messages: [{ role: "user", content: "ok" }],
    });
    return NextResponse.json({ ok: true, model: result.model });
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unknown error.";
    return NextResponse.json({ ok: false, error: message }, { status: 200 });
  }
}
