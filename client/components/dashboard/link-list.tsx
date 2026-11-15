"use client";

import { useState, useOptimistic, useTransition, useEffect } from "react";
import { LinkItem } from "@/lib/link-schemas";
import { LinkRow } from "@/components/dashboard/link-row";
import { LinkToolbar, StatusFilter, SortOption } from "@/components/dashboard/link-toolbar";
import { EmptyState } from "@/components/dashboard/empty-state";
import { CreateLinkDialog } from "@/components/dashboard/create-link-dialog";
import { LinkDetailsSheet } from "@/components/dashboard/link-details-sheet";
import { DeleteLinkDialog } from "@/components/dashboard/delete-link-dialog";
import { QrCodeBlock } from "@/components/dashboard/qr-code-block";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { deleteLinkAction } from "@/app/actions/link-actions";
import { toast } from "sonner";
import { AnimatePresence, motion } from "motion/react";

interface LinkListProps {
  initialLinks: LinkItem[];
  isCreateOpen?: boolean;
  onOpenCreateChange?: (open: boolean) => void;
  onSelectForDetails?: (link: LinkItem | null) => void;
  selectedLinkForDetails?: LinkItem | null;
}

type OptimisticAction =
  | { type: "ADD"; link: LinkItem }
  | { type: "DELETE"; shortCode: string };

