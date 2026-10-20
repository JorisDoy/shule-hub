import Link from "next/link";
import { cx } from "@/lib/cx";

/**
 * The Shule Hub mark: an exercise-book page (ruled lines, red margin) folded into a shield.
 * Inline SVG, ~600 bytes, no request.
 */
export function Mark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" aria-hidden className={cx("shrink-0", className)}>
      <path d="M4 4h24v13.5c0 6.2-5.1 10.4-12 12.5C9.1 27.9 4 23.7 4 17.5V4Z" fill="#1F6B4F" />
      <path d="M7 7h18v10.3c0 4.7-3.8 7.9-9 9.6-5.2-1.7-9-4.9-9-9.6V7Z" fill="#FFFFFF" />
      <path d="M10.5 7v19" stroke="#D9584E" strokeWidth="1.3" />
      <path d="M7 11.5h18M7 15.5h18M7 19.5h17.4M8.2 23.5h15.6" stroke="#C9D6E6" strokeWidth="1.1" />
      <path d="M13.5 14.6l2.4 2.5 4.6-5.2" stroke="#5B3F9E" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function Wordmark({ className, href, inverted }: { className?: string; href?: string; inverted?: boolean }) {
  const content = (
    <>
      <Mark className="size-7" />
      <span className={cx("font-display text-[1.3rem] leading-none font-bold tracking-[-0.01em] whitespace-nowrap", inverted ? "text-white" : "text-ink")}>Shule Hub</span>
    </>
  );
  if (href) {
    return (
      <Link href={href} className={cx("inline-flex items-center gap-2", className)} aria-label="Shule Hub home">
        {content}
      </Link>
    );
  }
  return <span className={cx("inline-flex items-center gap-2", className)}>{content}</span>;
}
