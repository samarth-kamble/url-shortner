"use client";

import { useState } from "react";
import { LinkItem } from "@/lib/link-schemas";
import { UserProfile } from "@/app/actions/auth-actions";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { StatSummaryStrip } from "@/components/dashboard/stat-summary-strip";
import { LinkList } from "@/components/dashboard/link-list";
import { Button } from "@/components/ui/button";
import { Plus } from "lucide-react";

interface DashboardViewProps {
  initialLinks: LinkItem[];
  userProfile?: UserProfile | null;
}

export function DashboardView({ initialLinks, userProfile }: DashboardViewProps) {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedDetailsLink, setSelectedDetailsLink] = useState<LinkItem | null>(null);

  return (
    <DashboardShell
      links={initialLinks}
      userProfile={userProfile}
      onCreateClick={() => setIsCreateOpen(true)}
      onSelectLink={(link) => setSelectedDetailsLink(link)}
    >
      {/* Page Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <h1 className="font-heading text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Links
          </h1>
          <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed">
            Manage, redirect and analyze your high-throughput short URLs.
          </p>
        </div>

        <div>
          <Button
            onClick={() => setIsCreateOpen(true)}
            className="gap-2 cursor-pointer h-10 px-4 text-xs sm:text-sm font-semibold shadow-xs"
          >
            <Plus className="h-4 w-4" />
            <span>Create link</span>
            <kbd className="hidden sm:inline-block ml-1 rounded bg-primary-foreground/20 px-1.5 py-0.5 text-[10px] font-mono">
              C
            </kbd>
          </Button>
        </div>
      </div>

      {/* KPI Stat Cards Summary */}
      <StatSummaryStrip links={initialLinks} />

      {/* Main Links List (Toolbar + Rich Rows + Details Sheet) */}
      <LinkList
        initialLinks={initialLinks}
        isCreateOpen={isCreateOpen}
        onOpenCreateChange={setIsCreateOpen}
        selectedLinkForDetails={selectedDetailsLink}
        onSelectForDetails={setSelectedDetailsLink}
      />
    </DashboardShell>
  );
}

