"use client";

import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { BarChart3, Clock } from "lucide-react";

export default function AnalyticsPage() {
  return (
    <DashboardShell>
      <div className="space-y-6">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <h1 className="font-heading text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
              Analytics & Telemetry
            </h1>
            <Badge variant="outline" className="text-[10px] font-mono border-primary/30 text-primary">
              Day 21 Milestone
            </Badge>
          </div>
          <p className="text-xs sm:text-sm text-muted-foreground leading-relaxed">
            Real-time click streams, time-series windows, GeoIP heatmaps, and referrer telemetry.
          </p>
        </div>

        <Card className="flex min-h-[360px] flex-col items-center justify-center rounded-2xl border-dashed border-border/80 bg-card/40 p-8 text-center backdrop-blur-xs">
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl border border-primary/20 bg-primary/10 text-primary shadow-xs">
            <BarChart3 className="h-7 w-7" />
          </div>
          <h3 className="font-heading text-lg font-bold text-foreground">
            Click Stream Telemetry Pipeline
          </h3>
          <p className="mt-1.5 max-w-md text-xs sm:text-sm text-muted-foreground leading-relaxed">
            In Day 21, this dashboard connects to our Spring Boot <code className="font-mono text-xs text-foreground bg-muted px-1.5 py-0.5 rounded">/api/v1/links/&#123;code&#125;/analytics/timeseries</code> and Kafka aggregation consumer for interactive charts and geographical breakdowns.
          </p>
          <div className="mt-5 flex items-center gap-2 text-xs font-mono text-muted-foreground">
            <Clock className="h-3.5 w-3.5" />
            <span>Scheduled for Episode 21</span>
          </div>
        </Card>
      </div>
    </DashboardShell>
  );
}
