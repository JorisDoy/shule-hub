import type { HttpClient } from "@/lib/api/client";
import type * as T from "@/lib/api/types";

type Sig = { signal?: AbortSignal };

/** Typed wrappers for every endpoint in docs/api.md that the web app uses. */
export function createApi(http: HttpClient) {
  return {
    me: {
      get: (o?: Sig) => http.get<T.Me>("/api/me", o),
      memberships: (o?: Sig) => http.get<T.Membership[]>("/api/me/memberships", o),
    },
    branches: {
      list: (o?: Sig) => http.get<T.Branch[]>("/api/branches", o),
    },
    academics: {
      years: (o?: Sig) => http.get<T.AcademicYear[]>("/api/academic-years", o),
      createYear: (body: T.CreateAcademicYearRequest) => http.post<T.AcademicYear>("/api/academic-years", body),
      currentTerm: (o?: Sig) => http.get<T.CurrentTerm>("/api/terms/current", o),
      streams: (q: { branchId?: string; academicYearId?: string }, o?: Sig) =>
        http.get<T.Stream[]>("/api/streams", { query: q, ...o }),
      createStream: (body: T.CreateStreamRequest) => http.post<T.Stream>("/api/streams", body),
      streamLearners: (streamId: string, o?: Sig) => http.get<T.LearnerSummary[]>(`/api/streams/${streamId}/learners`, o),
      learningAreas: (levelBand: T.LevelBand, o?: Sig) =>
        http.get<T.LearningArea[]>("/api/learning-areas", { query: { levelBand }, ...o }),
    },
    learners: {
      search: (
        q: { branchId?: string; gradeLevel?: string; streamId?: string; q?: string; status?: string; page?: number; size?: number },
        o?: Sig,
      ) => http.get<T.Page<T.LearnerSummary>>("/api/learners", { query: q, ...o }),
      get: (id: string, o?: Sig) => http.get<T.LearnerDetail>(`/api/learners/${id}`, o),
      update: (id: string, body: T.UpdateLearnerRequest) => http.patch<T.LearnerDetail>(`/api/learners/${id}`, body),
    },
    admissions: {
      list: (q: { branchId?: string; status?: string }, o?: Sig) =>
        http.get<T.ApplicationSummary[]>("/api/admissions/applications", { query: q, ...o }),
      create: (body: T.CreateApplicationRequest) => http.post<T.ApplicationDetail>("/api/admissions/applications", body),
      get: (id: string, o?: Sig) => http.get<T.ApplicationDetail>(`/api/admissions/applications/${id}`, o),
      transition: (id: string, body: T.TransitionRequest) =>
        http.post<T.ApplicationDetail>(`/api/admissions/applications/${id}/transitions`, body),
      admit: (id: string, streamId: string) =>
        http.post<T.AdmitResponse>(`/api/admissions/applications/${id}/admit`, { streamId }),
    },
    fees: {
      voteHeads: (o?: Sig) => http.get<T.VoteHead[]>("/api/fees/vote-heads", o),
      createVoteHead: (body: { code: string; name: string; priority: number }) => http.post<T.VoteHead>("/api/fees/vote-heads", body),
      structures: (q: { branchId?: string; termId?: string }, o?: Sig) =>
        http.get<T.FeeStructure[]>("/api/fees/structures", { query: q, ...o }),
      createStructure: (body: T.FeeStructureRequest) => http.post<T.FeeStructure>("/api/fees/structures", body),
      updateStructure: (id: string, body: T.FeeStructureRequest) => http.put<T.FeeStructure>(`/api/fees/structures/${id}`, body),
      publishStructure: (id: string) => http.post<T.FeeStructure>(`/api/fees/structures/${id}/publish`),
      generateInvoices: (body: { branchId: string; termId: string; dueOn: string }) =>
        http.post<T.GenerateInvoicesResponse>("/api/fees/invoices/generate", body),
      invoices: (q: { branchId?: string; termId?: string; learnerId?: string; status?: string }, o?: Sig) =>
        http.get<T.InvoiceSummary[]>("/api/fees/invoices", { query: q, ...o }),
      statement: (learnerId: string, o?: Sig) => http.get<T.Statement>(`/api/fees/learners/${learnerId}/statement`, o),
      arrears: (q: { branchId?: string; asOf?: string }, o?: Sig) => http.get<T.ArrearsReport>("/api/fees/arrears", { query: q, ...o }),
      payments: (q: { branchId?: string; status?: string; learnerId?: string }, o?: Sig) =>
        http.get<T.PaymentView[]>("/api/fees/payments", { query: q, ...o }),
      recordPayment: (body: T.RecordPaymentRequest) => http.post<T.PaymentView>("/api/fees/payments", body),
      assignPayment: (id: string, learnerId: string) => http.post<T.PaymentView>(`/api/fees/payments/${id}/assign`, { learnerId }),
      stk: (body: T.StkRequest) => http.post<T.StkInitiation>("/api/fees/stk", body),
      stkStatus: (intentId: string, o?: Sig) => http.get<T.StkStatus>(`/api/fees/stk/${intentId}`, o),
    },
    mpesa: {
      channels: (o?: Sig) => http.get<T.MpesaChannel[]>("/api/payments/mpesa/channels", o),
      createChannel: (body: T.CreateMpesaChannelRequest) => http.post<T.MpesaChannel>("/api/payments/mpesa/channels", body),
      registerC2b: (id: string) => http.post<{ responseDescription: string }>(`/api/payments/mpesa/channels/${id}/register-c2b`),
    },
    assessment: {
      scales: (o?: Sig) => http.get<T.GradingScale[]>("/api/assessment/grading-scales", o),
      list: (q: { streamId?: string; termId?: string; learningAreaId?: string }, o?: Sig) =>
        http.get<T.Assessment[]>("/api/assessment/assessments", { query: q, ...o }),
      create: (body: T.CreateAssessmentRequest) => http.post<T.Assessment>("/api/assessment/assessments", body),
      scores: (id: string, o?: Sig) => http.get<T.ScoreSheet>(`/api/assessment/assessments/${id}/scores`, o),
      saveScores: (id: string, scores: T.ScoreInput[]) => http.put<T.ScoreSheet>(`/api/assessment/assessments/${id}/scores`, { scores }),
      release: (id: string) => http.post<T.Assessment>(`/api/assessment/assessments/${id}/release`),
      reportCards: (q: { streamId: string; termId: string }, o?: Sig) =>
        http.get<T.ReportCardListItem[]>("/api/assessment/report-cards", { query: q, ...o }),
      reportCard: (learnerId: string, termId: string, o?: Sig) =>
        http.get<T.ReportCard>(`/api/assessment/report-cards/${learnerId}/${termId}`, o),
      updateReportCard: (learnerId: string, termId: string, body: T.UpdateReportCardRequest) =>
        http.put<T.ReportCard>(`/api/assessment/report-cards/${learnerId}/${termId}`, body),
      approveReportCard: (learnerId: string, termId: string) =>
        http.post<T.ReportCard>(`/api/assessment/report-cards/${learnerId}/${termId}/approve`),
      releaseReportCards: (body: { streamId: string; termId: string }) =>
        http.post<{ released: number }>("/api/assessment/report-cards/release", body),
    },
    parent: {
      children: (o?: Sig) => http.get<T.Child[]>("/api/me/children", o),
      statement: (learnerId: string, o?: Sig) => http.get<T.Statement>(`/api/me/children/${learnerId}/statement`, o),
      reportCards: (learnerId: string, o?: Sig) => http.get<T.ChildReportCardRef[]>(`/api/me/children/${learnerId}/report-cards`, o),
      reportCard: (learnerId: string, termId: string, o?: Sig) =>
        http.get<T.ReportCard>(`/api/me/children/${learnerId}/report-cards/${termId}`, o),
      pay: (learnerId: string, body: { amount: number; phone: string }) =>
        http.post<T.StkInitiation>(`/api/me/children/${learnerId}/pay`, body),
      paymentStatus: (learnerId: string, intentId: string, o?: Sig) =>
        http.get<T.StkStatus>(`/api/me/children/${learnerId}/payments/${intentId}`, o),
    },
  };
}

export type Api = ReturnType<typeof createApi>;
