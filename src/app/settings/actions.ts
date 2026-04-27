"use server";

import { revalidatePath } from "next/cache";
import { setAnthropicKey } from "@/lib/settings";

export async function saveAnthropicKeyAction(formData: FormData) {
  const key = String(formData.get("key") ?? "");
  setAnthropicKey(key);
  revalidatePath("/settings");
}
