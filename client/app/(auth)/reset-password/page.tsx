"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  resetPasswordSchema,
  type ResetPasswordFormData,
} from "@/lib/auth-schemas";
import { AuthCard } from "@/components/auth/auth-card";
import { PasswordInput } from "@/components/auth/password-input";
import { SubmitButton } from "@/components/auth/submit-button";
import { ArrowLeft, KeyRound } from "lucide-react";
import { toast } from "sonner";

export default function ResetPasswordPage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [shake, setShake] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      password: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async () => {
    setIsSubmitting(true);
    setShake(false);

    try {
      // Simulate API latency (TODO: Call backend password-update endpoint)
      await new Promise((resolve) => setTimeout(resolve, 800));

      setIsSuccess(true);
      toast.success("Password updated!", {
        description: "Your new password has been saved. Please sign in.",
      });

      setTimeout(() => {
        router.push("/login");
      }, 700);
    } catch {
      setShake(true);
      toast.error("Update failed", {
        description: "Invalid or expired token.",
      });
      setTimeout(() => setShake(false), 500);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthCard shake={shake}>
      <div className="space-y-2 pb-6 text-center sm:text-left">
        <div className="flex items-center gap-2.5 mb-1 justify-center sm:justify-start">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-muted/60 text-foreground">
            <KeyRound className="h-4 w-4" />
          </div>
          <h2 className="font-heading text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Set new password
          </h2>
        </div>
        <p className="text-sm text-muted-foreground leading-relaxed">
          Please choose a strong, unique password for your account
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* New Password */}
        <div className="space-y-2">
          <label
            htmlFor="password"
            className="text-sm font-medium text-foreground"
          >
            New password
          </label>
          <PasswordInput
            id="password"
            placeholder="••••••••••••"
            autoComplete="new-password"
            disabled={isSubmitting || isSuccess}
            error={errors.password?.message}
            {...register("password")}
          />
        </div>

        {/* Confirm Password */}
        <div className="space-y-2 pt-1">
          <label
            htmlFor="confirmPassword"
            className="text-sm font-medium text-foreground"
          >
            Confirm new password
          </label>
          <PasswordInput
            id="confirmPassword"
            placeholder="••••••••••••"
            autoComplete="new-password"
            disabled={isSubmitting || isSuccess}
            error={errors.confirmPassword?.message}
            {...register("confirmPassword")}
          />
        </div>

        <div className="pt-2">
          <SubmitButton
            isLoading={isSubmitting}
            isSuccess={isSuccess}
            successText="Password Changed"
          >
            Update Password
          </SubmitButton>
        </div>
      </form>

      <div className="mt-6 text-center text-sm text-muted-foreground">
        <Link
          href="/login"
          className="inline-flex items-center gap-1.5 font-medium text-foreground hover:text-primary transition-colors"
        >
          <ArrowLeft className="h-4 w-4" /> Back to sign in
        </Link>
      </div>
    </AuthCard>
  );
}
