"use client";

import React from "react";
import { AlertTriangle, ExternalLink, Loader2, MousePointerClick, Trash2 } from "lucide-react";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";

interface DeleteLinkDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  shortCode: string | null;
  shortUrl?: string;
  destinationUrl?: string;
  clickCount?: number;
  onConfirm: () => Promise<void> | void;
  isDeleting?: boolean;
}

export function DeleteLinkDialog({
  open,
  onOpenChange,
  shortCode,
  shortUrl,
  destinationUrl,
  clickCount,
  onConfirm,
  isDeleting = false,
}: DeleteLinkDialogProps) {
  if (!shortCode) return null;

  const displayShort = shortUrl || `/${shortCode}`;

  return (
    <AlertDialog open={open} onOpenChange={(val) => !isDeleting && onOpenChange(val)}>
      <AlertDialogContent className="w-[92vw] sm:w-full max-w-lg sm:!max-w-xl data-[size=default]:!max-w-xl p-6 sm:p-7 gap-5 rounded-2xl border-border/80 shadow-2xl">
        <AlertDialogHeader className="sm:place-items-start sm:text-left gap-3.5">
          {/* Warning Icon Badge */}
          <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-destructive/10 text-destructive border border-destructive/20 shadow-xs">
            <AlertTriangle className="h-5 w-5" />
          </div>

          <div className="space-y-1.5">
            <AlertDialogTitle className="text-lg font-bold tracking-tight text-foreground">
              Delete short link /{shortCode}?
            </AlertDialogTitle>
            <AlertDialogDescription className="text-xs sm:text-sm text-muted-foreground leading-relaxed">
              This action is <span className="font-semibold text-destructive">permanent and irreversible</span>. Once deleted, anyone visiting this short link will immediately receive an <span className="font-mono text-foreground font-medium">HTTP 404 Not Found</span>, and all historical click routing for this alias will be permanently severed.
            </AlertDialogDescription>
          </div>
        </AlertDialogHeader>

        {/* Link Info Card */}
        <div className="rounded-xl border border-border/80 bg-muted/40 p-4 space-y-2.5 text-xs">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2 min-w-0">
              <span className="font-mono text-sm font-semibold text-foreground truncate select-all">
                {displayShort}
              </span>
            </div>
            {typeof clickCount === "number" && (
              <span className="inline-flex items-center gap-1.5 rounded-md bg-background px-2.5 py-1 text-xs font-mono font-medium text-muted-foreground border border-border/70 shrink-0">
                <MousePointerClick className="h-3.5 w-3.5 text-primary" />
                {clickCount} {clickCount === 1 ? "click" : "clicks"}
              </span>
            )}
          </div>

          {destinationUrl && (
            <div className="flex items-start gap-2 text-muted-foreground text-xs pt-1 border-t border-border/50">
              <ExternalLink className="h-3.5 w-3.5 shrink-0 mt-0.5 text-muted-foreground/70" />
              <span className="break-all font-mono text-foreground/80" title={destinationUrl}>
                {destinationUrl}
              </span>
            </div>
          )}

          <div className="text-xs text-destructive bg-destructive/10 rounded-lg p-2.5 font-medium border border-destructive/20">
            ⚠️ Traffic routing and connected QR codes will cease functioning immediately.
          </div>
        </div>

        <AlertDialogFooter className="gap-2.5 sm:gap-2.5 pt-1">
          <AlertDialogCancel
            disabled={isDeleting}
            className="cursor-pointer h-10 px-4 text-xs font-medium"
          >
            Cancel
          </AlertDialogCancel>
          <Button
            type="button"
            variant="destructive"
            onClick={onConfirm}
            disabled={isDeleting}
            className="gap-2 cursor-pointer h-10 px-4 text-xs font-medium shadow-xs"
          >
            {isDeleting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Trash2 className="h-4 w-4" />
            )}
            <span>Delete permanently</span>
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

