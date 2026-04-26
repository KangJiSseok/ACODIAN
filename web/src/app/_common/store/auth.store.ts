import { create } from "zustand";
import type { UserRoleCode } from "@/app/_common/constants/roles";

export type AuthStatus = "idle" | "loading" | "authenticated" | "unauthenticated";

export interface AuthUserTeam {
  isPrimary: boolean;
  teamId: number;
  teamName: string;
  teamLeader: boolean;
  teamRole: string;
  allocation: string | null;
}

export interface AuthUser {
  userId: number;
  userName: string;
  departmentId: number;
  departmentName: string;
  positionName: string | null;
  titleName: string | null;
  profileImageUrl: string | null;
  teams: AuthUserTeam[];
  roleCode?: UserRoleCode;
}

interface SetAuthPayload {
  accessToken: string | null;
  user: AuthUser | null;
}

interface AuthState {
  status: AuthStatus;
  accessToken: string | null;
  user: AuthUser | null;
  setStatus: (status: AuthStatus) => void;
  setAccessToken: (accessToken: string | null) => void;
  setUser: (user: AuthUser | null) => void;
  setAuth: (payload: SetAuthPayload) => void;
  resetAuth: () => void;
}

const initialAuthState = {
  status: "idle" as AuthStatus,
  accessToken: null,
  user: null,
};

export const useAuthStore = create<AuthState>()((set) => ({
  ...initialAuthState,
  setStatus: (status) => set({ status }),
  setAccessToken: (accessToken) => set({ accessToken }),
  setUser: (user) => set({ user }),
  setAuth: ({ accessToken, user }) =>
    set({
      accessToken,
      user,
      status: accessToken && user ? "authenticated" : "unauthenticated",
    }),
  resetAuth: () => set(initialAuthState),
}));

export const selectAuthUser = (state: AuthState) => state.user;

export const selectAccessToken = (state: AuthState) => state.accessToken;

export const selectIsAuthenticated = (state: AuthState) =>
  state.status === "authenticated" && Boolean(state.accessToken && state.user);
