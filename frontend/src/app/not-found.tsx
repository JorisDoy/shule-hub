import Link from "next/link";
import { Wordmark } from "@/components/Brand";

export default function NotFound() {
  return (
    <main className="flex min-h-dvh flex-col items-center justify-center px-4">
      <div className="w-full max-w-md">
        <Wordmark href="/" className="mb-8" />
        <div className="paper-margin rounded-lg border border-line py-7 pr-6 pl-14">
          <h1 className="font-display text-2xl">Page not found</h1>
          <p className="mt-2 text-ink-2">The link may be old or mistyped.</p>
          <div className="mt-5 flex gap-4 font-semibold">
            <Link href="/" className="text-uniform-dark underline underline-offset-2">
              Shule Hub home
            </Link>
            <Link href="/app" className="text-uniform-dark underline underline-offset-2">
              Open the app
            </Link>
          </div>
        </div>
      </div>
    </main>
  );
}
