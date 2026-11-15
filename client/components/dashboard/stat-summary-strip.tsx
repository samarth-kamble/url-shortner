"use client";

import { useEffect, useState } from "react";
import { LinkItem } from "@/lib/link-schemas";
import { Badge } from "@/components/ui/badge";
import { Link2, CheckCircle2, AlertCircle, MousePointerClick } from "lucide-react";
import { useReducedMotion } from "motion/react";

interface StatSummaryStripProps {
  links: LinkItem[];
}

export function StatSummaryStrip({ links }: StatSummaryStripProps) {
  const shouldReduceMotion = useReducedMotion();
  const totalLinks = links.length;
  const activeLinks = links.filter((l) => l.isActive).length;
  const expiredLinks = links.filter((l) => !l.isActive).length;
  const totalClicks = links.reduce((sum, l) => sum + (l.clickCount || 0), 0);

  const hasSampleData = links.some((l) => l.isSampleData);

  // One-time count up animation for clicks
  const [displayedClicks, setDisplayedClicks] = useState(shouldReduceMotion ? totalClicks : 0);

  useEffect(() => {
    if (shouldReduceMotion) {
      const frame = requestAnimationFrame(() => setDisplayedClicks(totalClicks));
      return () => cancelAnimationFrame(frame);
    }

    const start = 0;
    const end = totalClicks;
    if (end === 0) return;

    const duration = 800;
    const startTime = performance.now();

    const animate = (time: number) => {
      const elapsed = time - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const ease = 1 - Math.pow(1 - progress, 3);
      setDisplayedClicks(Math.round(start + (end - start) * ease));

      if (progress < 1) {
        requestAnimationFrame(animate);
      } else {
        setDisplayedClicks(end);
      }
    };

    requestAnimationFrame(animate);
  }, [totalClicks, shouldReduceMotion]);

  const cards = [
    {
      title: "Total Links",
      value: totalLinks.toLocaleString(),
      icon: Link2,
      color: "text-foreground",
      badge: null,
    },
    {
      title: "Active Routing",
      value: activeLinks.toLocaleString(),
      icon: CheckCircle2,
      color: "text-emerald-500",
      badge: null,
    },
    {
      title: "Expired",
      value: expiredLinks.toLocaleString(),
      icon: AlertCircle,
      color: "text-amber-500",
      badge: null,
    },
    {
      title: "Total Clicks",
      value: displayedClicks.toLocaleString(),
      icon: MousePointerClick,
      color: "text-primary",
      badge: hasSampleData ? "Sample data" : null,
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 sm:gap-4">
      {cards.map((card) => {
        const Icon = card.icon;
        return (
          <div
            key={card.title}
            className="relative flex flex-col justify-between rounded-xl border border-border/80 bg-card/90 p-4 shadow-xs backdrop-blur-xs transition-all hover:border-border hover:shadow-sm"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-medium text-muted-foreground">
                {card.title}
              </span>
              <Icon className={`h-4 w-4 ${card.color}`} />
            </div>

            <div className="mt-3 flex items-baseline justify-between gap-2">
              <span className="font-mono text-2xl font-bold tracking-tight text-foreground tabular-nums">
                {card.value}
              </span>

              {card.badge && (
                <Badge
                  variant="outline"
                  className="font-mono text-[9px] text-muted-foreground/80 px-1.5 py-0 border-border/60"
                >
                  {card.badge}
                </Badge>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
