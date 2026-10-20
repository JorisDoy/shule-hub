import type { Metadata } from "next";
import Link from "next/link";
import { Wordmark } from "@/components/Brand";
import { ButtonLink } from "@/components/ui/Button";
import { ArrearsSpecimen, HeroReportCard, ParentPhone, PhonePrompt, RubricSpecimens } from "@/features/marketing/Specimens";
import { CONTACT_EMAIL, DEMO_MAILTO, PLANS, SITE_DESCRIPTION, SITE_NAME, SITE_URL } from "@/lib/site";
import { formatKes } from "@/lib/format";

export const metadata: Metadata = {
  title: { absolute: "Shule Hub: school management built for CBE and M-Pesa" },
  description: SITE_DESCRIPTION,
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    url: "/",
    siteName: SITE_NAME,
    locale: "en_KE",
    title: "Shule Hub: school management built for CBE and M-Pesa",
    description: "CBE report cards, M-Pesa fee collection, admissions and a parent app for Kenyan private schools, Daycare to Grade 9.",
  },
  twitter: {
    card: "summary_large_image",
    title: "Shule Hub: school management built for CBE and M-Pesa",
    description: "CBE report cards, M-Pesa fee collection, admissions and a parent app for Kenyan private schools.",
  },
  keywords: ["school management system Kenya", "CBE report cards", "CBC report card", "KNEC 8-level scale", "M-Pesa school fees", "school fees STK push", "school admissions Kenya", "Junior School"],
};

const jsonLd = {
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  name: SITE_NAME,
  url: SITE_URL,
  applicationCategory: "EducationalApplication",
  applicationSubCategory: "School management system",
  operatingSystem: "Web, Android, iOS",
  description: SITE_DESCRIPTION,
  inLanguage: "en-KE",
  areaServed: { "@type": "Country", name: "Kenya" },
  audience: { "@type": "EducationalAudience", educationalRole: "administrator" },
  featureList: [
    "CBE report cards on the 4-level rubric (PP1 to Grade 6) and the KNEC 8-level scale (Grades 7 to 9)",
    "Fee structures, invoices and statements in Kenya shillings",
    "M-Pesa STK Push and Paybill payment matching",
    "Arrears aging report with CSV export",
    "Admissions from enquiry to admission number",
    "Multi-branch school groups",
    "Parent app for balances, payments and report cards",
  ],
  offers: PLANS.map((p) => ({
    "@type": "Offer",
    name: `${p.name} (proposed)`,
    price: p.price,
    priceCurrency: "KES",
    description: `KES ${p.price} per learner per term. ${p.forWho}.`,
  })),
};

function Section({ id, title, intro, children, aside }: { id: string; title: string; intro: React.ReactNode; children?: React.ReactNode; aside?: React.ReactNode }) {
  return (
    <section id={id} aria-labelledby={`${id}-title`} className="scroll-mt-20 border-t border-line py-16 sm:py-20">
      <div className="grid gap-10 lg:grid-cols-[minmax(0,26rem)_1fr] lg:gap-14">
        <div>
          <h2 id={`${id}-title`} className="font-display text-[1.85rem] leading-tight sm:text-[2.2rem]">
            {title}
          </h2>
          <div className="mt-4 flex max-w-[60ch] flex-col gap-3 text-[1.05rem] text-ink-2">{intro}</div>
          {aside}
        </div>
        <div className="min-w-0">{children}</div>
      </div>
    </section>
  );
}

const ADMISSION_STEPS = [
  { title: "Enquiry", text: "From the school website or a walk-in, with the parent's phone number." },
  { title: "Application", text: "Grade, intake year, day or boarding." },
  { title: "Assessment", text: "Book the date. Optional for Daycare and PP1." },
  { title: "Offer", text: "With an expiry date, so places don't sit unclaimed." },
  { title: "Acceptance", text: "Recorded when the family confirms." },
  { title: "Admission", text: "Pick a stream with space. The admission number is issued on the spot." },
];

