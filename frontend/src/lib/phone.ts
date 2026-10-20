// Kenyan mobile numbers. People type them every way: 0712 345 678, +254 712 345 678, 254712345678.
// The API wants 12 digits: 2547XXXXXXXX (Safaricom/Airtel 07) or 2541XXXXXXXX (newer 01 ranges).

/**
 * Normalises a Kenyan mobile number to 2547XXXXXXXX / 2541XXXXXXXX.
 * Accepts 07XXXXXXXX, 01XXXXXXXX, +2547…, +2541…, 2547…, 2541… and the bare 9-digit 7XXXXXXXX / 1XXXXXXXX,
 * with spaces, dashes, dots or brackets anywhere. Returns null when it is not a valid Kenyan mobile number.
 */
export function normalizeKenyanPhone(input: string | null | undefined): string | null {
  if (!input) return null;
  const trimmed = input.trim();
  if (!/^\+?[\d\s\-().]+$/.test(trimmed)) return null;
  const hasPlus = trimmed.startsWith("+");
  const digits = trimmed.replace(/\D/g, "");

  let national: string | null = null;
  if (hasPlus) {
    if (digits.length === 12 && digits.startsWith("254")) national = digits.slice(3);
  } else if (digits.length === 12 && digits.startsWith("254")) {
    national = digits.slice(3);
  } else if (digits.length === 10 && digits.startsWith("0")) {
    national = digits.slice(1);
  } else if (digits.length === 9) {
    national = digits;
  }

  if (!national || !/^[17]\d{8}$/.test(national)) return null;
  return `254${national}`;
}

/** 254712345678 → "0712 345 678" for display. Falls back to the raw value. */
export function formatKenyanPhone(value: string | null | undefined): string {
  if (!value) return "—";
  const normalized = normalizeKenyanPhone(value);
  if (!normalized) return value;
  const local = `0${normalized.slice(3)}`;
  return `${local.slice(0, 4)} ${local.slice(4, 7)} ${local.slice(7)}`;
}

export const PHONE_HINT = "Safaricom or Airtel number, e.g. 0712 345 678";
export const PHONE_ERROR = "Enter a Kenyan mobile number like 0712 345 678 or 0110 123 456";
