import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

// 현재 GNB 구현이 지원하는 light/dark 테마만 전역 UI 상태로 관리합니다.
export const THEME_MODES = ["light", "dark"] as const;

export type ThemeMode = (typeof THEME_MODES)[number];

interface UiState {
  themeMode: ThemeMode;
  sidebarCollapsed: boolean;
  setThemeMode: (themeMode: ThemeMode) => void;
  setSidebarCollapsed: (sidebarCollapsed: boolean) => void;
  toggleSidebarCollapsed: () => void;
  resetUi: () => void;
}

const initialUiState = {
  themeMode: "light" as ThemeMode,
  sidebarCollapsed: false,
};

// 테마 선택은 민감정보가 아니므로 새로고침 후에도 유지되도록 localStorage에 저장합니다.
export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      ...initialUiState,
      setThemeMode: (themeMode) => set({ themeMode }),
      setSidebarCollapsed: (sidebarCollapsed) => set({ sidebarCollapsed }),
      toggleSidebarCollapsed: () =>
        set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
      resetUi: () => set(initialUiState),
    }),
    {
      name: "ax-wms-ui",
      storage: createJSONStorage(() => localStorage),
      partialize: ({ themeMode, sidebarCollapsed }) => ({
        themeMode,
        sidebarCollapsed,
      }),
    },
  ),
);
