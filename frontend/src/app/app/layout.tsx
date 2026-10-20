import type { Metadata, Viewport } from "next";
import { AppFrame } from "@/features/shell/AppFrame";

export const metadata: Metadata = {
  title: { default: "Shule Hub", template: "%s · Shule Hub" },
  robots: { index: false, follow: false },
};

export const viewport: Viewport = {
  themeColor: "#ffffff",
};

/** Everything under /app is client-side and behind Keycloak sign-in. */
export default function AppLayout({ children }: { children: React.ReactNode }) {
  return <AppFrame>{children}</AppFrame>;
}
