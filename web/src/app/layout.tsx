import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";
import { cn } from "@/lib/utils";
import QueryProvider from "@/app/_common/providers/queryProvider";

const pretendard = localFont({
  src: "./_assets/fonts/PretendardVariable.woff2",
  variable: "--font-pretendard",
  weight: "45 920",
  display: "swap",
  preload: true,
  fallback: ["Apple SD Gothic Neo", "Malgun Gothic", "Segoe UI", "sans-serif"],
});

export const metadata: Metadata = {
  title: "ACODIAN",
  description: "ACODIAN (Ax Coworking Obsidian) 협업 업무 관리 서비스",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    // 전역 레이아웃은 폰트, 전역 스타일, HTML 기본 속성만 담당합니다.
    <html
      lang="ko"
      data-scroll-behavior="smooth"
      className={cn("h-full", "font-sans", pretendard.variable)}
    >
      <body className="min-h-full bg-background text-foreground antialiased">
        {/* useQuery/useMutation을 어디서든 쓸 수 있게 앱 전체를 provider로 감쌉니다. */}
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
