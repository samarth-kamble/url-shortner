"use client";

import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Search,
  Plus,
  Menu,
} from "lucide-react";
import { logoutAction, UserProfile } from "@/app/actions/auth-actions";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

interface TopBarProps {
  userProfile?: UserProfile | null;
  onOpenCommand: () => void;
  onCreateClick: () => void;
  onOpenMobileMenu?: () => void;
}

export function TopBar({
  userProfile,
  onOpenCommand,
  onCreateClick,
  onOpenMobileMenu,
}: TopBarProps) {
  const router = useRouter();

  const handleLogout = async () => {
    await logoutAction();
    toast.success("Signed out successfully");
    router.push("/login");
  };

  const displayName = userProfile?.fullName || (userProfile?.email ? userProfile.email.split("@")[0] : "Developer");
  const displayEmail = userProfile?.email || "developer@shortlink.io";
  const userInitial = (displayName[0] || "U").toUpperCase();
  const accountId = userProfile?.id ? `usr_${userProfile.id}slnk` : "o_7mqf0l2tdl";

  return (
    <header className="sticky top-0 z-30 flex h-14 w-full items-center justify-between border-b border-border/80 bg-background/80 px-4 sm:px-6 backdrop-blur-md">
      {/* Left: Mobile menu button & breadcrumbs */}
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={onOpenMobileMenu}
          className="lg:hidden cursor-pointer h-8 w-8 text-muted-foreground"
        >
          <Menu className="h-4 w-4" />
        </Button>

        <div className="flex items-center gap-2 font-mono text-xs">
          <span className="text-muted-foreground font-sans font-medium">Workspace</span>
          <span className="text-muted-foreground/60">/</span>
          <span className="font-semibold text-foreground">Links</span>
        </div>
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Cmd+K Search Bar Trigger */}
        <button
          type="button"
          onClick={onOpenCommand}
          className="flex h-9 w-40 sm:w-64 items-center justify-between rounded-lg border border-border/70 bg-muted/40 px-3 text-xs text-muted-foreground transition-all hover:border-border hover:bg-muted/70 cursor-pointer"
        >
          <span className="flex items-center gap-2 truncate">
            <Search className="h-3.5 w-3.5 shrink-0" />
            <span className="truncate">Search links...</span>
          </span>
          <kbd className="hidden sm:inline-flex items-center gap-0.5 rounded border border-border/80 bg-background px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground">
            <span className="text-[10px]">⌘</span>K
          </kbd>
        </button>

        {/* Create Link Header Button */}
        <Button
          onClick={onCreateClick}
          size="sm"
          className="gap-1.5 cursor-pointer h-9 px-3 text-xs font-semibold shadow-xs"
        >
          <Plus className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Create link</span>
          <kbd className="hidden md:inline-block ml-1 rounded bg-primary-foreground/20 px-1 py-0.2 text-[9px] font-mono">
            C
          </kbd>
        </Button>

        {/* Theme Switcher */}
        <ThemeToggle />

        {/* User Avatar & Dropdown Menu (Matches Screenshot 1:1) */}
        <DropdownMenu>
          <DropdownMenuTrigger className="h-8 w-8 rounded-full border border-border/80 cursor-pointer p-0 inline-flex items-center justify-center overflow-hidden hover:opacity-85 transition-opacity">
            <div className="flex h-full w-full items-center justify-center rounded-full bg-primary/15 text-primary font-mono text-xs font-bold">
              {userInitial}
            </div>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            align="end"
            className="!w-72 sm:!w-80 w-72 sm:w-80 p-0 shadow-2xl border-border/80 bg-popover rounded-2xl overflow-hidden"
          >
            {/* Header: Avatar, Name, Email */}
            <div className="flex items-center gap-3.5 p-4 border-b border-border/60">
              <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-muted font-heading font-bold text-base text-foreground border border-border/70 shadow-xs">
                {userInitial}
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-foreground truncate">{displayName}</p>
                <p className="text-xs text-muted-foreground truncate">{displayEmail}</p>
              </div>
            </div>

            {/* Account Tier: ID + Free account + Upgrade Button */}
            <div className="flex items-center justify-between p-3.5 border-b border-border/60 bg-muted/20">
              <div className="min-w-0 flex-1 pr-2">
                <p className="text-xs font-mono font-medium text-foreground truncate">
                  {accountId}
                </p>
                <p className="text-[11px] text-muted-foreground">Free account</p>
              </div>
              <Button
                size="sm"
                onClick={() => router.push("/settings")}
                className="h-7 px-3 text-xs font-semibold rounded-md bg-[#007b8a] hover:bg-[#006672] text-white cursor-pointer shadow-xs"
              >
                Upgrade
              </Button>
            </div>

            {/* Options matching screenshot */}
            <div className="py-1">
              <DropdownMenuItem
                onClick={() => toast.info("Support portal available in settings")}
                className="cursor-pointer px-4 py-2.5 text-xs font-medium text-foreground hover:bg-muted/60 transition-colors"
              >
                <span>Support</span>
              </DropdownMenuItem>

              <DropdownMenuItem
                onClick={() => router.push("/settings")}
                className="cursor-pointer px-4 py-2.5 text-xs font-medium text-foreground hover:bg-muted/60 transition-colors"
              >
                <span>API documentation</span>
              </DropdownMenuItem>

              <DropdownMenuItem
                onClick={() => toast.info("ShortLink Terms of Service v1.0 active")}
                className="cursor-pointer px-4 py-2.5 text-xs font-medium text-foreground hover:bg-muted/60 transition-colors"
              >
                <span>ShortLink Terms</span>
              </DropdownMenuItem>
            </div>

            <DropdownMenuSeparator className="my-0" />

            {/* Sign Out */}
            <div className="p-1">
              <DropdownMenuItem
                onClick={handleLogout}
                className="cursor-pointer px-4 py-2.5 text-xs font-medium text-foreground hover:bg-destructive/10 hover:text-destructive transition-colors"
              >
                <span>Sign out</span>
              </DropdownMenuItem>
            </div>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
