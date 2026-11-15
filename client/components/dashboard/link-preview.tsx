"use client";

import { Globe } from "lucide-react";
import { Badge } from "@/components/ui/badge";

interface LinkPreviewProps {
  longUrl: string;
  customAlias?: string;
  baseUrl?: string;
}

export function LinkPreview({
  longUrl,
  customAlias,
  baseUrl = "localhost:8080",
}: LinkPreviewProps) {
  // Extract hostname or initial
  let domain = "your-domain.com";
  let initial = "S";
  try {
    if (longUrl && (longUrl.startsWith("http://") || longUrl.startsWith("https://"))) {
      const parsed = new URL(longUrl);
      domain = parsed.hostname.replace("www.", "");
      initial = domain.charAt(0).toUpperCase();
    }
  } catch {
    // fallback
  }

  const cleanBase = baseUrl.replace(/^https?:\/\//, "");
  const displayCode = customAlias?.trim() ? customAlias.trim() : "••••••";

  return (
    <div className="rounded-xl border border-border/80 bg-muted/30 p-4 transition-all">
      <div className="flex items-center justify-between pb-3">
        <span className="font-mono text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
          Live Link Preview
        </span>
        <Badge
          variant="outline"
          className="border-primary/30 bg-primary/10 text-primary text-[10px] font-mono px-2 py-0.5"
        >
          Preview
        </Badge>
      </div>

      <div className="flex items-center gap-3">
        {/* Domain Avatar Initial Tile */}
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/15 text-primary font-heading font-bold text-base select-none border border-primary/20">
          {initial}
        </div>

        <div className="min-w-0 flex-1 space-y-1">
          {/* Formed Short Link */}
          <div className="flex items-center gap-1.5 font-mono text-sm font-semibold tracking-tight text-foreground">
            <span className="text-muted-foreground">{cleanBase}/</span>
            <span className="text-primary truncate">{displayCode}</span>
          </div>

          {/* Destination URL Preview */}
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground truncate">
            <Globe className="h-3 w-3 shrink-0" />
            <span className="truncate">{longUrl || "https://example.com/long-destination-url"}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
