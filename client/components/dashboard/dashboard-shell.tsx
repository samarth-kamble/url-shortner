"use client";

import { useState, useEffect } from "react";
import { SidebarNav } from "@/components/dashboard/sidebar-nav";
import { TopBar } from "@/components/dashboard/top-bar";
import { CommandPalette } from "@/components/dashboard/command-palette";
import { Sheet, SheetContent } from "@/components/ui/sheet";
import { LinkItem } from "@/lib/link-schemas";
import { UserProfile } from "@/app/actions/auth-actions";
import { Link2, BarChart2, Settings, LogOut } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { logoutAction } from "@/app/actions/auth-actions";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";

interface DashboardShellProps {
  children: React.ReactNode;
  links?: LinkItem[];
  userProfile?: UserProfile | null;
  onCreateClick?: () => void;
  onSelectLink?: (link: LinkItem) => void;
}

export function DashboardShell({
  children,
  links = [],
  userProfile,
  onCreateClick,
  onSelectLink,
}: DashboardShellProps) {
  const pathname = usePathname();
  const router = useRouter();

  // Collapsible Sidebar state with localStorage persistence
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [commandPaletteOpen, setCommandPaletteOpen] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem("shortlink_sidebar_collapsed");
      if (stored !== null) {
        const frame = requestAnimationFrame(() => {
          setSidebarCollapsed(stored === "true");
        });
        return () => cancelAnimationFrame(frame);
      }
    } catch {
      // ignore
    }
  }, []);

  const handleToggleCollapse = () => {
    setSidebarCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem("shortlink_sidebar_collapsed", String(next));
      } catch {
        // ignore
      }
      return next;
    });
  };

  const handleLogout = async () => {
    setMobileMenuOpen(false);
    await logoutAction();
    toast.success("Signed out");
    router.push("/login");
  };

  const navItems = [
    { label: "Links", href: "/dashboard", icon: Link2 },
    { label: "Analytics", href: "/analytics", icon: BarChart2 },
    { label: "Settings", href: "/settings", icon: Settings },
  ];

  return (
    <div className="flex min-h-screen w-full bg-background text-foreground">
      {/* Desktop Collapsible Sidebar */}
      <SidebarNav
        collapsed={sidebarCollapsed}
        onToggleCollapse={handleToggleCollapse}
        linksCount={links.length}
      />

      {/* Mobile Drawer Sheet */}
      <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
        <SheetContent side="left" className="w-68 p-0 flex flex-col justify-between overflow-hidden">
          <div className="flex flex-col flex-1 overflow-y-auto no-scrollbar">
            {/* Brand Header */}
            <div className="flex h-14 shrink-0 items-center gap-2.5 px-4 border-b border-border/70">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold shadow-xs">
                <Link2 className="h-4 w-4" />
              </div>
              <div className="flex items-center gap-2">
                <span className="font-heading text-base font-bold text-foreground">ShortLink</span>
                <span className="rounded bg-primary/10 px-1.5 py-0.2 font-mono text-[10px] font-semibold text-primary">v1.0</span>
              </div>
            </div>

            {/* Nav Items */}
            <nav className="p-2 space-y-4 flex-1">
              <div className="space-y-1">
                <p className="px-2.5 pt-1.5 pb-1 text-[11px] font-medium text-muted-foreground/80">
                  Workspace
                </p>
                {navItems.slice(0, 2).map((item) => {
                  const Icon = item.icon;
                  const isActive = pathname === item.href;
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={() => setMobileMenuOpen(false)}
                      className={`flex h-10 items-center gap-3 rounded-lg px-3 text-xs sm:text-[13px] font-medium transition-colors ${
                        isActive
                          ? "bg-primary/10 text-primary font-semibold"
                          : "text-muted-foreground hover:bg-muted hover:text-foreground"
                      }`}
                    >
                      <Icon className="h-4 w-4" />
                      <span>{item.label}</span>
                    </Link>
                  );
                })}
              </div>

              <div className="space-y-1">
                <p className="px-2.5 pt-1.5 pb-1 text-[11px] font-medium text-muted-foreground/80">
                  Preferences
                </p>
                {navItems.slice(2).map((item) => {
                  const Icon = item.icon;
                  const isActive = pathname === item.href;
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={() => setMobileMenuOpen(false)}
                      className={`flex h-10 items-center gap-3 rounded-lg px-3 text-xs sm:text-[13px] font-medium transition-colors ${
                        isActive
                          ? "bg-primary/10 text-primary font-semibold"
                          : "text-muted-foreground hover:bg-muted hover:text-foreground"
                      }`}
                    >
                      <Icon className="h-4 w-4" />
                      <span>{item.label}</span>
                    </Link>
                  );
                })}
              </div>
            </nav>
          </div>

          <div className="shrink-0 border-t border-border/70 p-4 space-y-3 bg-card/60">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/15 text-primary font-mono text-xs font-bold">
                {(userProfile?.fullName || userProfile?.email || "U")[0].toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-semibold text-foreground truncate">
                  {userProfile?.fullName || "Developer"}
                </p>
                <p className="text-[11px] text-muted-foreground truncate font-mono">
                  {userProfile?.email || "Personal Workspace"}
                </p>
              </div>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={handleLogout}
              className="w-full gap-2 cursor-pointer text-xs text-destructive hover:bg-destructive/10 hover:text-destructive"
            >
              <LogOut className="h-3.5 w-3.5" />
              <span>Sign Out</span>
            </Button>
          </div>
        </SheetContent>
      </Sheet>

      {/* Main Content Area */}
      <div className="flex flex-1 flex-col min-w-0">
        <TopBar
          userProfile={userProfile}
          onOpenCommand={() => setCommandPaletteOpen(true)}
          onCreateClick={() => onCreateClick?.()}
          onOpenMobileMenu={() => setMobileMenuOpen(true)}
        />

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <div className="mx-auto max-w-6xl space-y-6">
            {children}
          </div>
        </main>
      </div>

      {/* Global Command Palette (Cmd+K) */}
      <CommandPalette
        open={commandPaletteOpen}
        onOpenChange={setCommandPaletteOpen}
        links={links}
        onCreateClick={() => onCreateClick?.()}
        onSelectLink={onSelectLink}
      />
    </div>
  );
}
