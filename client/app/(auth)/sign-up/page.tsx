"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { signUpSchema, type SignUpFormData } from "@/lib/auth-schemas";
import { AuthCard } from "@/components/auth/auth-card";
import { PasswordInput } from "@/components/auth/password-input";
import { SubmitButton } from "@/components/auth/submit-button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { registerAction } from "@/app/actions/auth-actions";
import { toast } from "sonner";

export default function SignUpPage() {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [shake, setShake] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  } = useForm<SignUpFormData>({
    resolver: zodResolver(signUpSchema),
    defaultValues: {
      fullName: "",
      email: "",
      password: "",
      terms: false,
    },
  });

  const onSubmit = async (data: SignUpFormData) => {
    setIsSubmitting(true);
    setShake(false);

    try {
      const res = await registerAction({
        fullName: data.fullName,
        email: data.email,
        password: data.password,
      });

      if (!res.success) {
        throw new Error(res.error || "Registration failed");
      }

      setIsSuccess(true);
      toast.success("Account created!", {
        description: "Your account is ready. Please sign in to continue.",
      });

      setTimeout(() => {
        router.push("/login");
      }, 700);
    } catch (err: unknown) {
      setShake(true);
      const msg = err instanceof Error ? err.message : "Registration failed";
      toast.error("Registration failed", { description: msg });
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
          Create an account
        </h2>
        <p className="text-sm text-muted-foreground leading-relaxed">
          Get started with scalable URL redirection and click telemetry
        </p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-3.5">
        {/* Full Name */}
        <div className="space-y-1.5">
          <label
            htmlFor="fullName"
            className="text-sm font-medium text-foreground"
          >
            Full name
          </label>
          <Input
            id="fullName"
            type="text"
            placeholder="Senior Engineer"
            autoComplete="name"
            disabled={isSubmitting || isSuccess}
            className="h-11 sm:h-12 text-sm"
            {...register("fullName")}
          />
          {errors.fullName && (
            <p className="text-xs text-destructive animate-in fade-in slide-in-from-top-1">
              {errors.fullName.message}
            </p>
          )}
        </div>

        {/* Email Field */}
        <div className="space-y-1.5">
          <label
            htmlFor="email"
            className="text-sm font-medium text-foreground"
          >
            Email
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
        <div className="space-y-1.5">
          <label
            htmlFor="password"
            className="text-sm font-medium text-foreground"
          >
            Password
          </label>
          <PasswordInput
            id="password"
            placeholder="Create a strong password"
            autoComplete="new-password"
            disabled={isSubmitting || isSuccess}
            error={errors.password?.message}
            {...register("password")}
          />
        </div>

        {/* Terms Agreement Checkbox */}
        <div className="space-y-1 pt-0.5">
          <div className="flex items-start gap-2.5">
            <Controller
              control={control}
              name="terms"
              render={({ field }) => (
                <Checkbox
                  id="terms"
                  checked={field.value}
                  onCheckedChange={field.onChange}
                  className="mt-0.5 cursor-pointer"
                />
              )}
            />
            <label
              htmlFor="terms"
              className="text-sm text-muted-foreground cursor-pointer leading-snug select-none"
            >
              I agree to the{" "}
              <Link
                href="/terms"
                className="text-foreground underline underline-offset-2 hover:text-primary"
              >
                Terms of Service
              </Link>{" "}
              and{" "}
              <Link
                href="/privacy"
                className="text-foreground underline underline-offset-2 hover:text-primary"
              >
                Privacy Policy
              </Link>
            </label>
          </div>
          {errors.terms && (
            <p className="text-xs text-destructive animate-in fade-in slide-in-from-top-1">
              {errors.terms.message}
            </p>
          )}
        </div>

        {/* Submit Button */}
        <div className="pt-1.5">
          <SubmitButton
            isLoading={isSubmitting}
            isSuccess={isSuccess}
            successText="Account Created"
          >
            Create Account
          </SubmitButton>
        </div>
      </form>

      {/* Footer Link */}
      <div className="mt-5 text-center text-sm text-muted-foreground">
        Already have an account?{" "}
        <Link
          href="/login"
          className="font-semibold text-foreground underline underline-offset-4 hover:text-primary transition-colors"
        >
          Sign in
        </Link>
      </div>
    </AuthCard>
  );
}