export function LinkList({
  initialLinks,
  isCreateOpen: controlledCreateOpen,
  onOpenCreateChange: setControlledCreateOpen,
  onSelectForDetails,
  selectedLinkForDetails,
}: LinkListProps) {
  const [internalCreateOpen, setInternalCreateOpen] = useState(false);
  const isCreateOpen = controlledCreateOpen !== undefined ? controlledCreateOpen : internalCreateOpen;
  const setCreateOpen = setControlledCreateOpen || setInternalCreateOpen;

  const [activeDetailsLink, setActiveDetailsLink] = useState<LinkItem | null>(null);
  const [activeQrLink, setActiveQrLink] = useState<LinkItem | null>(null);
  const [linkToDelete, setLinkToDelete] = useState<LinkItem | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Search, Filter & Sort State
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [sortBy, setSortBy] = useState<SortOption>("newest");

  const [, startTransition] = useTransition();

  // React 19 useOptimistic for immediate row additions & deletions
  const [optimisticLinks, setOptimisticLinks] = useOptimistic(
    initialLinks,
    (state: LinkItem[], action: OptimisticAction) => {
      if (action.type === "ADD") {
        return [action.link, ...state];
      }
      if (action.type === "DELETE") {
        return state.filter((l) => l.shortCode !== action.shortCode);
      }
      return state;
    }
  );

  // Keyboard shortcut: 'C' opens Create Link modal
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (
        e.key.toLowerCase() === "c" &&
        !isCreateOpen &&
        document.activeElement?.tagName !== "INPUT" &&
        document.activeElement?.tagName !== "TEXTAREA"
      ) {
        e.preventDefault();
        setCreateOpen(true);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isCreateOpen, setCreateOpen]);

  // Handle external selection for details (e.g. from CommandPalette)
  useEffect(() => {
    if (selectedLinkForDetails) {
      const frame = requestAnimationFrame(() => {
        setActiveDetailsLink(selectedLinkForDetails);
      });
      return () => cancelAnimationFrame(frame);
    }
  }, [selectedLinkForDetails]);

  const handleLinkCreated = (newLink: LinkItem) => {
    startTransition(() => {
      setOptimisticLinks({ type: "ADD", link: newLink });
    });
  };

  const handleDeleteLink = (shortCode: string) => {
    const target = optimisticLinks.find((l) => l.shortCode === shortCode);
    if (target) {
      setLinkToDelete(target);
    }
  };

  const handleConfirmDelete = async () => {
    if (!linkToDelete) return;
    const shortCode = linkToDelete.shortCode;
    setIsDeleting(true);

    startTransition(() => {
      setOptimisticLinks({ type: "DELETE", shortCode });
    });

    try {
      const res = await deleteLinkAction(shortCode);
      if (!res.success) {
        toast.error("Failed to delete link", { description: res.error });
      } else {
        toast.success("Link deleted successfully");
        setLinkToDelete(null);
      }
    } catch {
      toast.error("Failed to delete link");
    } finally {
      setIsDeleting(false);
    }
  };

  // Filter & Sort Logic
  const filteredLinks = optimisticLinks
    .filter((l) => {
      // Status filter
      if (statusFilter === "active" && !l.isActive) return false;
      if (statusFilter === "expired" && l.isActive) return false;

      // Search filter (shortCode or longUrl)
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase().trim();
        return (
          l.shortCode.toLowerCase().includes(query) ||
          l.longUrl.toLowerCase().includes(query)
        );
      }
      return true;
    })
    .sort((a, b) => {
      if (sortBy === "clicks") {
        return b.clickCount - a.clickCount;
      }
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });

  return (
    <div className="space-y-4">
      {/* Search & Filter Toolbar */}
      <LinkToolbar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        statusFilter={statusFilter}
        onStatusFilterChange={setStatusFilter}
        sortBy={sortBy}
        onSortByChange={setSortBy}
      />

      {/* Links List */}
      {filteredLinks.length === 0 ? (
        <EmptyState
          onCreateClick={() => setCreateOpen(true)}
          isFiltered={Boolean(searchQuery || statusFilter !== "all")}
        />
      ) : (
        <div className="space-y-2.5">
          <AnimatePresence initial={false}>
            {filteredLinks.map((link) => (
              <motion.div
                key={link.shortCode}
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, height: 0, overflow: "hidden" }}
                transition={{ duration: 0.25, ease: "easeOut" }}
              >
                <LinkRow
                  link={link}
                  onOpenDetails={(l) => {
                    setActiveDetailsLink(l);
                    onSelectForDetails?.(l);
                  }}
                  onDeleteLink={handleDeleteLink}
                  onOpenQr={(l) => setActiveQrLink(l)}
                />
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      )}

      {/* Create Link Dialog */}
      <CreateLinkDialog
        open={isCreateOpen}
        onOpenChange={setCreateOpen}
        onLinkCreated={handleLinkCreated}
      />

      {/* Slide-over Details Sheet */}
      <LinkDetailsSheet
        link={activeDetailsLink}
        open={Boolean(activeDetailsLink)}
        onOpenChange={(open) => {
          if (!open) {
            setActiveDetailsLink(null);
            onSelectForDetails?.(null);
          }
        }}
        onLinkDeleted={(code) => {
          startTransition(() => {
            setOptimisticLinks({ type: "DELETE", shortCode: code });
          });
        }}
      />

      {/* Delete Confirmation Alert Dialog */}
      <DeleteLinkDialog
        open={Boolean(linkToDelete)}
        onOpenChange={(open) => !open && setLinkToDelete(null)}
        shortCode={linkToDelete?.shortCode ?? null}
        shortUrl={linkToDelete?.shortUrl}
        destinationUrl={linkToDelete?.longUrl}
        clickCount={linkToDelete?.clickCount}
        onConfirm={handleConfirmDelete}
        isDeleting={isDeleting}
      />

      {/* Standalone QR Code Modal */}
      <Dialog
        open={Boolean(activeQrLink)}
        onOpenChange={(open) => !open && setActiveQrLink(null)}
      >
        <DialogContent className="w-[92vw] sm:max-w-sm p-6 sm:p-7 rounded-2xl border-border/80 bg-card shadow-2xl">
          <DialogHeader className="text-center pb-1">
            <DialogTitle className="font-heading text-lg font-bold">
              QR Code
            </DialogTitle>
            <DialogDescription className="font-mono text-xs text-muted-foreground truncate">
              /{activeQrLink?.shortCode}
            </DialogDescription>
          </DialogHeader>
          {activeQrLink && (
            <QrCodeBlock url={activeQrLink.shortUrl} size={180} />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
