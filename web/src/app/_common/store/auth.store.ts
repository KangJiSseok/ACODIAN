import { create } from "zustand";

// 인증 흐름의 단계만 표현하고, 실제 API 호출은 이후 service/hook 계층에서 붙입니다.
export type AuthStatus = "idle" | "loading" | "authenticated" | "unauthenticated";

// /api/users/me 응답의 teams[] 문맥을 프론트에서 사용하는 형태로 둡니다.
export interface AuthUserTeam {
  isPrimary: boolean;
  teamId: number;
  teamName: string;
  isLeader: boolean;
  teamRole: string;
  allocation: string | null;
}

// 현재 사용자 문맥은 /api/users/me 응답을 기준으로 그대로 보관합니다.
export interface AuthUser {
  userId: number;
  userName: string;
  email: string;
  phone: string | null;
  departmentId: number | null;
  departmentName: string;
  positionName: string | null;
  titleName: string | null;
  joinDate: string | null;
  // 백엔드 EmploymentStatus enum(ACTIVE, LEAVE, RETIRED)을 기준으로 저장한다.
  employmentStatus: "ACTIVE" | "LEAVE" | "RETIRED" | string;
  profileImageUrl: string | null;
  teams: AuthUserTeam[];
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

// accessToken은 localStorage에 저장하지 않고 현재 탭의 메모리 상태에만 유지합니다.
export const useAuthStore = create<AuthState>()((set) => ({
  ...initialAuthState,
  setStatus: (status) => set({ status }),
  setAccessToken: (accessToken) => set({ accessToken }),
  setUser: (user) => set({ user }),
  // 로그인/refresh 이후 accessToken과 사용자 문맥을 한 번에 반영하는 진입점입니다.
  setAuth: ({ accessToken, user }) =>
    set({
      accessToken,
      user,
      status: accessToken && user ? "authenticated" : "unauthenticated",
    }),
  resetAuth: () => set(initialAuthState),
}));

// 컴포넌트가 필요한 조각만 구독할 수 있도록 selector를 분리합니다.
export const selectAuthUser = (state: AuthState) => state.user;

export const selectAccessToken = (state: AuthState) => state.accessToken;

export const selectIsAuthenticated = (state: AuthState) =>
  state.status === "authenticated" && Boolean(state.accessToken && state.user);
