"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  Command,
  CommandDialog,
  CommandInput,
  CommandList,
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandSeparator,
} from "@/components/ui/command";
import { LinkItem } from "@/lib/link-schemas";
import { logoutAction } from "@/app/actions/auth-actions";
import { useTheme } from "next-themes";
import {
  Link2,
  Plus,
  BarChart2,
  Settings,
  SunMoon,
  LogOut,
} from "lucide-react";
import { toast } from "sonner";

interface CommandPaletteProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  links?: LinkItem[];
  onCreateClick?: () => void;
  onSelectLink?: (link: LinkItem) => void;
}

export function CommandPalette({
  open,
  onOpenChange,
  links = [],
  onCreateClick,
  onSelectLink,
}: CommandPaletteProps) {
  const router = useRouter();
  const { theme, setTheme } = useTheme();

  useEffect(() => {
    const down = (e: KeyboardEvent) => {
      if (e.key === "k" && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        onOpenChange(!open);
      }
    };

    document.addEventListener("keydown", down);
    return () => document.removeEventListener("keydown", down);
  }, [open, onOpenChange]);

  const handleLogout = async () => {
    onOpenChange(false);
    await logoutAction();
    toast.success("Signed out");
    router.push("/login");
  };

  return (
    <CommandDialog open={open} onOpenChange={onOpenChange} className="sm:max-w-xl md:max-w-2xl w-full">
      <Command className="border-0 shadow-none bg-transparent">
        <CommandInput placeholder="Type a command or search links..." />
        <CommandList className="max-h-80">
          <CommandEmpty>No results found.</CommandEmpty>

        {/* Quick Actions */}
        <CommandGroup heading="Actions">
          <CommandItem
            onSelect={() => {
              onOpenChange(false);
              onCreateClick?.();
            }}
            className="cursor-pointer gap-2"
          >
            <Plus className="h-4 w-4 text-primary" />
            <span>Create new short link</span>
            <kbd className="ml-auto text-[10px] font-mono text-muted-foreground">C</kbd>
          </CommandItem>
          <CommandItem
            onSelect={() => {
              setTheme(theme === "dark" ? "light" : "dark");
              onOpenChange(false);
            }}
            className="cursor-pointer gap-2"
          >
            <SunMoon className="h-4 w-4" />
            <span>Toggle theme (Dark / Light)</span>
          </CommandItem>
        </CommandGroup>

        <CommandSeparator />

        {/* Navigation */}
        <CommandGroup heading="Navigation">
          <CommandItem
            onSelect={() => {
              router.push("/dashboard");
              onOpenChange(false);
            }}
            className="cursor-pointer gap-2"
          >
            <Link2 className="h-4 w-4" />
            <span>Links</span>
          </CommandItem>
          <CommandItem
            onSelect={() => {
              router.push("/analytics");
              onOpenChange(false);
            }}
            className="cursor-pointer gap-2"
          >
            <BarChart2 className="h-4 w-4" />
            <span>Analytics</span>
          </CommandItem>
          <CommandItem
            onSelect={() => {
              router.push("/settings");
              onOpenChange(false);
            }}
            className="cursor-pointer gap-2"
          >
            <Settings className="h-4 w-4" />
            <span>Settings</span>
          </CommandItem>
        </CommandGroup>

        {/* Links List Search */}
        {links.length > 0 && (
          <>
            <CommandSeparator />
            <CommandGroup heading="Your Short Links">
              {links.map((link) => (
                <CommandItem
                  key={link.shortCode}
                  onSelect={() => {
                    onOpenChange(false);
                    onSelectLink?.(link);
                  }}
                  className="cursor-pointer gap-3 py-2 px-3 rounded-lg"
                >
                  <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-primary/10 text-primary font-mono text-xs font-bold">
                    {link.shortCode.charAt(0).toUpperCase()}
                  </div>
                  <span className="font-mono text-xs font-semibold text-foreground shrink-0">
                    /{link.shortCode}
                  </span>
                  <span className="text-xs text-muted-foreground truncate max-w-sm flex-1">
                    {link.longUrl}
                  </span>
                  <span className="ml-auto text-[11px] font-mono text-muted-foreground whitespace-nowrap bg-muted/50 px-2 py-0.5 rounded-md">
                    {link.clickCount} {link.clickCount === 1 ? "click" : "clicks"}
                  </span>
                </CommandItem>
              ))}
            </CommandGroup>
          </>
        )}

        <CommandSeparator />

        {/* Account */}
        <CommandGroup heading="Account">
          <CommandItem onSelect={handleLogout} className="cursor-pointer gap-2 text-destructive">
            <LogOut className="h-4 w-4" />
            <span>Sign Out</span>
          </CommandItem>
        </CommandGroup>
      </CommandList>
      </Command>
    </CommandDialog>
  );
}
