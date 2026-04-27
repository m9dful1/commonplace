import { NextResponse } from "next/server";
import { anthropicClient, MissingApiKeyError } from "@/lib/anthropic";
import { getFragment } from "@/lib/fragments";
import {
  createMarginalia,
  listMarginaliaForFragment,
  listRecentFragmentsForContext,
} from "@/lib/marginalia";
import {
  MARGINALIA_SYSTEM_PROMPT,
  buildMarginaliaUserMessage,
} from "@/lib/marginaliaPrompt";
import { MARGINALIA_MAX_TOKENS, MARGINALIA_MODEL } from "@/lib/models";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(
  _req: Request,
  { params }: { params: { id: string } }
) {
  const fragment = getFragment(params.id);
  if (!fragment) {
    return NextResponse.json({ error: "Fragment not found." }, { status: 404 });
  }

  let client;
  try {
    client = anthropicClient();
  } catch (err) {
    if (err instanceof MissingApiKeyError) {
      return NextResponse.json(
        { error: "Anthropic API key is not configured." },
        { status: 412 }
      );
    }
    throw err;
  }

  const priorMarginalia = listMarginaliaForFragment(fragment.id);
  const recentFragments = listRecentFragmentsForContext(fragment.id, 20);

  const userMessage = buildMarginaliaUserMessage({
    fragment,
    recentFragments,
    priorMarginalia,
  });

  const stream = new ReadableStream<Uint8Array>({
    async start(controller) {
      const encoder = new TextEncoder();
      let collected = "";
      try {
        const response = await client.messages.stream({
          model: MARGINALIA_MODEL,
          max_tokens: MARGINALIA_MAX_TOKENS,
          system: MARGINALIA_SYSTEM_PROMPT,
          messages: [{ role: "user", content: userMessage }],
        });

        for await (const event of response) {
          if (
            event.type === "content_block_delta" &&
            event.delta.type === "text_delta"
          ) {
            const chunk = event.delta.text;
            collected += chunk;
            controller.enqueue(encoder.encode(chunk));
          }
        }

        const trimmed = collected.trim();
        if (trimmed) {
          createMarginalia({ fragmentId: fragment.id, body: trimmed });
        }
        controller.close();
      } catch (err) {
        const message = err instanceof Error ? err.message : "Unknown error.";
        controller.enqueue(
          encoder.encode(`\n\n__error__:${message}`)
        );
        controller.close();
      }
    },
  });

  return new Response(stream, {
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "no-store, no-transform",
      "X-Accel-Buffering": "no",
    },
  });
}
