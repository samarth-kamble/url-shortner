"use client";

import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Settings, Shield, Key } from "lucide-react";

export default function SettingsPage() {
  return (
    <DashboardShell>
      <div className="space-y-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <h1 className="font-heading text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
              Workspace Settings
            </h1>
            <Badge variant="outline" className="text-[10px] font-mono border-primary/30 text-primary">
              Day 22 Milestone
            </Badge>
          </div>
          <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed">
            API keys, custom root domains, team multi-tenancy, and security policies.
          </p>
        </div>

        <Card className="flex min-h-[360px] flex-col items-center justify-center rounded-2xl border-dashed border-border/80 bg-card/40 p-8 text-center backdrop-blur-xs">
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl border border-primary/20 bg-primary/10 text-primary shadow-xs">
            <Settings className="h-7 w-7" />
          </div>
          <h3 className="font-heading text-lg font-bold text-foreground">
            Custom Domains & API Tokens
          </h3>
          <p className="mt-1.5 max-w-md text-xs sm:text-sm text-muted-foreground leading-relaxed">
            Configure custom branded short link domains (e.g. <code className="font-mono text-xs text-foreground bg-muted px-1.5 py-0.5 rounded">go.company.com</code>) with automatic TLS certificates and distributed rate-limiting tiers.
          </p>
          <div className="mt-5 flex items-center gap-3 text-xs font-mono text-muted-foreground">
            <span className="flex items-center gap-1">
              <Shield className="h-3.5 w-3.5 text-emerald-500" /> Multi-Tenant RBAC
            </span>
            <span>&bull;</span>
            <span className="flex items-center gap-1">
              <Key className="h-3.5 w-3.5 text-primary" /> API Tokens
            </span>
          </div>
        </Card>
      </div>
    </DashboardShell>
  );
}

