"use client";

import Link from "next/link";
import { Link2 } from "lucide-react";
import { ThemeToggle } from "@/components/theme-toggle";
import { useAuth } from "@/hooks/use-auth";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export function Navbar() {
  const { user, isAuthenticated, logout, isLoggingOut } = useAuth();

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border/60 bg-background/80 backdrop-blur-md">
      <div className="container mx-auto flex h-14 max-w-6xl items-center justify-between px-4 sm:px-6">
        {/* Brand */}
        <div className="flex items-center gap-6">
          <Link
            href="/"
            className="flex items-center gap-2.5 font-semibold tracking-tight text-foreground"
          >
            <div className="flex h-7 w-7 items-center justify-center rounded-md bg-foreground text-background">
              <Link2 className="h-4 w-4" />
            </div>
            <span className="font-mono text-sm font-bold tracking-tight">ShortLink</span>
          </Link>

          {/* Telemetry Status Beacon */}
          <div className="hidden items-center gap-2 sm:flex">
            <Badge variant="outline" className="gap-1.5 border-border bg-muted/30 font-mono text-[11px] font-normal text-muted-foreground">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
              <span>Nginx Cluster</span>
              <span className="text-border">|</span>
              <span className="text-foreground font-medium">&lt;1ms</span>
            </Badge>
          </div>
        </div>

        {/* Right Nav */}
        <div className="flex items-center gap-3">
          <ThemeToggle />

          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <Link href="/dashboard">
                <Button variant="ghost" size="sm" className="text-xs">
                  Dashboard
                </Button>
              </Link>
              <span className="hidden font-mono text-xs text-muted-foreground sm:inline-block">
                {user?.email}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => logout()}
                disabled={isLoggingOut}
                className="text-xs"
              >
                Sign out
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link href="/login">
                <Button variant="ghost" size="sm" className="text-xs text-muted-foreground hover:text-foreground">
                  Sign In
                </Button>
              </Link>
              <Link href="/sign-up">
                <Button size="sm" className="text-xs">
                  Get Started
                </Button>
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

