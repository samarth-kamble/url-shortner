"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { loginSchema, type LoginFormData } from "@/lib/auth-schemas";
import { AuthCard } from "@/components/auth/auth-card";
import { PasswordInput } from "@/components/auth/password-input";
import { SubmitButton } from "@/components/auth/submit-button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { loginAction } from "@/app/actions/auth-actions";
import { toast } from "sonner";

export default function LoginPage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [shake, setShake] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
      rememberMe: false,
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsSubmitting(true);
    setShake(false);

    try {
      const res = await loginAction({
        email: data.email,
        password: data.password,
      });

      if (!res.success) {
        throw new Error(res.error || "Invalid email or password");
      }

      setIsSuccess(true);
      toast.success("Welcome back!", {
        description: `Signed in as ${data.email}`,
      });

      setTimeout(() => {
        router.push("/dashboard");
        router.refresh();
      }, 700);
    } catch (err: unknown) {
      setShake(true);
      const msg = err instanceof Error ? err.message : "Authentication failed";
      toast.error("Sign in failed", { description: msg });
      setTimeout(() => setShake(false), 500);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthCard shake={shake}>
      {/* Header */}
      <div className="space-y-1.5 pb-5 text-center sm:text-left">
        <h2 className="font-heading text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
          Welcome back
        </h2>
        <p className="text-sm text-muted-foreground leading-relaxed">
          Enter your credentials to access your link analytics
        </p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* Email Field */}
        <div className="space-y-2">
          <label htmlFor="email" className="text-sm font-medium text-foreground">
            Email address
          </label>
          <Input
            id="email"
            type="email"
            placeholder="engineer@shortlink.io"
            autoComplete="email"
            disabled={isSubmitting || isSuccess}
            className="h-11 sm:h-12 text-sm"
            {...register("email")}
          />
          {errors.email && (
            <p className="text-xs text-destructive animate-in fade-in slide-in-from-top-1">
              {errors.email.message}
            </p>
          )}
        </div>

        {/* Password Field */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <label htmlFor="password" className="text-sm font-medium text-foreground">
              Password
            </label>
            <Link
              href="/forgot-password"
              className="text-sm font-medium text-primary hover:text-primary/80 transition-colors"
            >
              Forgot password?
            </Link>
          </div>
          <PasswordInput
            id="password"
            placeholder="••••••••••••"
            autoComplete="current-password"
            disabled={isSubmitting || isSuccess}
            error={errors.password?.message}
            {...register("password")}
          />
        </div>

        {/* Remember Me */}
        <div className="flex items-center gap-2.5 pt-1">
          <Controller
            control={control}
            name="rememberMe"
            render={({ field }) => (
              <Checkbox
                id="rememberMe"
                checked={field.value}
                onCheckedChange={field.onChange}
                className="cursor-pointer"
              />
            )}
          />
          <label
            htmlFor="rememberMe"
            className="text-sm text-muted-foreground cursor-pointer select-none"
          >
            Remember me on this device for 30 days
          </label>
        </div>

        {/* Submit Button */}
        <div className="pt-2">
          <SubmitButton
            isLoading={isSubmitting}
            isSuccess={isSuccess}
            successText="Authenticated"
          >
            Sign In
          </SubmitButton>
        </div>
      </form>

      {/* Footer Link */}
      <div className="mt-6 text-center text-sm text-muted-foreground">
        Don&apos;t have an account?{" "}
        <Link
          href="/sign-up"
          className="font-semibold text-foreground underline underline-offset-4 hover:text-primary transition-colors"
        >
          Sign up
        </Link>
      </div>
    </AuthCard>
  );
}

