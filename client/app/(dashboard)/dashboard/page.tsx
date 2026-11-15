import { Suspense } from "react";
import { getMyLinksAction } from "@/app/actions/link-actions";
import { getProfileAction } from "@/app/actions/auth-actions";
import { DashboardView } from "@/components/dashboard/dashboard-view";
import { Skeleton } from "@/components/ui/skeleton";

export const metadata = {
  title: "Links — ShortLink Dashboard",
  description: "Manage, redirect and analyze your short links.",
};

async function DashboardContent() {
  const [links, userProfile] = await Promise.all([
    getMyLinksAction(),
    getProfileAction(),
  ]);
  return <DashboardView initialLinks={links} userProfile={userProfile} />;
}

export default function DashboardPage() {
  return (
    <Suspense
      fallback={
        <div className="space-y-6 p-4 sm:p-6 lg:p-8 max-w-6xl mx-auto">
          <Skeleton className="h-10 w-48 rounded-lg" />
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Skeleton className="h-24 rounded-xl" />
            <Skeleton className="h-24 rounded-xl" />
            <Skeleton className="h-24 rounded-xl" />
            <Skeleton className="h-24 rounded-xl" />
          </div>
          <Skeleton className="h-64 rounded-xl" />
        </div>
      }
    >
      <DashboardContent />
    </Suspense>
  );
}
