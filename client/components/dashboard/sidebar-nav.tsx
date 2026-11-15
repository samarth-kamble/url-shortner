"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  Link2,
  BarChart2,
  Settings,
  PanelLeftClose,
  PanelLeft,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";

interface SidebarNavProps {
  collapsed: boolean;
  onToggleCollapse: () => void;
  linksCount?: number;
}

const WORKSPACE_NAV = [
  { label: "Links", href: "/dashboard", icon: Link2, badgeKey: "links" },
  { label: "Analytics", href: "/analytics", icon: BarChart2, badge: "Live" },
];

const SYSTEM_NAV = [
  { label: "Settings", href: "/settings", icon: Settings },
];

export function SidebarNav({
  collapsed,
  onToggleCollapse,
  linksCount = 0,
}: SidebarNavProps) {
  const pathname = usePathname();

  return (
    <TooltipProvider delay={150}>
      <aside
        className={cn(
          "sticky top-0 h-screen hidden lg:flex flex-col justify-between border-r border-border/70 bg-card/80 backdrop-blur-md transition-all duration-200 select-none overflow-hidden z-20",
          collapsed ? "w-16" : "w-64"
        )}
      >
        {/* Top Header & Scrollable Nav Area */}
        <div className="flex flex-col flex-1 overflow-y-auto no-scrollbar">
          {/* Brand Row */}
          <div className="flex h-14 shrink-0 items-center justify-between px-3.5 border-b border-border/60">
            <Link
              href="/dashboard"
              className={cn(
                "flex items-center gap-2.5 overflow-hidden transition-all",
                collapsed ? "justify-center w-full" : ""
              )}
            >
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold shadow-xs">
                <Link2 className="h-4 w-4" />
              </div>
              {!collapsed && (
                <div className="flex items-center gap-2 min-w-0">
                  <span className="font-heading text-base font-bold tracking-tight text-foreground truncate">
                    ShortLink
                  </span>
                  <span className="rounded bg-primary/10 px-1.5 py-0.2 font-mono text-[10px] font-semibold text-primary">
                    v1.0
                  </span>
                </div>
              )}
            </Link>

            {!collapsed && (
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={onToggleCollapse}
                className="cursor-pointer text-muted-foreground hover:text-foreground h-7 w-7"
                title="Collapse sidebar (⌘B)"
              >
                <PanelLeftClose className="h-4 w-4" />
              </Button>
            )}
          </div>

          {/* Navigation Items */}
          <nav className="p-2 space-y-4 flex-1">
            {/* WORKSPACE SECTION */}
            <div className="space-y-1">
              {!collapsed && (
                <p className="px-2.5 pt-1.5 pb-1 text-[11px] font-medium text-muted-foreground/80">
                  Workspace
                </p>
              )}
              {WORKSPACE_NAV.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.href;

                const content = (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "group relative flex h-10 items-center gap-3 rounded-lg px-3 text-xs sm:text-[13px] font-medium transition-all",
                      isActive
                        ? "bg-primary/10 text-primary font-semibold shadow-2xs before:absolute before:left-0 before:top-2 before:bottom-2 before:w-1 before:rounded-r before:bg-primary"
                        : "text-muted-foreground hover:bg-muted/60 hover:text-foreground",
                      collapsed ? "justify-center px-0 w-full" : ""
                    )}
                  >
                    <Icon className={cn("h-4 w-4 shrink-0 transition-transform group-hover:scale-105", isActive ? "text-primary" : "")} />
                    {!collapsed && (
                      <div className="flex flex-1 items-center justify-between min-w-0">
                        <span className="truncate">{item.label}</span>
                        {item.badgeKey === "links" && linksCount > 0 && (
                          <span className="rounded-full bg-primary/15 px-1.5 py-0.2 font-mono text-[10px] font-bold text-primary">
                            {linksCount}
                          </span>
                        )}
                        {item.badge && (
                          <span className="rounded bg-muted px-1.5 py-0.2 font-mono text-[9px] font-medium text-muted-foreground">
                            {item.badge}
                          </span>
                        )}
                      </div>
                    )}
                  </Link>
                );

                if (collapsed) {
                  return (
                    <Tooltip key={item.href}>
                      <TooltipTrigger className="w-full block">{content}</TooltipTrigger>
                      <TooltipContent side="right" className="font-sans text-xs">
                        {item.label}
                      </TooltipContent>
                    </Tooltip>
                  );
                }

                return content;
              })}
            </div>

            {/* PREFERENCES SECTION */}
            <div className="space-y-1">
              {!collapsed && (
                <p className="px-2.5 pt-1.5 pb-1 text-[11px] font-medium text-muted-foreground/80">
                  Preferences
                </p>
              )}
              {SYSTEM_NAV.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.href;

                const content = (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "group relative flex h-10 items-center gap-3 rounded-lg px-3 text-xs sm:text-[13px] font-medium transition-all",
                      isActive
                        ? "bg-primary/10 text-primary font-semibold shadow-2xs before:absolute before:left-0 before:top-2 before:bottom-2 before:w-1 before:rounded-r before:bg-primary"
                        : "text-muted-foreground hover:bg-muted/60 hover:text-foreground",
                      collapsed ? "justify-center px-0 w-full" : ""
                    )}
                  >
                    <Icon className={cn("h-4 w-4 shrink-0 transition-transform group-hover:scale-105", isActive ? "text-primary" : "")} />
                    {!collapsed && <span className="truncate">{item.label}</span>}
                  </Link>
                );

                if (collapsed) {
                  return (
                    <Tooltip key={item.href}>
                      <TooltipTrigger className="w-full block">{content}</TooltipTrigger>
                      <TooltipContent side="right" className="font-sans text-xs">
                        {item.label}
                      </TooltipContent>
                    </Tooltip>
                  );
                }

                return content;
              })}
            </div>
          </nav>
        </div>

        {/* Pinned Bottom Section: Clean Collapse / Expand Toggle Only */}
        <div className="shrink-0 border-t border-border/70 p-2.5 bg-card/60">
          {collapsed ? (
            <Tooltip>
              <TooltipTrigger
                onClick={onToggleCollapse}
                className="w-full h-9 inline-flex items-center justify-center rounded-lg cursor-pointer text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
              >
                <PanelLeft className="h-4 w-4" />
              </TooltipTrigger>
              <TooltipContent side="right" className="font-sans text-xs">
                Expand sidebar (⌘B)
              </TooltipContent>
            </Tooltip>
          ) : (
            <button
              type="button"
              onClick={onToggleCollapse}
              className="flex w-full items-center justify-between rounded-lg px-3 py-2 text-xs font-medium text-muted-foreground hover:bg-muted/70 hover:text-foreground transition-colors cursor-pointer"
            >
              <span className="flex items-center gap-2.5">
                <PanelLeftClose className="h-4 w-4" />
                <span>Collapse</span>
              </span>
              <kbd className="rounded border border-border/70 bg-background/60 px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground">
                ⌘B
              </kbd>
            </button>
          )}
        </div>
      </aside>
    </TooltipProvider>
  );
}
