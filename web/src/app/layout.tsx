import type { Metadata } from "next";
import "./globals.css";
import { cn } from "@/lib/utils";
import QueryProvider from "@/app/_common/providers/queryProvider";

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
    <html
      lang="ko"
      data-scroll-behavior="smooth"
      className={cn("h-full", "font-sans")}
    >
      <body className="min-h-full bg-background text-foreground antialiased">
        {/* useQuery/useMutation을 어디서든 쓸 수 있게 앱 전체를 provider로 감쌉니다. */}
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
