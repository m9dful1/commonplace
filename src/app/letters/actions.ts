"use server";

import { revalidatePath } from "next/cache";
import { deleteLetter, writeLettersMirror } from "@/lib/letters";

export async function deleteLetterAction(id: string): Promise<void> {
  deleteLetter(id);
  writeLettersMirror();
  revalidatePath("/letters");
}
