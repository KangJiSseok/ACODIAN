"use client";

import type { PropsWithChildren } from "react";
import { useEffect, useState } from "react";
import {
  QueryClient,
  QueryClientProvider,
  useQueryClient,
} from "@tanstack/react-query";
import { useAuthStore } from "@/app/_common/store/auth.store";

function ClearQueryCacheOnUnauthenticated() {
  const queryClient = useQueryClient();
  const status = useAuthStore((state) => state.status);

  useEffect(() => {
    if (status === "unauthenticated") {
      queryClient.clear();
    }
  }, [queryClient, status]);

  return null;
}

function createQueryClient() {
  // 전역에서 공통으로 쓸 React Query 기본 동작을 한곳에서 고정합니다.
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 1000 * 60,
        gcTime: 1000 * 60 * 10,
        retry: 1,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: 0,
      },
    },
  });
}

export default function QueryProvider({ children }: PropsWithChildren) {
  // 렌더링마다 새 client가 만들어지지 않도록 최초 한 번만 생성합니다.
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      <ClearQueryCacheOnUnauthenticated />
      {children}
    </QueryClientProvider>
  );
}
