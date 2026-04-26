import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

export const THEME_MODES = ["light", "dark"] as const;

export type ThemeMode = (typeof THEME_MODES)[number];

interface UiState {
  themeMode: ThemeMode;
  setThemeMode: (themeMode: ThemeMode) => void;
  resetUi: () => void;
}

const initialUiState = {
  themeMode: "light" as ThemeMode,
};

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      ...initialUiState,
      setThemeMode: (themeMode) => set({ themeMode }),
      resetUi: () => set(initialUiState),
    }),
    {
      name: "ax-wms-ui",
      storage: createJSONStorage(() => localStorage),
      partialize: ({ themeMode }) => ({
        themeMode,
      }),
    },
  ),
);
