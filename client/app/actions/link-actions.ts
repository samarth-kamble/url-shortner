"use server";

import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";
import { createLinkSchema, CreateLinkFormData, LinkItem } from "@/lib/link-schemas";
import { getLinksApi, createLinkApi, deleteLinkApi } from "@/lib/api/links";

const ACCESS_COOKIE = "shortlink_access_token";

export async function getMyLinksAction(): Promise<LinkItem[]> {
  const cookieStore = await cookies();
  const token = cookieStore.get(ACCESS_COOKIE)?.value;
  return await getLinksApi(token);
}

export async function createLinkAction(
  data: CreateLinkFormData
): Promise<{ success: boolean; link?: LinkItem; error?: string }> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get(ACCESS_COOKIE)?.value;

    if (!token) {
      return {
        success: false,
        error: "Your session has expired. Please refresh or sign in again.",
      };
    }

    const validated = createLinkSchema.parse(data);
    const link = await createLinkApi(validated, token);
    revalidatePath("/dashboard");
    return { success: true, link };
  } catch (err: unknown) {
    const message =
      err instanceof Error ? err.message : "Failed to create short link";
    return { success: false, error: message };
  }
}

export async function deleteLinkAction(
  shortCode: string
): Promise<{ success: boolean; error?: string }> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get(ACCESS_COOKIE)?.value;

    await deleteLinkApi(shortCode, token);
    revalidatePath("/dashboard");
    return { success: true };
  } catch (err: unknown) {
    const message =
      err instanceof Error ? err.message : "Failed to delete link";
    return { success: false, error: message };
  }
}

