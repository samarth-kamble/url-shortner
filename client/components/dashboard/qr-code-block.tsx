"use client";

import { useEffect, useRef, useState } from "react";
import QRCode from "qrcode";
import { Button } from "@/components/ui/button";
import { Download, Check, Copy } from "lucide-react";
import { toast } from "sonner";

interface QrCodeBlockProps {
  url: string;
  size?: number;
}

export function QrCodeBlock({ url, size = 200 }: QrCodeBlockProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [svgString, setSvgString] = useState<string>("");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!url) return;

    // Render Canvas for visual and PNG export
    if (canvasRef.current) {
      QRCode.toCanvas(canvasRef.current, url, {
        width: size,
        margin: 2,
        color: {
          dark: "#0B1220",
          light: "#FFFFFF",
        },
      }).catch((err) => console.error("QR canvas render failed:", err));
    }

    // Generate SVG string for vector export
    QRCode.toString(url, {
      type: "svg",
      width: size,
      margin: 2,
      color: {
        dark: "#0B1220",
        light: "#FFFFFF",
      },
    })
      .then((svg) => setSvgString(svg))
      .catch((err) => console.error("QR SVG render failed:", err));
  }, [url, size]);

  const handleDownloadPng = () => {
    if (!canvasRef.current) return;
    const dataUrl = canvasRef.current.toDataURL("image/png");
    const link = document.createElement("a");
    link.download = `shortlink-qr-${Date.now()}.png`;
    link.href = dataUrl;
    link.click();
    toast.success("QR code PNG downloaded");
  };

  const handleDownloadSvg = () => {
    if (!svgString) return;
    const blob = new Blob([svgString], { type: "image/svg+xml" });
    const link = document.createElement("a");
    link.download = `shortlink-qr-${Date.now()}.svg`;
    link.href = URL.createObjectURL(blob);
    link.click();
    toast.success("QR code SVG downloaded");
  };

  const handleCopyLink = () => {
    navigator.clipboard?.writeText(url);
    setCopied(true);
    toast.success("Short link copied to clipboard");
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex flex-col items-center gap-4 p-2">
      {/* QR Canvas Card */}
      <div className="relative rounded-xl border border-border/80 bg-white p-3 shadow-md">
        <canvas ref={canvasRef} className="block rounded-lg" />
      </div>

      <div className="flex items-center gap-1.5 font-mono text-xs text-muted-foreground truncate max-w-xs">
        <span className="truncate">{url}</span>
      </div>

      {/* Action Buttons */}
      <div className="flex w-full items-center gap-2 pt-1">
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleCopyLink}
          className="flex-1 gap-1.5 cursor-pointer text-xs"
        >
          {copied ? <Check className="h-3.5 w-3.5 text-emerald-500" /> : <Copy className="h-3.5 w-3.5" />}
          <span>{copied ? "Copied" : "Copy"}</span>
        </Button>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleDownloadPng}
          className="flex-1 gap-1.5 cursor-pointer text-xs"
        >
          <Download className="h-3.5 w-3.5" />
          <span>PNG</span>
        </Button>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleDownloadSvg}
          className="flex-1 gap-1.5 cursor-pointer text-xs"
        >
          <Download className="h-3.5 w-3.5" />
          <span>SVG</span>
        </Button>
      </div>
    </div>
  );
}

