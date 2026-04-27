import { db } from "./db";

const ANTHROPIC_KEY = "anthropic_api_key";

export function getAnthropicKey(): string | null {
  const row = db()
    .prepare("SELECT value FROM settings WHERE key = ?")
    .get(ANTHROPIC_KEY) as { value: string } | undefined;
  const fromDb = row?.value?.trim();
  if (fromDb) return fromDb;

  const fromEnv = process.env.ANTHROPIC_API_KEY?.trim();
  return fromEnv || null;
}

export function setAnthropicKey(key: string): void {
  const trimmed = key.trim();
  if (!trimmed) {
    db().prepare("DELETE FROM settings WHERE key = ?").run(ANTHROPIC_KEY);
    return;
  }
  db()
    .prepare(
      `INSERT INTO settings (key, value) VALUES (?, ?)
       ON CONFLICT(key) DO UPDATE SET value = excluded.value`
    )
    .run(ANTHROPIC_KEY, trimmed);
}

export function maskKey(key: string | null): string {
  if (!key) return "";
  if (key.length <= 8) return "•".repeat(key.length);
  return `${key.slice(0, 4)}${"•".repeat(key.length - 8)}${key.slice(-4)}`;
}
