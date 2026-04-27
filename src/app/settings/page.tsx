import { SettingsForm } from "@/components/SettingsForm";
import { getAnthropicKey, maskKey } from "@/lib/settings";

export const dynamic = "force-dynamic";

export default function SettingsPage() {
  const key = getAnthropicKey();
  const masked = maskKey(key);
  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-serif text-2xl">Settings</h1>
        <p className="mt-2 font-serif text-base text-muted">
          Your API key is stored locally in <span className="font-mono text-sm">~/.commonplace/db.sqlite</span>.
          Nothing leaves this machine except the calls to Claude themselves.
        </p>
      </div>
      <SettingsForm initialMaskedKey={masked} hasKey={Boolean(key)} />
    </div>
  );
}
