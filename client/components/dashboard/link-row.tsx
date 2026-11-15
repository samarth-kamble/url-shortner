"use client";

import { useState } from "react";
import { LinkItem } from "@/lib/link-schemas";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Copy,
  Check,
  MoreVertical,
  ExternalLink,
  QrCode,
  Trash2,
  MousePointerClick,
  Info,
} from "lucide-react";
import { toast } from "sonner";

interface LinkRowProps {
  link: LinkItem;
  onOpenDetails: (link: LinkItem) => void;
  onDeleteLink: (shortCode: string) => void;
  onOpenQr: (link: LinkItem) => void;
}

export function LinkRow({
  link,
  onOpenDetails,
  onDeleteLink,
  onOpenQr,
}: LinkRowProps) {
  const [copied, setCopied] = useState(false);

  // Extract initial letter for avatar tile
  let initial = link.shortCode.charAt(0).toUpperCase();
  let domain = "";
  try {
    const url = new URL(link.longUrl);
    domain = url.hostname.replace("www.", "");
    initial = domain.charAt(0).toUpperCase();
  } catch {
    // fallback
  }

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard?.writeText(link.shortUrl);
    setCopied(true);
    toast.success("Short link copied to clipboard");
    setTimeout(() => setCopied(false), 2000);
  };

  const formattedDate = new Date(link.createdAt).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });

  return (
    <TooltipProvider delay={200}>
      <div
        onClick={() => onOpenDetails(link)}
        className="group relative flex flex-col gap-3 rounded-xl border border-border/70 bg-card/80 p-4 transition-all hover:border-border hover:bg-card hover:shadow-sm sm:flex-row sm:items-center sm:justify-between cursor-pointer"
      >
        {/* Left: Avatar tile & Link text */}
        <div className="flex items-start sm:items-center gap-3.5 min-w-0 flex-1">
          {/* Domain Avatar Initial Tile */}
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary font-heading font-bold text-sm border border-primary/20 transition-transform group-hover:scale-105 select-none">
            {initial}
          </div>

          <div className="min-w-0 flex-1 space-y-1">
            {/* Short Link & Inline Copy */}
            <div className="flex items-center gap-2">
              <span className="font-mono text-sm font-semibold tracking-tight text-foreground group-hover:text-primary transition-colors">
                /{link.shortCode}
              </span>

              {/* Inline Copy Button */}
              <button
                type="button"
                onClick={handleCopy}
                className="inline-flex h-6 w-6 items-center justify-center rounded text-muted-foreground hover:bg-muted hover:text-foreground transition-colors cursor-pointer"
                title="Copy short link"
              >
                {copied ? (
                  <Check className="h-3.5 w-3.5 text-emerald-500 animate-in zoom-in-75" />
                ) : (
                  <Copy className="h-3.5 w-3.5" />
                )}
              </button>

              {/* Status Badge */}
              <Badge
                variant={link.isActive ? "default" : "secondary"}
                className={`text-[10px] font-mono px-2 py-0 h-4.5 ${
                  link.isActive
                    ? "bg-emerald-500/15 text-emerald-500 border-emerald-500/20"
                    : "text-muted-foreground"
                }`}
              >
                {link.isActive ? "Active" : "Expired"}
              </Badge>

              {link.isCustom && (
                <Badge variant="outline" className="text-[10px] font-mono px-1.5 py-0 h-4.5 text-muted-foreground">
                  Custom
                </Badge>
              )}
            </div>

            {/* Destination URL Truncated with Full Tooltip */}
            <Tooltip>
              <TooltipTrigger className="cursor-default text-left max-w-sm sm:max-w-md md:max-w-lg truncate block">
                <span className="font-mono text-xs text-muted-foreground truncate block">
                  {link.longUrl}
                </span>
              </TooltipTrigger>
              <TooltipContent side="bottom" className="font-mono text-xs max-w-md break-all">
                {link.longUrl}
              </TooltipContent>
            </Tooltip>
          </div>
        </div>

        {/* Right: Metrics, Date & Menu */}
        <div className="flex items-center justify-between sm:justify-end gap-3 sm:gap-5 border-t border-border/40 pt-2 sm:border-t-0 sm:pt-0">
          {/* Click Count Badge */}
          <div className="flex items-center gap-1.5 font-mono text-xs text-muted-foreground bg-muted/40 px-2.5 py-1 rounded-md">
            <MousePointerClick className="h-3.5 w-3.5 text-primary" />
            <span className="font-semibold text-foreground">{link.clickCount.toLocaleString()}</span>
            <span className="text-[10px] text-muted-foreground">clicks</span>
          </div>

          {/* Date */}
          <span className="font-mono text-[11px] text-muted-foreground/80 shrink-0">
            {formattedDate}
          </span>

          {/* Quick Row Action Menu */}
          <DropdownMenu>
            <DropdownMenuTrigger
              onClick={(e) => e.stopPropagation()}
              className="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground transition-colors cursor-pointer"
            >
              <MoreVertical className="h-4 w-4" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="!w-48 w-48 min-w-48 p-1.5 shadow-xl border-border/80 bg-popover rounded-xl">
              <DropdownMenuItem
                onClick={handleCopy}
                className="cursor-pointer gap-2.5 text-xs font-medium whitespace-nowrap py-1.5 px-2"
              >
                <Copy className="h-4 w-4 shrink-0 text-muted-foreground" />
                <span>Copy link</span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={(e) => {
                  e.stopPropagation();
                  window.open(link.shortUrl, "_blank");
                }}
                className="cursor-pointer gap-2.5 text-xs font-medium whitespace-nowrap py-1.5 px-2"
              >
                <ExternalLink className="h-4 w-4 shrink-0 text-muted-foreground" />
                <span>Open in new tab</span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={(e) => {
                  e.stopPropagation();
                  onOpenQr(link);
                }}
                className="cursor-pointer gap-2.5 text-xs font-medium whitespace-nowrap py-1.5 px-2"
              >
                <QrCode className="h-4 w-4 shrink-0 text-muted-foreground" />
                <span>View QR code</span>
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={(e) => {
                  e.stopPropagation();
                  onOpenDetails(link);
                }}
                className="cursor-pointer gap-2.5 text-xs font-medium whitespace-nowrap py-1.5 px-2"
              >
                <Info className="h-4 w-4 shrink-0 text-muted-foreground" />
                <span>View details</span>
              </DropdownMenuItem>
              <DropdownMenuSeparator className="my-1" />
              <DropdownMenuItem
                onClick={(e) => {
                  e.stopPropagation();
                  onDeleteLink(link.shortCode);
                }}
                className="cursor-pointer gap-2.5 text-xs font-medium whitespace-nowrap py-1.5 px-2 text-destructive focus:text-destructive"
              >
                <Trash2 className="h-4 w-4 shrink-0" />
                <span>Delete link</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
    </TooltipProvider>
  );
}
