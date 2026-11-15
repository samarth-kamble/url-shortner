"use client";

import { useEffect, useRef } from "react";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Search, ArrowUpDown } from "lucide-react";

export type StatusFilter = "all" | "active" | "expired";
export type SortOption = "newest" | "clicks";

interface LinkToolbarProps {
  searchQuery: string;
  onSearchChange: (q: string) => void;
  statusFilter: StatusFilter;
  onStatusFilterChange: (status: StatusFilter) => void;
  sortBy: SortOption;
  onSortByChange: (sort: SortOption) => void;
}

export function LinkToolbar({
  searchQuery,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  sortBy,
  onSortByChange,
}: LinkToolbarProps) {
  const searchInputRef = useRef<HTMLInputElement | null>(null);

  // Keyboard shortcut: '/' focuses search input
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (
        e.key === "/" &&
        document.activeElement?.tagName !== "INPUT" &&
        document.activeElement?.tagName !== "TEXTAREA"
      ) {
        e.preventDefault();
        searchInputRef.current?.focus();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      {/* Search Input */}
      <div className="relative flex-1 max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
        <Input
          ref={searchInputRef}
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Filter by short code or destination... (Press /)"
          className="h-10 pl-9 pr-9 text-xs sm:text-sm font-sans border-border/80 bg-card/60 backdrop-blur-xs"
        />
        <kbd className="absolute right-3 top-1/2 -translate-y-1/2 hidden sm:inline-flex items-center rounded border border-border/70 bg-muted/40 px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground select-none">
          /
        </kbd>
      </div>

      {/* Filter Tabs & Sort Dropdown */}
      <div className="flex items-center gap-2">
        {/* Status Filter Toggle Pills */}
        <div className="flex items-center rounded-lg border border-border/80 bg-muted/40 p-1 text-xs">
          <button
            type="button"
            onClick={() => onStatusFilterChange("all")}
            className={`rounded-md px-2.5 py-1 font-medium transition-all cursor-pointer ${
              statusFilter === "all"
                ? "bg-background text-foreground shadow-xs font-semibold"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            All
          </button>
          <button
            type="button"
            onClick={() => onStatusFilterChange("active")}
            className={`rounded-md px-2.5 py-1 font-medium transition-all cursor-pointer ${
              statusFilter === "active"
                ? "bg-background text-emerald-500 shadow-xs font-semibold"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            Active
          </button>
          <button
            type="button"
            onClick={() => onStatusFilterChange("expired")}
            className={`rounded-md px-2.5 py-1 font-medium transition-all cursor-pointer ${
              statusFilter === "expired"
                ? "bg-background text-amber-500 shadow-xs font-semibold"
                : "text-muted-foreground hover:text-foreground"
            }`}
          >
            Expired
          </button>
        </div>

        {/* Sort Dropdown */}
        <DropdownMenu>
          <DropdownMenuTrigger className="inline-flex h-9 items-center gap-1.5 rounded-md border border-border/80 bg-background px-3 text-xs font-medium text-foreground hover:bg-muted/70 transition-colors cursor-pointer">
            <ArrowUpDown className="h-3.5 w-3.5 text-muted-foreground" />
            <span>{sortBy === "newest" ? "Newest" : "Most Clicks"}</span>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="!w-40 w-40 min-w-40 text-xs p-1 shadow-md border-border/80 bg-popover rounded-xl">
            <DropdownMenuItem
              onClick={() => onSortByChange("newest")}
              className="cursor-pointer whitespace-nowrap py-1.5 px-2 font-medium"
            >
              Newest first
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() => onSortByChange("clicks")}
              className="cursor-pointer whitespace-nowrap py-1.5 px-2 font-medium"
            >
              Most clicks
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );
}
