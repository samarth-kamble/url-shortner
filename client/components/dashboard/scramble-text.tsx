"use client";

import { useEffect, useState } from "react";
import { useReducedMotion } from "motion/react";

interface ScrambleTextProps {
  text: string;
  duration?: number;
  className?: string;
  onComplete?: () => void;
}

const GLYPHS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

export function ScrambleText({
  text,
  duration = 600,
  className,
  onComplete,
}: ScrambleTextProps) {
  const shouldReduceMotion = useReducedMotion();
  const [displayText, setDisplayText] = useState(text);

  useEffect(() => {
    if (shouldReduceMotion) {
      const frame = requestAnimationFrame(() => {
        setDisplayText(text);
        onComplete?.();
      });
      return () => cancelAnimationFrame(frame);
    }

    let frameId: number;
    const startTime = performance.now();
    const length = text.length;

    const animate = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const lockedChars = Math.floor(progress * length);

      let scrambled = "";
      for (let i = 0; i < length; i++) {
        if (i < lockedChars) {
          scrambled += text[i];
        } else {
          scrambled += GLYPHS[Math.floor(Math.random() * GLYPHS.length)];
        }
      }

      setDisplayText(scrambled);

      if (progress < 1) {
        frameId = requestAnimationFrame(animate);
      } else {
        setDisplayText(text);
        onComplete?.();
      }
    };

    frameId = requestAnimationFrame(animate);

    return () => cancelAnimationFrame(frameId);
  }, [text, duration, shouldReduceMotion, onComplete]);

  return <span className={className}>{displayText}</span>;
}
