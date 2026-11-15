import { z } from "zod";

export const createLinkSchema = z.object({
  longUrl: z
    .string()
    .min(1, "Destination URL is required")
    .max(2048, "URL cannot exceed 2048 characters")
    .refine(
      (url) => {
        try {
          const parsed = new URL(url);
          return parsed.protocol === "http:" || parsed.protocol === "https:";
        } catch {
          return false;
        }
      },
      { message: "Please enter a valid HTTP or HTTPS destination URL" }
    ),
  customAlias: z
    .string()
    .optional()
    .refine(
      (val) => {
        if (!val || val.trim() === "") return true;
        return /^[a-zA-Z0-9_-]{4,16}$/.test(val);
      },
      {
        message: "Alias must be 4–16 alphanumeric characters, hyphens, or underscores",
      }
    ),
  expiresAt: z.string().optional(),
});

export type CreateLinkFormData = z.infer<typeof createLinkSchema>;

export interface LinkItem {
  id?: string;
  shortCode: string;
  shortUrl: string;
  longUrl: string;
  isCustom: boolean;
  isActive: boolean;
  createdAt: string;
  expiresAt?: string | null;
  clickCount: number;
  isSampleData?: boolean;
}

