"use client";

import { Button } from "@/components/ui/button";
import { Plus, Link2 } from "lucide-react";

interface EmptyStateProps {
  onCreateClick: () => void;
  isFiltered?: boolean;
}

export function EmptyState({ onCreateClick, isFiltered = false }: EmptyStateProps) {
  return (
    <div className="flex min-h-[340px] flex-col items-center justify-center rounded-2xl border border-dashed border-border/80 bg-card/40 p-8 text-center backdrop-blur-xs">
      {/* Code-drawn SVG Illustration */}
      <div className="relative mb-5 flex h-20 w-20 items-center justify-center rounded-2xl border border-border/80 bg-muted/40 shadow-inner">
        <svg
          className="absolute inset-0 h-full w-full stroke-primary/30 [stroke-dasharray:3_3]"
          viewBox="0 0 80 80"
          fill="none"
        >
          <rect x="8" y="8" width="64" height="64" rx="12" />
        </svg>
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary text-primary-foreground shadow-md transition-transform hover:scale-105">
          <Link2 className="h-6 w-6" />
        </div>
      </div>

      <h3 className="font-heading text-lg font-bold tracking-tight text-foreground sm:text-xl">
        {isFiltered ? "No links match your filter" : "No short links created yet"}
      </h3>
      <p className="mt-1.5 max-w-sm text-xs text-muted-foreground sm:text-sm leading-relaxed">
        {isFiltered
          ? "Try adjusting your search query or status filter to see other links."
          : "Transform your first destination URL into a high-speed Base62 redirect with real-time click telemetry."}
      </p>

      {!isFiltered && (
        <div className="mt-6">
          <Button
            onClick={onCreateClick}
            className="gap-2 cursor-pointer font-medium text-xs sm:text-sm h-10 px-5 shadow-sm"
          >
            <Plus className="h-4 w-4" />
            <span>Create your first link</span>
            <kbd className="hidden sm:inline-block ml-1 rounded bg-primary-foreground/20 px-1.5 py-0.5 text-[10px] font-mono">
              C
            </kbd>
          </Button>
        </div>
      )}
    </div>
  );
}

