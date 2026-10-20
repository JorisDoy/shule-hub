// Types mirroring docs/api.md (Shule Hub API contract v0.1).
// Dates are ISO "YYYY-MM-DD", instants are ISO-8601 UTC, money is a number in KES, IDs are UUID strings.

export type Uuid = string;
export type IsoDate = string;
export type IsoInstant = string;
export type Money = number;

// ---- Enumerations ----

export const GRADE_LEVELS = [
  "DAYCARE",
  "PP1",
  "PP2",
  "GRADE_1",
  "GRADE_2",
  "GRADE_3",
  "GRADE_4",
  "GRADE_5",
  "GRADE_6",
  "GRADE_7",
  "GRADE_8",
  "GRADE_9",
] as const;
export type GradeLevel = (typeof GRADE_LEVELS)[number];

export const LEVEL_BANDS = ["DAYCARE", "PRE_PRIMARY", "LOWER_PRIMARY", "UPPER_PRIMARY", "JUNIOR_SCHOOL"] as const;
export type LevelBand = (typeof LEVEL_BANDS)[number];

export type Role =
  | "GROUP_DIRECTOR"
  | "BRANCH_HEAD"
  | "DEPUTY"
  | "BURSAR"
  | "ADMISSIONS_OFFICER"
  | "CLASS_TEACHER"
  | "SUBJECT_TEACHER"
  | "PARENT";

export type ScopeType = "GROUP" | "BRANCH" | "STREAM";
export type Gender = "FEMALE" | "MALE";
export type BoardingStatus = "DAY" | "BOARDING";
export type LearnerStatus = "ACTIVE" | "WITHDRAWN" | "ALUMNI";
export type Relationship = "MOTHER" | "FATHER" | "GUARDIAN" | "OTHER";

export const APPLICATION_STATUSES = [
  "ENQUIRY",
  "APPLIED",
  "ASSESSMENT_SCHEDULED",
  "OFFERED",
  "ACCEPTED",
  "ADMITTED",
  "WAITLISTED",
  "REJECTED",
  "WITHDRAWN",
] as const;
export type ApplicationStatus = (typeof APPLICATION_STATUSES)[number];

export type FeeAppliesTo = "ALL" | "DAY" | "BOARDING";
export type FeeStructureStatus = "DRAFT" | "PUBLISHED";
export const INVOICE_STATUSES = ["OPEN", "PARTIALLY_PAID", "PAID", "VOID"] as const;
export type InvoiceStatus = (typeof INVOICE_STATUSES)[number];
export type PaymentChannel = "MPESA_STK" | "MPESA_C2B" | "BANK" | "CASH";
export const PAYMENT_STATUSES = ["ALLOCATED", "SUSPENSE", "REVERSED"] as const;
export type PaymentStatus = (typeof PAYMENT_STATUSES)[number];
export type PaymentIntentStatus = "PENDING" | "SUBMITTED" | "SUCCEEDED" | "FAILED";
export const ASSESSMENT_TYPES = ["CAT", "END_TERM", "OBSERVATION", "PROJECT"] as const;
export type AssessmentType = (typeof ASSESSMENT_TYPES)[number];
export type AssessmentStatus = "DRAFT" | "RELEASED";
export type ReportCardStatus = "DRAFT" | "APPROVED" | "RELEASED";

export type Permission =
  | "TENANT_ADMIN"
  | "BRANCH_MANAGE"
  | "ACADEMICS_MANAGE"
  | "ADMISSIONS_VIEW"
  | "ADMISSIONS_MANAGE"
  | "LEARNERS_VIEW"
  | "LEARNERS_EDIT"
  | "FEES_VIEW"
  | "FEES_MANAGE"
  | "PAYMENTS_CONFIGURE"
  | "ASSESSMENT_ENTER"
  | "ASSESSMENT_MANAGE"
  | "REPORTS_RELEASE";

// ---- Common ----

