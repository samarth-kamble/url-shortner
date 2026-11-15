"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getProfileAction,
  loginAction,
  logoutAction,
  registerAction,
  type LoginInput,
  type RegisterInput,
  type UserProfile,
} from "@/app/actions/auth-actions";
import { toast } from "sonner";
import { useRouter } from "next/navigation";

export const AUTH_QUERY_KEY = ["auth", "profile"] as const;

export function useAuth() {
  const queryClient = useQueryClient();
  const router = useRouter();

  // 1. Session Query
  const {
    data: user = null,
    isLoading,
    isError,
  } = useQuery<UserProfile | null>({
    queryKey: AUTH_QUERY_KEY,
    queryFn: async () => {
      return await getProfileAction();
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  // 2. Login Mutation
  const loginMutation = useMutation({
    mutationFn: async (credentials: LoginInput) => {
      const res = await loginAction(credentials);
      if (!res.success) {
        throw new Error(res.error || "Login failed");
      }
      return res.data;
    },
    onSuccess: (userData) => {
      queryClient.setQueryData(AUTH_QUERY_KEY, userData);
      toast.success("Welcome back!", {
        description: `Signed in as ${userData?.email}`,
      });
      router.push("/dashboard");
    },
    onError: (err: Error) => {
      toast.error("Authentication failed", {
        description: err.message,
      });
    },
  });

  // 3. Register Mutation
  const registerMutation = useMutation({
    mutationFn: async (data: RegisterInput) => {
      const res = await registerAction(data);
      if (!res.success) {
        throw new Error(res.error || "Registration failed");
      }
      return res.data;
    },
    onSuccess: (userData) => {
      queryClient.setQueryData(AUTH_QUERY_KEY, userData);
      toast.success("Account created successfully!", {
        description: `Welcome to ShortLink, ${userData?.fullName}`,
      });
      router.push("/dashboard");
    },
    onError: (err: Error) => {
      toast.error("Registration failed", {
        description: err.message,
      });
    },
  });

  // 4. Logout Mutation
  const logoutMutation = useMutation({
    mutationFn: async () => {
      await logoutAction();
    },
    onSuccess: () => {
      queryClient.setQueryData(AUTH_QUERY_KEY, null);
      queryClient.clear();
      toast.info("Signed out", {
        description: "You have been logged out of your session.",
      });
      router.push("/login");
    },
  });

  return {
    user,
    isLoading,
    isError,
    isAuthenticated: Boolean(user),
    login: loginMutation.mutateAsync,
    isLoggingIn: loginMutation.isPending,
    register: registerMutation.mutateAsync,
    isRegistering: registerMutation.isPending,
    logout: logoutMutation.mutateAsync,
    isLoggingOut: logoutMutation.isPending,
  };
}

