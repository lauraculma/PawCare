"use client";

import { usePathname, useRouter } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { ApiError, authApi } from "@/lib/api";
import type { LoginPayload, RegisterPayload, RegistrationResponse, User } from "@/lib/types";

type SessionContextValue = {
  user: User | null;
  loading: boolean;
  login: (payload: LoginPayload) => Promise<User>;
  register: (payload: RegisterPayload) => Promise<RegistrationResponse>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
};

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const pathname = usePathname();
  const router = useRouter();

  const refresh = useCallback(async () => {
    try {
      const current = await authApi.me();
      setUser(current);
    } catch (error) {
      setUser(null);

      if (error instanceof ApiError && error.status === 401) {
        // A stale HttpOnly cookie cannot be removed from browser JavaScript.
        // The public logout endpoint expires it using Set-Cookie.
        try {
          await authApi.logout();
        } catch {
          // Keep the login screen usable even if cookie cleanup fails.
        }

        const protectedPrefixes = [
          "/dashboard",
          "/mascotas",
          "/citas",
          "/agenda",
          "/pacientes",
          "/alertas",
          "/historial",
          "/admin",
        ];
        const isProtected = protectedPrefixes.some(
          (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
        );

        if (isProtected) {
          router.replace(`/login?next=${encodeURIComponent(pathname)}`);
        }
      }
    } finally {
      setLoading(false);
    }
  }, [pathname, router]);

  useEffect(() => {
    Promise.resolve().then(() => void refresh());
  }, [refresh]);

  const login = useCallback(async (payload: LoginPayload) => {
    const response = await authApi.login(payload);
    setUser(response.user);
    return response.user;
  }, []);

  const register = useCallback(async (payload: RegisterPayload): Promise<RegistrationResponse> => {
    const response: RegistrationResponse = await authApi.register(payload);
    if (!response.requiresApproval) {
      setUser(response.user);
    }
    return response;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, register, logout, refresh }),
    [user, loading, login, register, logout, refresh],
  );

  return (
    <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
  );
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession debe usarse dentro de SessionProvider");
  }
  return context;
}