export default function LandingPage() {
  return (
    <>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd).replace(/</g, "\\u003c") }} />
      <header className="sticky top-0 z-30 border-b border-line bg-canvas/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-3 px-4 sm:px-6 md:gap-6">
          <Wordmark href="/" />
          <nav aria-label="Sections" className="ml-auto hidden items-center gap-6 text-[0.95rem] font-semibold text-ink-2 md:flex">
            <a href="#report-cards" className="hover:text-ink">Report cards</a>
            <a href="#fees" className="hover:text-ink">Fees</a>
            <a href="#admissions" className="hover:text-ink">Admissions</a>
            <a href="#pricing" className="hover:text-ink">Pricing</a>
          </nav>
          <ButtonLink href="/app" size="sm" className="ml-auto md:ml-0">
            <span className="sm:hidden">Open demo</span>
            <span className="hidden sm:inline">Open the demo school</span>
          </ButtonLink>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 sm:px-6">
        <section aria-labelledby="hero-title" className="grid items-center gap-14 pt-12 pb-24 sm:pt-16 lg:grid-cols-[1.05fr_1fr] lg:gap-16 lg:pt-20 lg:pb-28">
          <div>
            <h1 id="hero-title" className="font-display text-[2.6rem] leading-[1.04] font-bold tracking-[-0.02em] sm:text-[3.4rem] lg:text-[4rem]">
              School management built for CBE and M-Pesa
            </h1>
            <p className="mt-6 max-w-[34rem] text-[1.15rem] leading-relaxed text-ink-2">
              Shule Hub runs admissions, fees and report cards for Kenyan private schools, from Daycare and PP1 to Grade 9. Teachers grade on the CBE rubric, parents pay from their phones,
              and the bursar always knows who owes what.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <ButtonLink href="/app" size="lg">
                Open the demo school
              </ButtonLink>
              <ButtonLink href={DEMO_MAILTO} size="lg" variant="secondary">
                Book a demo
              </ButtonLink>
            </div>
            <p className="mt-5 text-sm text-ink-3">One campus or a group of branches. Works on the phones parents and teachers already carry.</p>
          </div>
          <div className="lg:pl-4">
            <HeroReportCard />
          </div>
        </section>

        <Section
          id="report-cards"
          title="Report cards that follow CBE, grade by grade"
          intro={
            <>
              <p>
                Lower and upper primary use the 4-level rubric. Junior School uses the KNEC 8-level scale with points. Shule Hub picks the right scale from the learner&apos;s grade, so teachers just
                enter marks or levels.
              </p>
              <p>
                Type <strong className="text-ink">149 out of 200</strong> and the score sheet shows <strong className="text-ink">75%, EE2</strong> before you save. Rounding matches the printed
                report, every time.
              </p>
              <p>Class teacher and head teacher comments, approval, then one click releases the class&apos;s cards to parents. Cards print cleanly on A4.</p>
            </>
          }
        >
          <RubricSpecimens />
        </Section>

        <Section
          id="fees"
          title="Fees collected by M-Pesa, arrears you can act on"
          intro={
            <>
              <p>Set fee structures per grade and term with vote heads for tuition, lunch, transport and boarding. Generate the whole term&apos;s invoices in one step.</p>
              <p>
                Send an <strong className="text-ink">M-Pesa STK Push</strong> from the office or let parents pay from the parent app. Paybill payments are matched to the learner by admission number;
                anything that doesn&apos;t match waits in suspense for the bursar to assign.
              </p>
              <p>The arrears report ages every balance, sorts by who owes most, and downloads as a CSV for follow-up calls.</p>
            </>
          }
        >
          <div className="flex flex-col gap-6">
            <ol className="grid gap-3 sm:grid-cols-3">
              {[
                ["Request", "Enter the amount and the payer's number, or the parent taps Pay."],
                ["Confirm", "The phone shows the M-Pesa prompt. The payer enters their PIN."],
                ["Receipt", "The M-Pesa receipt lands on the statement within seconds."],
              ].map(([t, d], i) => (
                <li key={t} className="rounded-md border border-line bg-paper px-4 py-3.5">
                  <p className="flex items-baseline gap-2 font-semibold">
                    <span className="font-display text-xl text-uniform">{i + 1}</span>
                    {t}
                  </p>
                  <p className="mt-1 text-[0.95rem] text-ink-2">{d}</p>
                </li>
              ))}
            </ol>
            <div className="grid items-start gap-6 md:grid-cols-[1fr_15rem]">
              <ArrearsSpecimen />
              <PhonePrompt className="mx-auto w-full max-w-[15rem]" amount={12500} />
            </div>
          </div>
        </Section>

        <Section
          id="admissions"
          title="Admissions from first enquiry to admission number"
          intro={
            <>
              <p>
                Every enquiry gets a reference and a clear next step. Each change is logged with who made it and why, so nothing gets lost between the gate, the admissions office and the head
                teacher.
              </p>
              <p>Admitting places the child in a stream with space and issues the admission number immediately.</p>
            </>
          }
        >
          <ol className="relative flex flex-col gap-0 border-l-2 border-rule pl-7">
            {ADMISSION_STEPS.map((s, i) => (
              <li key={s.title} className="relative pb-6 last:pb-0">
                <span aria-hidden className="absolute top-0 -left-[2.6rem] flex size-7 items-center justify-center rounded-full border-2 border-canvas bg-uniform text-[0.8rem] font-bold text-white">
                  {i + 1}
                </span>
                <p className="font-semibold">{s.title}</p>
                <p className="text-ink-2">{s.text}</p>
              </li>
            ))}
            <li className="relative mt-5">
              <div className="paper-margin inline-block rounded-md border border-line py-3 pr-6 pl-14">
                <p className="text-sm text-ink-2">Admission number</p>
                <p className="font-display text-2xl font-bold tracking-wide">DA/2027/0042</p>
              </div>
            </li>
          </ol>
        </Section>

        <Section
          id="groups"
          title="One group, several branches"
          intro={
            <>
              <p>
                Directors see every campus. Head teachers, bursars and teachers see their own branch, or just their own class. Each branch can have its own Paybill or Till, fee structures and
                streams.
              </p>
            </>
          }
        >
          <div className="overflow-x-auto rounded-md border border-line bg-paper">
            <table className="w-full text-left text-[0.95rem]">
              <caption className="sr-only">Example school group with three branches</caption>
              <thead className="border-b border-line-strong text-[0.8rem] text-ink-2">
                <tr>
                  <th scope="col" className="px-4 py-2.5 font-semibold">Branch</th>
                  <th scope="col" className="px-4 py-2.5 font-semibold">County</th>
                  <th scope="col" className="px-4 py-2.5 text-right font-semibold">Learners</th>
                  <th scope="col" className="px-4 py-2.5 font-semibold">Offers</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {[
                  ["Ruaka", "Kiambu", 412, "Daycare to Grade 6, day"],
                  ["Kitengela", "Kajiado", 287, "PP1 to Grade 9, day"],
                  ["Nakuru", "Nakuru", 530, "Grade 1 to 9, day and boarding"],
                ].map(([b, c, n, o]) => (
                  <tr key={b as string}>
                    <th scope="row" className="px-4 py-3 font-semibold">{b}</th>
                    <td className="px-4 py-3">{c}</td>
                    <td className="px-4 py-3 text-right tabular">{n}</td>
                    <td className="px-4 py-3 text-ink-2">{o}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Section>

        <Section
          id="parents"
          title="A parent app that works over mobile data"
          intro={
            <>
              <p>Parents sign in on their phone&apos;s browser. Nothing to download from an app store. Pages are small, so they load on a basic data bundle.</p>
              <p>They see each child&apos;s balance, pay with M-Pesa, read the fee statement and open released report cards. Fewer calls to the office about balances.</p>
            </>
          }
        >
          <ParentPhone />
        </Section>

        <section id="pricing" aria-labelledby="pricing-title" className="scroll-mt-20 border-t border-line py-16 sm:py-20">
          <div className="flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 id="pricing-title" className="font-display text-[1.85rem] leading-tight sm:text-[2.2rem]">
                Pricing per learner, per term
              </h2>
              <p className="mt-3 max-w-[60ch] text-[1.05rem] text-ink-2">
                Proposed pricing. You pay for active learners each term, so the bill follows your enrolment. No setup fee for schools that join the pilot.
              </p>
            </div>
            <span className="rounded-sm border border-owe/40 bg-owe-tint px-2.5 py-1 text-sm font-semibold text-owe">Proposed pricing</span>
          </div>
          <div className="mt-8 rounded-md border border-line bg-paper">
            <table className="w-full text-left">
              <caption className="sr-only">Proposed pricing per learner per term, in Kenya shillings</caption>
              <thead className="hidden border-b border-line-strong text-[0.85rem] text-ink-2 sm:table-header-group">
                <tr>
                  <th scope="col" className="px-5 py-3 font-semibold">Plan</th>
                  <th scope="col" className="px-5 py-3 text-right font-semibold">Per learner per term</th>
                  <th scope="col" className="px-5 py-3 font-semibold">Includes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line align-top">
                {PLANS.map((p) => (
                  <tr key={p.name} className="grid grid-cols-[1fr_auto] gap-x-4 px-4 py-5 sm:table-row sm:p-0">
                    <th scope="row" className="font-normal sm:px-5 sm:py-5">
                      <span className="block font-display text-xl font-semibold">{p.name}</span>
                      <span className="block max-w-[16rem] text-[0.95rem] text-ink-2">{p.forWho}</span>
                    </th>
                    <td className="text-right sm:px-5 sm:py-5">
                      <span className="block font-display text-[1.6rem] leading-tight font-bold whitespace-nowrap tabular sm:text-[1.75rem]">KES {p.price}</span>
                      <span className="block text-[0.8rem] text-ink-3 sm:hidden">per learner per term</span>
                    </td>
                    <td className="col-span-2 mt-3 sm:mt-0 sm:px-5 sm:py-5">
                      <ul className="flex flex-col gap-1 text-[0.95rem]">
                        {p.includes.map((i) => (
                          <li key={i}>{i}</li>
                        ))}
                      </ul>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="mt-4 text-ink-2">
            For example, a school of 400 learners on Professional pays {formatKes(400 * 60, { whole: true })} a term. M-Pesa transaction charges are billed by Safaricom as usual.
          </p>
        </section>

        <section aria-labelledby="cta-title" className="border-t border-line py-16 sm:py-20">
          <div className="paper-margin rounded-lg border border-line py-10 pr-6 pl-14 sm:py-12 sm:pr-10 sm:pl-20">
            <h2 id="cta-title" className="font-display text-[1.85rem] leading-tight sm:text-[2.2rem]">
              See it with a demo school
            </h2>
            <p className="mt-3 max-w-[52ch] text-[1.05rem] text-ink-2">
              Open Demo Academy to try score entry, fee statements and the parent app, or book a call and we&apos;ll walk through it with your own grades and fee structure.
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <ButtonLink href="/app" size="lg">
                Open the demo school
              </ButtonLink>
              <ButtonLink href={DEMO_MAILTO} size="lg" variant="secondary">
                Book a demo
              </ButtonLink>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-line bg-paper">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-4 py-8 text-sm text-ink-2 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <Wordmark />
          <p>School management for Kenyan private schools, Daycare to Grade 9.</p>
          <div className="flex gap-5">
            <a href={`mailto:${CONTACT_EMAIL}`} className="font-semibold text-ink hover:text-uniform">
              {CONTACT_EMAIL}
            </a>
            <Link href="/app" className="font-semibold text-ink hover:text-uniform">
              Sign in
            </Link>
          </div>
        </div>
      </footer>
    </>
  );
}
