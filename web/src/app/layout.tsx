import type { Metadata } from "next";
import "./globals.css";
import { Geist } from "next/font/google";
import { cn } from "@/lib/utils";

const geist = Geist({subsets:['latin'],variable:'--font-sans'});

export const metadata: Metadata = {
  title: "AX-WMS",
  description: "AX-WMS 웹 애플리케이션 스캐폴드",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    // 전역 레이아웃은 폰트, 전역 스타일, HTML 기본 속성만 담당합니다.
    <html lang="ko" className={cn("h-full", "font-sans", geist.variable)}>
      <body className="min-h-full bg-background text-foreground antialiased">
        {children}
      </body>
    </html>
  );
}
