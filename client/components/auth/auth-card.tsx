"use client";

import { motion, useReducedMotion } from "motion/react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface AuthCardProps {
  children: React.ReactNode;
  className?: string;
  shake?: boolean;
}

export function AuthCard({ children, className, shake = false }: AuthCardProps) {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.div
      initial={shouldReduceMotion ? { opacity: 0 } : { opacity: 0, y: 16 }}
      animate={
        shake && !shouldReduceMotion
          ? {
              x: [-8, 8, -6, 6, -3, 3, 0],
              opacity: 1,
              y: 0,
              transition: { duration: 0.4 },
            }
          : {
              opacity: 1,
              y: 0,
              transition: { duration: 0.35, ease: "easeOut" },
            }
      }
      className="w-full"
    >
      <Card
        className={cn(
          "w-full border-border/80 bg-card/95 px-7 py-6 sm:px-10 sm:py-8 shadow-2xl backdrop-blur-sm rounded-2xl",
          className
        )}
      >
        {children}
      </Card>
    </motion.div>
  );
}

