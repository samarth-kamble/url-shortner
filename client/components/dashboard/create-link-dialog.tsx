"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  createLinkSchema,
  CreateLinkFormData,
  LinkItem,
} from "@/lib/link-schemas";
import { createLinkAction } from "@/app/actions/link-actions";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { LinkPreview } from "@/components/dashboard/link-preview";
import { ScrambleText } from "@/components/dashboard/scramble-text";
import { QrCodeBlock } from "@/components/dashboard/qr-code-block";
import {
  Link2,
  Copy,
  Check,
  QrCode,
  ArrowRight,
  Sparkles,
  Loader2,
} from "lucide-react";
import { toast } from "sonner";

interface CreateLinkDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onLinkCreated: (link: LinkItem) => void;
}

export function CreateLinkDialog({
  open,
  onOpenChange,
  onLinkCreated,
}: CreateLinkDialogProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdLink, setCreatedLink] = useState<LinkItem | null>(null);
  const [showQr, setShowQr] = useState(false);
  const [copied, setCopied] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    reset,
    setError,
    formState: { errors },
  } = useForm<CreateLinkFormData>({
    resolver: zodResolver(createLinkSchema),
    defaultValues: {
      longUrl: "",
      customAlias: "",
      expiresAt: "",
    },
  });

  const longUrlValue = watch("longUrl");
  const customAliasValue = watch("customAlias");

  const onSubmit = async (data: CreateLinkFormData) => {
    setIsSubmitting(true);
    try {
      const result = await createLinkAction(data);
      if (!result.success || !result.link) {
        if (
          result.error?.toLowerCase().includes("alias") ||
          result.error?.toLowerCase().includes("already")
        ) {
          setError("customAlias", {
            type: "manual",
            message: result.error,
          });
        } else {
          toast.error("Creation failed", { description: result.error });
        }
        return;
      }

      setCreatedLink(result.link);
      onLinkCreated(result.link);
      toast.success("Short link created!");
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Failed to create link";
      toast.error("Creation failed", { description: msg });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCopy = () => {
    if (!createdLink) return;
    navigator.clipboard?.writeText(createdLink.shortUrl);
    setCopied(true);
    toast.success("Short link copied to clipboard");
    setTimeout(() => setCopied(false), 2000);
  };

  const handleResetForAnother = () => {
    setCreatedLink(null);
    setShowQr(false);
    reset();
  };

  const handleClose = () => {
    onOpenChange(false);
    // Reset state after dialog closes
    setTimeout(() => {
      setCreatedLink(null);
      setShowQr(false);
      reset();
    }, 200);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-xl md:max-w-2xl w-full p-6 sm:p-8 rounded-2xl border-border/80 bg-card shadow-2xl">
        <DialogHeader className="space-y-1.5 text-left pb-2">
          <div className="flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Link2 className="h-4 w-4" />
            </div>
            <DialogTitle className="font-heading text-xl font-bold tracking-tight">
              {createdLink ? "Link ready!" : "Create short link"}
            </DialogTitle>
          </div>
          <DialogDescription className="text-xs text-muted-foreground sm:text-sm">
            {createdLink
              ? "Your link has locked in and is ready to route traffic."
              : "Generate an instant Base62 short URL with sub-millisecond redirection."}
          </DialogDescription>
        </DialogHeader>

        {createdLink ? (
          /* Success Lock-In State */
          <div className="space-y-5 pt-1">
            <div className="rounded-xl border border-primary/30 bg-primary/5 p-5 text-center space-y-3">
              <div className="flex items-center justify-center gap-1.5 text-xs font-mono text-primary font-medium">
                <Sparkles className="h-3.5 w-3.5" />
                <span>Base62 Routing Active</span>
              </div>

              {/* Scramble-Reveal Component */}
              <div className="font-mono text-2xl font-bold tracking-tight text-foreground sm:text-3xl break-all">
                <ScrambleText
                  text={createdLink.shortUrl.replace(/^https?:\/\//, "")}
                  duration={600}
                />
              </div>

              <p className="text-xs text-muted-foreground truncate max-w-sm mx-auto">
                Redirects to: {createdLink.longUrl}
              </p>
            </div>

            {/* Quick Actions: Copy and QR toggle */}
            <div className="flex items-center gap-3">
              <Button
                type="button"
                onClick={handleCopy}
                className="flex-1 gap-2 cursor-pointer h-11 font-medium"
              >
                {copied ? (
                  <>
                    <Check className="h-4 w-4 text-emerald-400" />
                    <span>Copied!</span>
                  </>
                ) : (
                  <>
                    <Copy className="h-4 w-4" />
                    <span>Copy Link</span>
                  </>
                )}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => setShowQr((prev) => !prev)}
                className="gap-2 cursor-pointer h-11 text-xs"
              >
                <QrCode className="h-4 w-4" />
                <span>{showQr ? "Hide QR" : "QR Code"}</span>
              </Button>
            </div>

            {/* Expandable QR Code */}
            {showQr && (
              <div className="pt-2 animate-in fade-in zoom-in-95 duration-200">
                <QrCodeBlock url={createdLink.shortUrl} size={180} />
              </div>
            )}

            <div className="flex items-center justify-between border-t border-border/60 pt-4">
              <Button
                type="button"
                variant="ghost"
                onClick={handleResetForAnother}
                className="text-xs cursor-pointer text-muted-foreground hover:text-foreground"
              >
                Create another
              </Button>
              <Button
                type="button"
                variant="default"
                onClick={handleClose}
                className="text-xs cursor-pointer px-5"
              >
                Done
              </Button>
            </div>
          </div>
        ) : (
          /* Creation Form */
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-1">
            {/* Live Preview Card */}
            <LinkPreview
              longUrl={longUrlValue}
              customAlias={customAliasValue}
            />

            {/* Destination URL Field */}
            <div className="space-y-1.5">
              <label
                htmlFor="longUrl"
                className="text-xs sm:text-sm font-medium text-foreground"
              >
                Destination URL <span className="text-destructive">*</span>
              </label>
              <Input
                id="longUrl"
                type="url"
                autoFocus
                placeholder="https://example.com/long-campaign-url"
                disabled={isSubmitting}
                className="h-11 text-sm font-mono"
                {...register("longUrl")}
              />
              {errors.longUrl && (
                <p className="text-xs text-destructive animate-in fade-in slide-in-from-top-1">
                  {errors.longUrl.message}
                </p>
              )}
            </div>

            {/* Optional Custom Alias */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="customAlias"
                  className="text-xs sm:text-sm font-medium text-foreground"
                >
                  Custom alias <span className="text-xs font-normal text-muted-foreground">(Optional)</span>
                </label>
              </div>
              <div className="relative flex items-center">
                <span className="absolute left-3 font-mono text-xs text-muted-foreground select-none pointer-events-none">
                  /
                </span>
                <Input
                  id="customAlias"
                  type="text"
                  placeholder="custom-slug"
                  disabled={isSubmitting}
                  className="h-11 pl-7 text-sm font-mono"
                  {...register("customAlias")}
                />
              </div>
              {errors.customAlias && (
                <p className="text-xs text-destructive animate-in fade-in slide-in-from-top-1">
                  {errors.customAlias.message}
                </p>
              )}
            </div>

            {/* Submit Button */}
            <div className="pt-2">
              <Button
                type="submit"
                disabled={isSubmitting}
                className="w-full h-11 gap-2 cursor-pointer font-semibold text-sm"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span>Routing...</span>
                  </>
                ) : (
                  <>
                    <span>Create Short Link</span>
                    <ArrowRight className="h-4 w-4" />
                  </>
                )}
              </Button>
            </div>
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}

