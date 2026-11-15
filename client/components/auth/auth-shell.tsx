"use client";

import Link from "next/link";
import { Link2 } from "lucide-react";
import { ThemeToggle } from "@/components/theme-toggle";
import { HeroLinkAnimation } from "@/components/auth/hero-link-animation";

interface AuthShellProps {
  children: React.ReactNode;
}

export function AuthShell({ children }: AuthShellProps) {
  return (
    <div className="relative flex min-h-screen w-full flex-col lg:grid lg:grid-cols-2 lg:h-screen lg:overflow-hidden">
      {/* Top Controls: Mobile Logo + Universal Theme Switcher */}
      <div className="absolute top-4 right-4 z-50 flex items-center gap-3 sm:top-6 sm:right-6">
        <ThemeToggle />
      </div>

      {/* LEFT PANEL: Brand & Signature Visual (Desktop side-by-side, Mobile compact header) */}
      <div className="relative flex flex-col justify-between border-b border-border/60 bg-muted/20 p-6 sm:p-10 lg:border-b-0 lg:border-r lg:p-12 xl:p-16">
        {/* Subtle grid background */}
        <div className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(var(--border)_1px,transparent_1px)] [background-size:24px_24px] opacity-40" />

        {/* Brand Header */}
        <div className="flex items-center gap-3">
          <Link href="/" className="flex items-center gap-2.5 group">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary text-primary-foreground transition-transform group-hover:scale-105">
              <Link2 className="h-5 w-5" />
            </div>
            <span className="font-heading text-xl font-bold tracking-tight text-foreground">
              ShortLink
            </span>
          </Link>
        </div>

        {/* Center: Hero Statement & Signature Interactive Animation */}
        <div className="my-8 flex flex-col gap-8 lg:my-auto">
          <div className="space-y-3">
            <h1 className="font-heading text-3xl font-bold tracking-tight text-foreground sm:text-4xl lg:text-5xl">
              Sub-millisecond link routing. Built for scale.
            </h1>
            <p className="max-w-md text-sm text-muted-foreground sm:text-base leading-relaxed">
              Create memorable short links with instant redirects and real-time click analytics.
            </p>
          </div>

          <div className="hidden sm:block">
            <HeroLinkAnimation />
          </div>
        </div>

        {/* Bottom space retainer to balance layout cleanly without tech stack specs */}
        <div className="hidden lg:block h-6" />
      </div>

      {/* RIGHT PANEL: Form Card Container */}
      <div className="flex flex-1 items-center justify-center p-4 sm:p-6 lg:p-10 xl:p-14">
        <div className="w-full max-w-[550px]">
          {children}
        </div>
      </div>
    </div>
  );
}

