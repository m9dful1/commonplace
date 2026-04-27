import Anthropic from "@anthropic-ai/sdk";
import { getAnthropicKey } from "./settings";

export class MissingApiKeyError extends Error {
  constructor() {
    super("Anthropic API key is not configured.");
    this.name = "MissingApiKeyError";
  }
}

export function anthropicClient(): Anthropic {
  const apiKey = getAnthropicKey();
  if (!apiKey) throw new MissingApiKeyError();
  return new Anthropic({ apiKey });
}
