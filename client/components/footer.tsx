import Link from "next/link";
import { Cpu, Globe, Server } from "lucide-react";

export function Footer() {
  return (
    <footer className="w-full border-t border-border/50 bg-background/50 py-8 text-xs text-muted-foreground">
      <div className="container mx-auto flex max-w-6xl flex-col justify-between gap-6 px-4 sm:flex-row sm:items-center sm:px-6">
        {/* Left: Engine identity */}
        <div className="flex flex-col gap-1.5">
          <div className="flex items-center gap-2 text-foreground font-mono font-medium">
            <span>ShortLink Engine</span>
            <span className="text-[10px] text-muted-foreground bg-muted/60 px-1.5 py-0.5 rounded-sm">v1.0-prod</span>
          </div>
          <p className="text-muted-foreground">
            Distributed URL Redirection &bull; Snowflake ID Gen &bull; Kafka Stream
          </p>
        </div>

        {/* Center: System specs */}
        <div className="flex items-center gap-4 font-mono text-[11px] text-muted-foreground">
          <div className="flex items-center gap-1.5">
            <Server className="h-3 w-3 text-emerald-500" />
            <span>Postgres Shards: 2</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Globe className="h-3 w-3 text-emerald-500" />
            <span>L1/L2 Cache: Active</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Cpu className="h-3 w-3 text-emerald-500" />
            <span>Nginx L7: Least-Conn</span>
          </div>
        </div>

        {/* Right: Legal & Terms */}
        <div className="flex items-center gap-4">
          <Link href="/terms" className="hover:text-foreground transition-colors">
            Terms
          </Link>
          <Link href="/privacy" className="hover:text-foreground transition-colors">
            Privacy
          </Link>
          <span className="text-border">|</span>
          <span className="font-mono text-[11px]">SLA &lt;1ms</span>
        </div>
      </div>
    </footer>
  );
}

