export const SITE_URL = (process.env.NEXT_PUBLIC_SITE_URL || "https://shulehub.example").replace(/\/+$/, "");
export const SITE_NAME = "Shule Hub";
export const SITE_DESCRIPTION =
  "School management for Kenyan private schools, Daycare to Grade 9: CBE report cards on the 4-level rubric and the KNEC 8-level scale, fees with M-Pesa STK Push and arrears, admissions, multi-branch groups and a parent app.";
export const CONTACT_EMAIL = "hello@shulehub.example";
export const DEMO_MAILTO = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent("Shule Hub demo request")}&body=${encodeURIComponent(
  "School name:\nTown / county:\nNumber of learners:\nNumber of branches:\nBest phone number:\n",
)}`;

export const PLANS = [
  {
    name: "Starter",
    price: 40,
    forWho: "One campus getting off spreadsheets",
    includes: ["Admissions and learner records", "Fee structures, invoices and statements", "M-Pesa STK Push and Paybill matching", "CBE report cards"],
  },
  {
    name: "Professional",
    price: 60,
    forWho: "Schools that want parents paying from their phones",
    includes: ["Everything in Starter", "Parent app: balances, statements, report cards", "Arrears aging and CSV export", "Junior School KNEC 8-level scale"],
  },
  {
    name: "Group",
    price: 80,
    forWho: "Directors running several branches",
    includes: ["Everything in Professional", "Multiple branches under one group", "Branch-level roles and M-Pesa channels", "Group-wide reporting"],
  },
] as const;