export interface ProblemFieldError {
  field: string;
  message: string;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: ProblemFieldError[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ---- Identity and tenancy ----

export interface Me {
  personId: Uuid;
  givenName: string;
  familyName: string;
  email: string | null;
  phone: string | null;
  platformAdmin: boolean;
}

export interface Grant {
  role: Role;
  scopeType: ScopeType;
  branchId: Uuid | null;
  branchName: string | null;
  streamId: Uuid | null;
}

export interface Membership {
  tenantId: Uuid;
  tenantName: string;
  tenantSlug: string;
  grants: Grant[];
  permissions: Permission[];
  parent: boolean;
}

export interface Branch {
  id: Uuid;
  name: string;
  code: string;
  county: string;
  boardingEnabled: boolean;
}

// ---- Academics ----

export interface Term {
  id: Uuid;
  number: number;
  startDate: IsoDate;
  endDate: IsoDate;
}

export interface AcademicYear {
  id: Uuid;
  year: number;
  startDate: IsoDate;
  endDate: IsoDate;
  current: boolean;
  terms: Term[];
}

export interface CreateAcademicYearRequest {
  year: number;
  startDate: IsoDate;
  endDate: IsoDate;
  terms: { number: number; startDate: IsoDate; endDate: IsoDate }[];
}

export interface CurrentTerm {
  id: Uuid;
  academicYearId: Uuid;
  year: number;
  number: number;
  startDate: IsoDate;
  endDate: IsoDate;
}

export interface Stream {
  id: Uuid;
  branchId: Uuid;
  academicYearId: Uuid;
  gradeLevel: GradeLevel;
  name: string;
  capacity: number;
}

export interface CreateStreamRequest {
  branchId: Uuid;
  academicYearId: Uuid;
  gradeLevel: GradeLevel;
  name: string;
  capacity: number;
}

export interface LearningArea {
  id: Uuid;
  code: string;
  name: string;
  levelBand: LevelBand;
  sortOrder: number;
}

// ---- Learners ----

export interface LearnerSummary {
  id: Uuid;
  branchId: Uuid;
  admissionNumber: string;
  givenName: string;
  familyName: string;
  fullName: string;
  gradeLevel: GradeLevel;
  boardingStatus: BoardingStatus;
  status: LearnerStatus;
  streamId: Uuid | null;
  streamName: string | null;
  primaryContactPhone: string | null;
}

export interface LearnerGuardian {
  personId: Uuid;
  fullName: string;
  phone: string | null;
  email: string | null;
  relationship: Relationship;
  feePayer: boolean;
  canPickUp: boolean;
  primaryContact: boolean;
}

export interface LearnerDetail extends LearnerSummary {
  otherNames: string | null;
  dateOfBirth: IsoDate | null;
  gender: Gender | null;
  upi: string | null;
  assessmentNumber: string | null;
  admittedOn: IsoDate | null;
  guardians: LearnerGuardian[];
}

export interface UpdateLearnerRequest {
  upi?: string;
  assessmentNumber?: string;
  boardingStatus?: BoardingStatus;
}

// ---- Admissions ----

export interface ApplicantLearner {
  givenName: string;
  familyName: string;
  dateOfBirth: IsoDate;
  gender: Gender;
}

export interface ApplicantGuardian {
  givenName: string;
  familyName: string;
  phone: string;
  email?: string;
  relationship: Relationship;
}

export interface ApplicationSummary {
  id: Uuid;
  reference: string;
  branchId: Uuid;
  learnerName: string;
  gradeLevel: GradeLevel;
  status: ApplicationStatus;
  guardianName: string;
  guardianPhone: string;
  source: string;
  createdAt: IsoInstant;
}

export interface ApplicationHistoryEntry {
  fromStatus: ApplicationStatus | null;
  toStatus: ApplicationStatus;
  note: string | null;
  actorName: string | null;
  occurredAt: IsoInstant;
}

export interface ApplicationDetail extends ApplicationSummary {
  academicYearId: Uuid | null;
  learner: ApplicantLearner;
  guardian: ApplicantGuardian;
  boardingRequested: boolean;
  notes: string | null;
  assessmentDate: IsoDate | null;
  offerExpiresOn: IsoDate | null;
  learnerId: Uuid | null;
  admissionNumber: string | null;
  allowedTransitions: ApplicationStatus[];
  history: ApplicationHistoryEntry[];
}

export interface CreateApplicationRequest {
  branchId: Uuid;
  academicYearId?: Uuid;
  gradeLevel: GradeLevel;
  boardingRequested: boolean;
  learner: ApplicantLearner;
  guardian: ApplicantGuardian;
  notes?: string;
}

export interface TransitionRequest {
  to: ApplicationStatus;
  note?: string;
  assessmentDate?: IsoDate;
  offerExpiresOn?: IsoDate;
}

export interface AdmitResponse {
  learnerId: Uuid;
  admissionNumber: string;
}

// ---- Fees ----

export interface VoteHead {
  id: Uuid;
  code: string;
  name: string;
  priority: number;
}

export interface FeeStructureItem {
  id: Uuid;
  voteHeadId: Uuid;
  voteHeadName: string;
  amount: Money;
  appliesTo: FeeAppliesTo;
}

export interface FeeStructure {
  id: Uuid;
  branchId: Uuid;
  termId: Uuid;
  gradeLevel: GradeLevel;
  name: string;
  status: FeeStructureStatus;
  dayTotal: Money;
  boardingTotal: Money;
  items: FeeStructureItem[];
}

export interface FeeStructureRequest {
  branchId: Uuid;
  termId: Uuid;
  gradeLevel: GradeLevel;
  name: string;
  items: { voteHeadId: Uuid; amount: Money; appliesTo: FeeAppliesTo }[];
}

export interface GenerateInvoicesResponse {
  created: number;
  skipped: number;
}

export interface InvoiceSummary {
  id: Uuid;
  number: string;
  learnerId: Uuid;
  learnerName: string;
  admissionNumber: string;
  termId: Uuid;
  issuedOn: IsoDate;
  dueOn: IsoDate;
  total: Money;
  paid: Money;
  balance: Money;
  status: InvoiceStatus;
}

export interface StatementEntry {
  date: IsoDate;
  type: string;
  reference: string;
  description: string;
  debit: Money;
  credit: Money;
  runningBalance: Money;
}

export interface Statement {
  learnerId: Uuid;
  learnerName: string;
  admissionNumber: string;
  totalInvoiced: Money;
  totalPaid: Money;
  balance: Money;
  entries: StatementEntry[];
}

export interface AgingBuckets {
  notDue: Money;
  days1To30: Money;
  days31To60: Money;
  days61To90: Money;
  over90: Money;
  total: Money;
}

export interface ArrearsLearner extends AgingBuckets {
  learnerId: Uuid;
  learnerName: string;
  admissionNumber: string;
  gradeLevel: GradeLevel;
  primaryContactPhone: string | null;
  oldestDueOn: IsoDate | null;
}

export interface ArrearsReport {
  asOf: IsoDate;
  totals: AgingBuckets;
  learners: ArrearsLearner[];
}

export interface PaymentView {
  id: Uuid;
  branchId: Uuid;
  channel: PaymentChannel;
  reference: string;
  amount: Money;
  allocated: Money;
  unallocated: Money;
  payerPhone: string | null;
  payerName: string | null;
  accountReference: string | null;
  learnerId: Uuid | null;
  learnerName: string | null;
  receivedAt: IsoInstant;
  status: PaymentStatus;
}

export interface RecordPaymentRequest {
  learnerId: Uuid;
  amount: Money;
  channel: "CASH" | "BANK";
  reference: string;
  receivedOn: IsoDate;
}

export interface StkRequest {
  learnerId: Uuid;
  amount: number;
  phone: string;
  invoiceId?: Uuid;
}

export interface StkInitiation {
  intentId: Uuid;
  status: PaymentIntentStatus;
  customerMessage: string | null;
}

export interface StkStatus {
  intentId: Uuid;
  status: PaymentIntentStatus;
  amount: number;
  phone: string;
  resultDesc: string | null;
  mpesaReceipt: string | null;
  updatedAt: IsoInstant;
}

// ---- M-Pesa configuration ----

export type MpesaChannelType = "PAYBILL" | "TILL";
export type MpesaEnvironment = "SANDBOX" | "PRODUCTION";

export interface MpesaChannel {
  id: Uuid;
  branchId: Uuid;
  shortcode: string;
  partyB: string | null;
  channelType: MpesaChannelType;
  environment: MpesaEnvironment;
  active: boolean;
  callbackUrls: { stk: string; validation: string; confirmation: string };
}

export interface CreateMpesaChannelRequest {
  branchId: Uuid;
  shortcode: string;
  partyB?: string;
  channelType: MpesaChannelType;
  environment: MpesaEnvironment;
  consumerKey: string;
  consumerSecret: string;
  passkey: string;
}

// ---- Assessment ----

export interface GradingBand {
  code: string;
  label: string;
  minPercent: number;
  maxPercent: number;
  points: number;
  descriptor: string | null;
}

export interface GradingScale {
  id: Uuid;
  code: string;
  name: string;
  appliesTo: LevelBand[];
  bands: GradingBand[];
}

export interface Assessment {
  id: Uuid;
  title: string;
  type: AssessmentType;
  termId: Uuid;
  streamId: Uuid;
  streamName: string;
  gradeLevel: GradeLevel;
  learningAreaId: Uuid;
  learningAreaName: string;
  maxScore: number | null;
  weight: number;
  assessedOn: IsoDate;
  status: AssessmentStatus;
  scoredCount: number;
  learnerCount: number;
}

export interface CreateAssessmentRequest {
  title: string;
  type: AssessmentType;
  termId: Uuid;
  streamId: Uuid;
  learningAreaId: Uuid;
  maxScore?: number;
  weight: number;
  assessedOn: IsoDate;
}

export interface ScoreRow {
  learnerId: Uuid;
  admissionNumber: string;
  learnerName: string;
  rawScore: number | null;
  rubricLevel: string | null;
  percent: number | null;
  bandCode: string | null;
  comment: string | null;
}

export interface ScoreSheet {
  assessment: Assessment;
  scale: GradingScale | null;
  rows: ScoreRow[];
}

export interface ScoreInput {
  learnerId: Uuid;
  rawScore?: number;
  rubricLevel?: string;
  comment?: string;
}

export interface ReportCardArea {
  learningAreaId: Uuid;
  name: string;
  percent: number | null;
  bandCode: string | null;
  bandLabel: string | null;
  points: number | null;
  assessmentCount: number;
  comment: string | null;
}

export interface ReportCard {
  learner: { id: Uuid; fullName: string; admissionNumber: string; gradeLevel: GradeLevel; streamName: string | null };
  term: { id: Uuid; year: number; number: number };
  scale: { code: string; name: string } | null;
  areas: ReportCardArea[];
  classTeacherComment: string | null;
  headComment: string | null;
  status: ReportCardStatus;
  releasedAt: IsoInstant | null;
}

export interface ReportCardListItem {
  learnerId: Uuid;
  learnerName: string;
  admissionNumber: string;
  status: ReportCardStatus;
}

export interface UpdateReportCardRequest {
  classTeacherComment?: string;
  headComment?: string;
  areaComments: { learningAreaId: Uuid; comment: string }[];
}

// ---- Parent portal ----

export interface Child {
  learnerId: Uuid;
  fullName: string;
  admissionNumber: string;
  gradeLevel: GradeLevel;
  streamName: string | null;
  balance: Money;
}

export interface ChildReportCardRef {
  termId: Uuid;
  year: number;
  number: number;
  releasedAt: IsoInstant | null;
}
