"use client";

import { useEffect, useState, useRef } from "react";
import { motion, useReducedMotion, AnimatePresence } from "motion/react";
import { Link2, ArrowDown, MousePointerClick, Globe } from "lucide-react";

export function HeroLinkAnimation() {
  const shouldReduceMotion = useReducedMotion();
  const sampleLongUrl = "https://example.com/products/summer-sale?utm_source=app&utm_campaign=launch";
  
  // Stages: "compressing" -> "active"
  const [stage, setStage] = useState<"compressing" | "active">(
    shouldReduceMotion ? "active" : "compressing"
  );
  const [clickCount, setClickCount] = useState(shouldReduceMotion ? 1284 : 0);
  const isVisibleRef = useRef(true);

  useEffect(() => {
    if (shouldReduceMotion) {
      return;
    }

    const handleVisibility = () => {
      isVisibleRef.current = document.visibilityState === "visible";
    };
    document.addEventListener("visibilitychange", handleVisibility);

    let isMounted = true;
    let cycleTimer: NodeJS.Timeout;
    let counterInterval: NodeJS.Timeout;

    const runCycle = () => {
      if (!isMounted) return;
      if (!isVisibleRef.current) {
        cycleTimer = setTimeout(runCycle, 1000);
        return;
      }

      // Step 1: Ingestion & Compressing phase
      setStage("compressing");
      setClickCount(0);

      // Step 2: Transition to Active Short Link after 2.4s
      cycleTimer = setTimeout(() => {
        if (!isMounted) return;
        setStage("active");

        // Step 3: Smooth click count roll up
        let current = 0;
        const target = 1284;
        const steps = 20;
        const stepTime = 40;
        let stepCount = 0;

        counterInterval = setInterval(() => {
          if (!isMounted) return;
          stepCount++;
          // Quadratic ease-out progression for buttery smooth counter
          const progress = stepCount / steps;
          const easeOut = 1 - Math.pow(1 - progress, 2);
          current = Math.round(target * easeOut);

          if (stepCount >= steps) {
            setClickCount(target);
            clearInterval(counterInterval);
            // Hold active state for 4s before restarting cycle
            cycleTimer = setTimeout(runCycle, 4000);
          } else {
            setClickCount(current);
          }
        }, stepTime);
      }, 2400);
    };

    runCycle();

    return () => {
      isMounted = false;
      document.removeEventListener("visibilitychange", handleVisibility);
      clearTimeout(cycleTimer);
      clearInterval(counterInterval);
    };
  }, [shouldReduceMotion]);

  return (
    <div className="relative flex w-full max-w-lg flex-col gap-5">
      {/* Top Status Header */}
      <div className="flex items-center justify-between font-mono text-xs text-muted-foreground">
        <div className="flex items-center gap-2">
          <span className="flex h-2 w-2 rounded-full bg-emerald-500" />
          <span className="text-foreground font-medium">Link Engine</span>
        </div>
        <span className="rounded border border-border/80 bg-muted/40 px-2.5 py-0.5 text-[11px] font-sans">
          Preview
        </span>
      </div>

      {/* Visual Canvas Container */}
      <div className="relative overflow-hidden rounded-xl border border-border/70 bg-card/60 p-6 shadow-xl backdrop-blur-md">
        {/* Subtle dot matrix background */}
        <div className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(var(--border)_1px,transparent_1px)] [background-size:16px_16px] opacity-30" />

        <div className="flex flex-col gap-4">
          {/* Section A: Long Incoming Destination URL */}
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <span className="font-mono text-[11px] uppercase tracking-wider text-muted-foreground">
                Destination URL
              </span>
              <span className="flex items-center gap-1 font-mono text-[10px] text-muted-foreground/80">
                <Globe className="h-3 w-3" />
                <span>Original</span>
              </span>
            </div>

            <div className="relative flex h-11 items-center rounded-lg border border-border/60 bg-background/60 px-3.5 font-mono text-xs text-muted-foreground overflow-hidden">
              <span className="truncate select-none font-normal">
                {sampleLongUrl}
              </span>
              {/* Hardware-accelerated smooth sweep bar during compression */}
              {stage === "compressing" && !shouldReduceMotion && (
                <motion.div
                  initial={{ x: "-100%" }}
                  animate={{ x: "100%" }}
                  transition={{
                    repeat: Infinity,
                    duration: 1.5,
                    ease: "easeInOut",
                  }}
                  className="pointer-events-none absolute inset-y-0 w-1/3 bg-gradient-to-r from-transparent via-primary/15 to-transparent"
                />
              )}
            </div>
          </div>

          {/* Morphing Connector */}
          <div className="flex items-center justify-center py-0.5">
            <motion.div
              animate={{
                scale: stage === "compressing" ? [1, 1.15, 1] : 1,
                opacity: stage === "compressing" ? 0.9 : 0.6,
              }}
              transition={{
                duration: 1.2,
                repeat: stage === "compressing" ? Infinity : 0,
                ease: "easeInOut",
              }}
              className="flex h-7 w-7 items-center justify-center rounded-full border border-border/80 bg-background/80 shadow-xs"
            >
              <ArrowDown className="h-3.5 w-3.5 text-primary" />
            </motion.div>
          </div>

          {/* Section B: Shortened Destination Link (No Copy Button, clean structure) */}
          <div className="flex flex-col gap-1.5">
            <div className="flex items-center justify-between">
              <span className="font-mono text-[11px] uppercase tracking-wider text-muted-foreground">
                Short Link
              </span>
              <span className="font-mono text-[10px] text-emerald-500 font-medium">
                Ready
              </span>
            </div>

            <motion.div
              animate={{
                borderColor: stage === "active" ? "var(--primary)" : "var(--border)",
              }}
              transition={{ duration: 0.3 }}
              className="relative flex h-12 items-center justify-between rounded-lg border border-border bg-background px-3.5 shadow-xs overflow-hidden"
            >
              <div className="flex items-center gap-2.5">
                <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary/10 text-primary">
                  <Link2 className="h-3.5 w-3.5" />
                </div>
                <div className="flex items-center font-mono text-sm font-semibold tracking-tight text-foreground">
                  <span className="text-muted-foreground/70">/s/</span>
                  <span>aB3xK9</span>
                </div>
              </div>

              {/* Status pill indicating active routing state without copy button */}
              <div className="flex items-center gap-1.5 rounded-md bg-muted/50 px-2 py-1 text-[11px] font-mono text-muted-foreground">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                <span>active</span>
              </div>
            </motion.div>
          </div>

          {/* Section C: Live Click Analytics Card */}
          <div className="flex items-center justify-between rounded-lg border border-border/40 bg-muted/20 px-3.5 py-2.5 font-mono text-xs">
            <div className="flex items-center gap-2 text-muted-foreground">
              <MousePointerClick className="h-3.5 w-3.5 text-primary" />
              <span>Real-Time Analytics</span>
            </div>
            <div className="flex items-center gap-1.5 text-foreground font-semibold">
              <AnimatePresence mode="wait">
                <motion.span
                  key={clickCount}
                  initial={{ opacity: 0.7, y: -1 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.15 }}
                >
                  {clickCount.toLocaleString()}
                </motion.span>
              </AnimatePresence>
              <span className="text-[11px] text-muted-foreground font-normal">clicks</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
