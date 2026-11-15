"use client";

import { motion } from "motion/react";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface SubmitButtonProps extends React.ComponentProps<typeof Button> {
  isLoading?: boolean;
  isSuccess?: boolean;
  successText?: string;
}

export function SubmitButton({
  children,
  isLoading = false,
  isSuccess = false,
  successText = "Success",
  className,
  disabled,
  ...props
}: SubmitButtonProps) {
  return (
    <Button
      type="submit"
      disabled={isLoading || isSuccess || disabled}
      className={cn(
        "h-11 sm:h-12 w-full font-semibold tracking-tight text-sm sm:text-base transition-all duration-200 cursor-pointer disabled:cursor-not-allowed",
        isSuccess ? "bg-emerald-600 text-white hover:bg-emerald-600" : "bg-primary text-primary-foreground hover:bg-primary/90",
        className
      )}
      {...props}
    >
      {isSuccess ? (
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="flex items-center gap-2"
        >
          <svg
            className="h-4 w-4 stroke-current fill-none"
            viewBox="0 0 24 24"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <motion.path
              d="M20 6L9 17l-5-5"
              initial={{ pathLength: 0 }}
              animate={{ pathLength: 1 }}
              transition={{ duration: 0.35, ease: "easeOut" }}
            />
          </svg>
          <span>{successText}</span>
        </motion.div>
      ) : isLoading ? (
        <div className="flex items-center gap-2">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span>Processing...</span>
        </div>
      ) : (
        children
      )}
    </Button>
  );
}

