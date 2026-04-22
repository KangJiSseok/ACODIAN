import { useSyncExternalStore } from "react"
import {
  getCurrentUser,
  loginAsUser,
  logout,
  refreshCurrentUser,
  subscribe,
} from "../_store/authStore"

export function useAuth() {
  const user = useSyncExternalStore(subscribe, getCurrentUser, getCurrentUser)

  return {
    user,
    login: loginAsUser,
    logout,
    refreshUser: refreshCurrentUser,
    isAuthenticated: Boolean(user),
  }
}
