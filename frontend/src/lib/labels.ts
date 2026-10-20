import type {
  ApplicationStatus,
  AssessmentStatus,
  AssessmentType,
  BoardingStatus,
  FeeAppliesTo,
  FeeStructureStatus,
  Gender,
  InvoiceStatus,
  LearnerStatus,
  PaymentChannel,
  PaymentIntentStatus,
  PaymentStatus,
  Relationship,
  ReportCardStatus,
} from "@/lib/api/types";

export type Tone = "neutral" | "green" | "stamp" | "owe" | "danger" | "info";

export const APPLICATION_STATUS: Record<ApplicationStatus, { label: string; tone: Tone; action: string }> = {
  ENQUIRY: { label: "Enquiry", tone: "neutral", action: "Mark as applied" },
  APPLIED: { label: "Applied", tone: "info", action: "Mark as applied" },
  ASSESSMENT_SCHEDULED: { label: "Assessment scheduled", tone: "info", action: "Schedule assessment" },
  OFFERED: { label: "Offered", tone: "green", action: "Make an offer" },
  ACCEPTED: { label: "Accepted", tone: "green", action: "Record acceptance" },
  ADMITTED: { label: "Admitted", tone: "stamp", action: "Admit" },
  WAITLISTED: { label: "Waitlisted", tone: "owe", action: "Move to waitlist" },
  REJECTED: { label: "Rejected", tone: "danger", action: "Reject" },
  WITHDRAWN: { label: "Withdrawn", tone: "neutral", action: "Mark as withdrawn" },
};

export const INVOICE_STATUS: Record<InvoiceStatus, { label: string; tone: Tone }> = {
  OPEN: { label: "Open", tone: "owe" },
  PARTIALLY_PAID: { label: "Part paid", tone: "info" },
  PAID: { label: "Paid", tone: "green" },
  VOID: { label: "Void", tone: "neutral" },
};

export const PAYMENT_STATUS: Record<PaymentStatus, { label: string; tone: Tone }> = {
  ALLOCATED: { label: "Allocated", tone: "green" },
  SUSPENSE: { label: "Suspense", tone: "owe" },
  REVERSED: { label: "Reversed", tone: "danger" },
};

export const PAYMENT_CHANNEL: Record<PaymentChannel, string> = {
  MPESA_STK: "M-Pesa prompt",
  MPESA_C2B: "M-Pesa Paybill",
  BANK: "Bank",
  CASH: "Cash",
};

export const INTENT_STATUS: Record<PaymentIntentStatus, { label: string; tone: Tone }> = {
  PENDING: { label: "Waiting for phone", tone: "info" },
  SUBMITTED: { label: "Waiting for PIN", tone: "info" },
  SUCCEEDED: { label: "Paid", tone: "green" },
  FAILED: { label: "Not paid", tone: "danger" },
};

export const FEE_STRUCTURE_STATUS: Record<FeeStructureStatus, { label: string; tone: Tone }> = {
  DRAFT: { label: "Draft", tone: "neutral" },
  PUBLISHED: { label: "Published", tone: "green" },
};

export const APPLIES_TO: Record<FeeAppliesTo, string> = {
  ALL: "Day and boarding",
  DAY: "Day scholars only",
  BOARDING: "Boarders only",
};

export const ASSESSMENT_TYPE: Record<AssessmentType, string> = {
  CAT: "CAT",
  END_TERM: "End of term",
  OBSERVATION: "Observation",
  PROJECT: "Project",
};

export const ASSESSMENT_STATUS: Record<AssessmentStatus, { label: string; tone: Tone }> = {
  DRAFT: { label: "Draft", tone: "neutral" },
  RELEASED: { label: "Released", tone: "stamp" },
};

export const REPORT_CARD_STATUS: Record<ReportCardStatus, { label: string; tone: Tone }> = {
  DRAFT: { label: "Draft", tone: "neutral" },
  APPROVED: { label: "Approved", tone: "green" },
  RELEASED: { label: "Released", tone: "stamp" },
};

export const LEARNER_STATUS: Record<LearnerStatus, { label: string; tone: Tone }> = {
  ACTIVE: { label: "Active", tone: "green" },
  WITHDRAWN: { label: "Withdrawn", tone: "neutral" },
  ALUMNI: { label: "Alumni", tone: "info" },
};

export const BOARDING: Record<BoardingStatus, string> = { DAY: "Day scholar", BOARDING: "Boarder" };
export const GENDER: Record<Gender, string> = { FEMALE: "Female", MALE: "Male" };
export const RELATIONSHIP: Record<Relationship, string> = {
  MOTHER: "Mother",
  FATHER: "Father",
  GUARDIAN: "Guardian",
  OTHER: "Other",
};

export const ROLE_LABELS: Record<string, string> = {
  GROUP_DIRECTOR: "Group director",
  BRANCH_HEAD: "Head teacher",
  DEPUTY: "Deputy head",
  BURSAR: "Bursar",
  ADMISSIONS_OFFICER: "Admissions officer",
  CLASS_TEACHER: "Class teacher",
  SUBJECT_TEACHER: "Subject teacher",
  PARENT: "Parent",
};
