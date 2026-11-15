"use client";

import * as React from "react";
import { useState } from "react";
import { Input } from "@/components/ui/input";
import { Eye, EyeOff } from "lucide-react";
import { cn } from "@/lib/utils";

interface PasswordInputProps
  extends Omit<React.ComponentProps<typeof Input>, "type"> {
  label?: string;
  error?: string;
}

export const PasswordInput = React.forwardRef<HTMLInputElement, PasswordInputProps>(
  ({ className, error, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false);
    const [capsLockActive, setCapsLockActive] = useState(false);

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
      setCapsLockActive(e.getModifierState("CapsLock"));
      props.onKeyDown?.(e);
    };

    return (
      <div className="space-y-1.5">
        <div className="relative">
          <Input
            ref={ref}
            type={showPassword ? "text" : "password"}
            onKeyDown={handleKeyDown}
            onKeyUp={(e) => setCapsLockActive(e.getModifierState("CapsLock"))}
            className={cn(
              "h-11 sm:h-12 pr-10 text-sm tracking-tight",
              error && "border-destructive focus-visible:ring-destructive",
              className
            )}
            {...props}
          />
          <button
            type="button"
            tabIndex={-1}
            onClick={() => setShowPassword((prev) => !prev)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? (
              <EyeOff className="h-4 w-4" />
            ) : (
              <Eye className="h-4 w-4" />
            )}
          </button>
        </div>

        {capsLockActive && (
          <p className="font-mono text-[11px] font-medium text-amber-500 animate-pulse">
            Caps Lock is active
          </p>
        )}

        {error && (
          <p className="text-xs text-destructive animate-in fade-in slide-in-from-top-1 duration-200">
            {error}
          </p>
        )}
      </div>
    );
  }
);

PasswordInput.displayName = "PasswordInput";

