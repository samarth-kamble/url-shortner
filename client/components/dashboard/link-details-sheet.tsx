"use client";

import { useState } from "react";
import { LinkItem } from "@/lib/link-schemas";
import { deleteLinkAction } from "@/app/actions/link-actions";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { QrCodeBlock } from "@/components/dashboard/qr-code-block";
import { DeleteLinkDialog } from "@/components/dashboard/delete-link-dialog";
import {
  Link2,
  ExternalLink,
  Copy,
  Check,
  MousePointerClick,
  Trash2,
  Clock,
} from "lucide-react";
import { toast } from "sonner";

interface LinkDetailsSheetProps {
  link: LinkItem | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onLinkDeleted: (shortCode: string) => void;
}

export function LinkDetailsSheet({
  link,
  open,
  onOpenChange,
  onLinkDeleted,
}: LinkDetailsSheetProps) {
  const [copied, setCopied] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);

  if (!link) return null;

  const handleCopy = () => {
    navigator.clipboard?.writeText(link.shortUrl);
    setCopied(true);
    toast.success("Short link copied to clipboard");
    setTimeout(() => setCopied(false), 2000);
  };

  const handleConfirmDelete = async () => {
    setIsDeleting(true);
    try {
      const res = await deleteLinkAction(link.shortCode);
      if (!res.success) {
        throw new Error(res.error || "Failed to delete link");
      }
      setIsConfirmOpen(false);
      onOpenChange(false);
      onLinkDeleted(link.shortCode);
      toast.success("Link deleted successfully");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Deletion failed";
      toast.error("Error", { description: msg });
    } finally {
      setIsDeleting(false);
    }
  };

  const formattedCreated = new Date(link.createdAt).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="w-full sm:max-w-lg md:max-w-xl overflow-y-auto no-scrollbar p-6 space-y-6">
        <SheetHeader className="text-left space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary border border-primary/20">
                <Link2 className="h-4 w-4" />
              </div>
              <SheetTitle className="font-heading text-xl font-bold tracking-tight">
                /{link.shortCode}
              </SheetTitle>
            </div>
            <div className="flex items-center gap-1.5">
              <Badge
                variant={link.isActive ? "default" : "secondary"}
                className={link.isActive ? "bg-emerald-500/15 text-emerald-500 border-emerald-500/20" : ""}
              >
                {link.isActive ? "Active" : "Expired"}
              </Badge>
              {link.isCustom && (
                <Badge variant="outline" className="text-[10px] font-mono">
                  Custom
                </Badge>
              )}
            </div>
          </div>
          <SheetDescription className="text-xs text-muted-foreground">
            Created on {formattedCreated}
          </SheetDescription>
        </SheetHeader>

        {/* Short Link Section (Unclipped Full Width Card) */}
        <div className="space-y-2.5 rounded-xl border border-border/70 bg-muted/30 p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-muted-foreground">
              Short link
            </span>
            <span className="text-[10px] font-mono text-muted-foreground/80">
              HTTP 302 Redirection
            </span>
          </div>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-1">
            <span className="font-mono text-sm font-semibold text-foreground break-all select-all">
              {link.shortUrl}
            </span>
            <div className="flex items-center gap-1.5 shrink-0">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleCopy}
                className="gap-1.5 cursor-pointer h-8 px-2.5 text-xs shadow-2xs"
              >
                {copied ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
                <span>{copied ? "Copied" : "Copy"}</span>
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                onClick={() => window.open(link.shortUrl, "_blank")}
                className="cursor-pointer h-8 w-8 text-muted-foreground hover:text-foreground shadow-2xs"
                title="Open short link"
              >
                <ExternalLink className="h-3.5 w-3.5" />
              </Button>
            </div>
          </div>
        </div>

        {/* Destination Section */}
        <div className="space-y-2">
          <span className="text-xs font-medium text-muted-foreground">
            Destination URL
          </span>
          <div className="rounded-xl border border-border/70 bg-card/60 p-3.5 font-mono text-xs break-all text-muted-foreground leading-relaxed select-all">
            {link.longUrl}
          </div>
        </div>

        {/* Telemetry Strip */}
        <div className="grid grid-cols-2 gap-3">
          <div className="rounded-xl border border-border/70 bg-muted/20 p-3.5 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <MousePointerClick className="h-3.5 w-3.5 text-primary" />
              <span>Total clicks</span>
            </div>
            <p className="font-mono text-2xl font-bold tracking-tight text-foreground tabular-nums">
              {link.clickCount.toLocaleString()}
            </p>
          </div>

          <div className="rounded-xl border border-border/70 bg-muted/20 p-3.5 space-y-1">
            <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <Clock className="h-3.5 w-3.5 text-primary" />
              <span>Created</span>
            </div>
            <p className="font-mono text-xs font-semibold text-foreground truncate pt-2">
              {formattedCreated}
            </p>
          </div>
        </div>

        {/* QR Code Block */}
        <div className="space-y-2">
          <span className="text-xs font-medium text-muted-foreground">
            QR code
          </span>
          <div className="rounded-xl border border-border/70 bg-card/60 p-4 flex justify-center">
            <QrCodeBlock url={link.shortUrl} size={180} />
          </div>
        </div>

        <Separator />

        {/* Danger Zone: Delete Link */}
        <div className="space-y-3 pt-1">
          <span className="text-xs font-medium text-destructive">
            Danger zone
          </span>
          <Button
            type="button"
            variant="destructive"
            onClick={() => setIsConfirmOpen(true)}
            disabled={isDeleting}
            className="w-full gap-2 cursor-pointer h-10 text-xs font-medium shadow-xs"
          >
            <Trash2 className="h-4 w-4" />
            <span>Delete this short link</span>
          </Button>
        </div>
      </SheetContent>

      {/* Delete Confirmation Alert Dialog */}
      <DeleteLinkDialog
        open={isConfirmOpen}
        onOpenChange={setIsConfirmOpen}
        shortCode={link.shortCode}
        shortUrl={link.shortUrl}
        destinationUrl={link.longUrl}
        clickCount={link.clickCount}
        onConfirm={handleConfirmDelete}
        isDeleting={isDeleting}
      />
    </Sheet>
  );
}
