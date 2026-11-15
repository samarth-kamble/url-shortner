"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { motion } from "motion/react";
import { forgotPasswordSchema, type ForgotPasswordFormData } from "@/lib/auth-schemas";
import { AuthCard } from "@/components/auth/auth-card";
import { SubmitButton } from "@/components/auth/submit-button";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Mail, RotateCw } from "lucide-react";
import { toast } from "sonner";

export default function ForgotPasswordPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState<string | null>(null);
  const [shake, setShake] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: "",
    },
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    setIsSubmitting(true);
    setShake(false);

    try {
      // Simulate API latency (TODO: Call backend password-reset endpoint)
      await new Promise((resolve) => setTimeout(resolve, 800));

      setSubmittedEmail(data.email);
      toast.success("Reset link sent!", {
        description: `Check ${data.email} for recovery instructions.`,
      });
    } catch {
      setShake(true);
      toast.error("Request failed", { description: "Please try again later." });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    toast.info("Recovery email resent", {
      description: `A new reset link was delivered to ${submittedEmail}.`,
    });
  };

  return (
    <AuthCard shake={shake}>
      {submittedEmail ? (
        /* Success State: Check Your Inbox */
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.35, ease: "easeOut" }}
          className="space-y-6 text-center"
        >
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full border border-primary/20 bg-primary/10 text-primary">
            <Mail className="h-7 w-7" />
          </div>

          <div className="space-y-2">
            <h2 className="font-heading text-2xl font-bold tracking-tight text-foreground">
              Check your inbox
            </h2>
            <p className="text-xs text-muted-foreground sm:text-sm leading-relaxed">
              We dispatched a secure password reset link to:
            </p>
            <p className="font-mono text-xs font-semibold text-foreground bg-muted/40 py-1.5 px-3 rounded-md inline-block">
              {submittedEmail}
            </p>
          </div>

          <p className="text-xs text-muted-foreground">
            Didn&apos;t receive the email? Check your spam filter or{" "}
            <button
              type="button"
              onClick={handleResend}
              className="font-medium text-primary hover:underline inline-flex items-center gap-1"
            >
              <RotateCw className="h-3 w-3" /> resend link
            </button>
          </p>

          <div className="pt-2">
            <Link href="/login">
              <Button variant="outline" className="h-11 w-full gap-2 text-xs">
                <ArrowLeft className="h-4 w-4" /> Back to sign in
              </Button>
            </Link>
          </div>
        </motion.div>
      ) : (
        /* Form State: Request Reset */
        <>
          <div className="space-y-2 pb-6 text-center sm:text-left">
            <h2 className="font-heading text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
              Reset password
            </h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              Enter the email associated with your account and we will send you a recovery link
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="email" className="text-sm font-medium text-foreground">
                Email address
              </label>
              <Input
                id="email"
                type="email"
                placeholder="engineer@shortlink.io"
                autoComplete="email"
                disabled={isSubmitting}
                className="h-11 sm:h-12 text-sm"
                {...register("email")}
              />
              {errors.email && (
                <p className="text-xs text-destructive animate-in fade-in slide-in-from-top-1">
                  {errors.email.message}
                </p>
              )}
            </div>

            <div className="pt-2">
              <SubmitButton isLoading={isSubmitting}>
                Send Recovery Link
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
        </>
      )}
    </AuthCard>
  );
}

