import { Suspense } from "react";
import { AppShell } from "@/components/shell/AppShell";
import { ErrorBoundary } from "@/components/ui/ErrorBoundary";
import Loading from "./loading";

export default function WorkspaceLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <Suspense fallback={<Loading />}>
      <AppShell>
        <ErrorBoundary>{children}</ErrorBoundary>
      </AppShell>
    </Suspense>
  );
}
