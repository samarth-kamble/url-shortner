import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const AUTH_ROUTES = ["/login", "/sign-up", "/register", "/forgot-password", "/reset-password"];
const PROTECTED_ROUTES = ["/dashboard", "/analytics", "/settings"];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const accessToken = request.cookies.get("shortlink_access_token")?.value;
  const refreshToken = request.cookies.get("shortlink_refresh_token")?.value;
  const isAuthenticated = Boolean(accessToken || refreshToken);

  // Skip redirecting Next.js Server Actions (POST requests with Next-Action header)
  // Let the server action execute and return a clean typed error instead of an unexpected 307 redirect
  const isServerAction = request.method === "POST" && Boolean(request.headers.get("next-action"));
  if (isServerAction) {
    return NextResponse.next();
  }

  // 1. If user is already authenticated, block access to auth/guest routes and redirect to /dashboard
  if (isAuthenticated && AUTH_ROUTES.some((route) => pathname.startsWith(route))) {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  // 2. If user is not authenticated and attempts to access protected routes, redirect to /login
  if (!isAuthenticated && PROTECTED_ROUTES.some((route) => pathname.startsWith(route))) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("from", pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/login",
    "/sign-up",
    "/register",
    "/forgot-password",
    "/reset-password",
    "/dashboard/:path*",
    "/analytics/:path*",
    "/settings/:path*",
  ],
};

