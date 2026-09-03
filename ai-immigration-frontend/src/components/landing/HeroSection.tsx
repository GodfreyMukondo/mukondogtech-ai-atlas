// src/components/landing/HeroSection.tsx
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  BadgeCheck,
  CheckCircle2,
  ChevronRight,
  Clock3,
  FileCheck2,
  FileSearch,
  FileText,
  Fingerprint,
  Globe2,
  Info,
  LockKeyhole,
  SearchCheck,
  ShieldCheck,
  Sparkles,
  UploadCloud,
  Users,
  type LucideIcon,
} from "lucide-react";

interface Feature {
  icon: LucideIcon;
  title: string;
  description: string;
}

interface Step {
  number: string;
  icon: LucideIcon;
  title: string;
  description: string;
}

interface ReviewItem {
  title: string;
  description: string;
  status: "checked" | "warning";
}

interface Audience {
  icon: LucideIcon;
  title: string;
  description: string;
}

const DOCUMENT_TYPES = [
  "Passport review",
  "Visa documents",
  "Residence permits",
  "Supporting documents",
];

const FEATURES: Feature[] = [
  {
    icon: FileSearch,
    title: "Document information",
    description:
      "Key details are pulled from your uploaded document so you can review everything in one place.",
  },
  {
    icon: SearchCheck,
    title: "Consistency checks",
    description:
      "The system flags information that looks inconsistent or needs a closer look.",
  },
  {
    icon: Fingerprint,
    title: "Identity fields",
    description:
      "Names, dates and other identity fields are identified and compared automatically.",
  },
  {
    icon: ShieldCheck,
    title: "Privacy-conscious processing",
    description:
      "Documents move through a secure workflow that's designed around handling sensitive information.",
  },
];

const STEPS: Step[] = [
  {
    number: "01",
    icon: UploadCloud,
    title: "Upload your document",
    description:
      "Choose a passport, visa, permit or supporting immigration document from your device.",
  },
  {
    number: "02",
    icon: Sparkles,
    title: "Let AI review it",
    description:
      "The workflow reads the document and organizes what it finds for you to review.",
  },
  {
    number: "03",
    icon: FileCheck2,
    title: "Review the findings",
    description:
      "See extracted information, checks, and anything that might need your attention.",
  },
];

const REVIEW_ITEMS: ReviewItem[] = [
  {
    title: "Document information extracted",
    description: "Key fields were identified from the uploaded document.",
    status: "checked",
  },
  {
    title: "Identity information reviewed",
    description: "Relevant identity fields were identified for comparison.",
    status: "checked",
  },
  {
    title: "Potential inconsistency",
    description: "One field may need a second look.",
    status: "warning",
  },
];

const AUDIENCES: Audience[] = [
  {
    icon: Users,
    title: "Individuals",
    description:
      "Understand your own documents before submitting or continuing with an immigration process.",
  },
  {
    icon: Globe2,
    title: "Immigration professionals",
    description:
      "Use AI-assisted review to organize and examine client documents more efficiently.",
  },
  {
    icon: FileText,
    title: "Organizations",
    description:
      "Support internal document workflows for recruitment, travel and immigration processes.",
  },
];

const DIFFERENCE_POINTS = [
  "Important information is organized for you",
  "Potential inconsistencies are highlighted",
  "You can inspect findings before taking the next step",
];

export default function HeroSection() {
  const isAuthenticated =
    typeof window !== "undefined" &&
    Boolean(window.localStorage.getItem("token"));
  const primaryAction = isAuthenticated ? "/upload" : "/register";

  return (
    <main className="bg-white text-slate-900">
      <Hero primaryAction={primaryAction} />
      <WhyThisExists primaryAction={primaryAction} />
      <HowItWorks />
      <Features />
      <ReviewExample />
      <WhoItsFor primaryAction={primaryAction} />
      <ResponsibleAi />
      <FinalCta primaryAction={primaryAction} isAuthenticated={isAuthenticated} />
    </main>
  );
}

function Hero({ primaryAction }: { primaryAction: string }) {
  return (
    <section
      id="home"
      aria-labelledby="hero-heading"
      className="relative overflow-hidden border-b border-slate-200 bg-[#fafaf8]"
    >
      <div aria-hidden className="pointer-events-none absolute inset-0">
        <div className="absolute -right-40 -top-40 h-[32rem] w-[32rem] rounded-full bg-[#F4B81A]/10 blur-3xl" />
        <div className="absolute -left-40 bottom-0 h-[24rem] w-[24rem] rounded-full bg-blue-100/40 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-7xl px-5 pb-20 pt-28 sm:px-6 lg:px-8 lg:pb-28 lg:pt-36">
        <div className="grid items-center gap-14 lg:grid-cols-[1.05fr_0.95fr] lg:gap-20">
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="order-2 max-w-2xl lg:order-1"
          >
            <p className="text-sm font-semibold text-[#A66F00]">
              AI-assisted immigration document review
            </p>

            <h1
              id="hero-heading"
              className="font-heading mt-4 text-4xl font-bold leading-[1.08] tracking-tight text-slate-950 sm:text-5xl md:text-6xl lg:text-[4.25rem]"
            >
              A simpler way to review your immigration documents.
            </h1>

            <p className="mt-7 max-w-xl text-base leading-8 text-slate-600 sm:text-lg">
              Upload your documents and use AI-assisted analysis to
              understand important information, spot inconsistencies, and
              organize your review before moving forward.
            </p>

            <div className="mt-9 flex flex-col gap-3 sm:flex-row">
              <Link
                to={primaryAction}
                className="group inline-flex min-h-12 items-center justify-center gap-2.5 rounded-lg bg-[#F4B81A] px-6 py-3.5 text-sm font-bold text-slate-950 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:bg-[#E8AB0D] hover:shadow-md focus:outline-none focus:ring-2 focus:ring-[#F4B81A] focus:ring-offset-2"
              >
                Start a document review
                <ArrowRight
                  size={18}
                  className="transition-transform group-hover:translate-x-1"
                />
              </Link>

              <a
                href="#how-it-works"
                className="inline-flex min-h-12 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-6 py-3.5 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
              >
                How it works
                <ChevronRight size={17} />
              </a>
            </div>

            <div className="mt-7 flex flex-wrap gap-x-6 gap-y-3 text-sm text-slate-500">
              <span className="flex items-center gap-2">
                <ShieldCheck size={16} className="text-emerald-600" />
                Secure processing
              </span>
              <span className="flex items-center gap-2">
                <Clock3 size={16} className="text-slate-500" />
                Fast analysis
              </span>
              <span className="flex items-center gap-2">
                <BadgeCheck size={16} className="text-slate-500" />
                AI-assisted
              </span>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.15 }}
            className="order-1 relative lg:order-2"
          >
            <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-[0_24px_70px_rgba(15,23,42,0.10)] sm:p-6">
              <div className="flex items-center justify-between border-b border-slate-100 pb-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-100">
                    <FileText size={19} className="text-slate-600" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-slate-900">passport_scan.pdf</p>
                    <p className="mt-1 text-xs text-slate-500">Document review</p>
                  </div>
                </div>
                <span className="rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700">
                  In review
                </span>
              </div>

              <div className="py-6">
                <div className="flex items-end justify-between">
                  <div>
                    <p className="text-xs font-medium text-slate-400">Review status</p>
                    <h2 className="mt-1 text-xl font-bold text-slate-900">
                      Initial review complete
                    </h2>
                  </div>
                  <div className="text-right">
                    <p className="text-2xl font-bold text-slate-900">98.7%</p>
                    <p className="text-[11px] text-slate-400">analysis confidence</p>
                  </div>
                </div>

                <div className="mt-4 h-2 overflow-hidden rounded-full bg-slate-100">
                  <div className="h-full w-[98.7%] rounded-full bg-[#F4B81A]" />
                </div>
              </div>

              <div className="space-y-3">
                {REVIEW_ITEMS.map((item) => {
                  const checked = item.status === "checked";
                  return (
                    <div
                      key={item.title}
                      className={`rounded-xl border p-4 ${
                        checked
                          ? "border-slate-100 bg-slate-50"
                          : "border-amber-200 bg-amber-50/60"
                      }`}
                    >
                      <div className="flex gap-3">
                        <div
                          className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${
                            checked
                              ? "bg-emerald-50 text-emerald-600"
                              : "bg-amber-100 text-amber-700"
                          }`}
                        >
                          {checked ? <CheckCircle2 size={17} /> : <Info size={17} />}
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-slate-800">{item.title}</p>
                          <p className="mt-1 text-xs leading-5 text-slate-500">
                            {item.description}
                          </p>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-5">
                <span className="text-xs text-slate-400">Last updated just now</span>
                <span className="flex items-center gap-1.5 text-xs font-medium text-slate-600">
                  <LockKeyhole size={14} />
                  Protected workflow
                </span>
              </div>
            </div>

            <div className="absolute -bottom-5 -left-5 hidden rounded-xl border border-slate-200 bg-white px-4 py-3 shadow-lg sm:block">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#F4B81A]/15">
                  <FileCheck2 size={17} className="text-[#A66F00]" />
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-800">Review organized</p>
                  <p className="mt-0.5 text-[11px] text-slate-500">Ready to inspect</p>
                </div>
              </div>
            </div>
          </motion.div>
        </div>

        <div className="mt-20 border-t border-slate-200 pt-8">
          <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <p className="text-sm font-medium text-slate-500">
              Review common immigration documents including:
            </p>
            <div className="flex flex-wrap gap-2">
              {DOCUMENT_TYPES.map((item) => (
                <span
                  key={item}
                  className="rounded-full border border-slate-200 bg-white px-3.5 py-2 text-xs font-medium text-slate-600"
                >
                  {item}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function WhyThisExists({ primaryAction }: { primaryAction: string }) {
  return (
    <section className="bg-white py-24 sm:py-28">
      <div className="mx-auto max-w-7xl px-5 sm:px-6 lg:px-8">
        <div className="grid gap-14 lg:grid-cols-[0.8fr_1.2fr] lg:gap-24">
          <h2 className="max-w-lg text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            Immigration paperwork can be hard to review on your own.
          </h2>

          <div className="max-w-2xl">
            <p className="text-base leading-8 text-slate-600 sm:text-lg">
              Immigration documents contain names, dates, numbers and
              conditions that all need careful attention. Reviewing several
              documents by hand takes time, and it's not always obvious what
              deserves a closer look.
            </p>

            <p className="mt-5 text-base leading-8 text-slate-600">
              MukondoGTech AI Platform adds a layer of document intelligence
              on top of your own judgment — not instead of it. It organizes
              information and points out what might be worth a second look.
            </p>

            <Link
              to={primaryAction}
              className="group mt-7 inline-flex items-center gap-2 text-sm font-bold text-slate-900"
            >
              Review a document
              <ArrowRight size={17} className="transition-transform group-hover:translate-x-1" />
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

function HowItWorks() {
  return (
    <section id="how-it-works" className="border-y border-slate-200 bg-slate-50 py-24 sm:py-28">
      <div className="mx-auto max-w-7xl px-5 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            From document to review in three steps.
          </h2>
          <p className="mt-5 text-base leading-7 text-slate-600">
            Upload your document, let the system analyze it, then review
            what it finds. That's the whole process.
          </p>
        </div>

        <div className="relative mt-16 grid gap-8 md:grid-cols-3">
          <div
            aria-hidden
            className="absolute left-[16.66%] right-[16.66%] top-9 hidden h-px bg-slate-200 md:block"
          />

          {STEPS.map((step) => (
            <motion.div
              key={step.number}
              initial={{ opacity: 0, y: 15 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.3 }}
              transition={{ duration: 0.45 }}
              className="relative text-center"
            >
              <div className="relative mx-auto flex h-[4.5rem] w-[4.5rem] items-center justify-center rounded-full border border-slate-200 bg-white shadow-sm">
                <step.icon size={22} className="text-slate-700" />
                <span className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-[#F4B81A] text-[10px] font-bold text-slate-950">
                  {step.number}
                </span>
              </div>
              <h3 className="mt-7 text-lg font-bold text-slate-900">{step.title}</h3>
              <p className="mx-auto mt-3 max-w-sm text-sm leading-6 text-slate-600">
                {step.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}

function Features() {
  return (
    <section className="bg-white py-24 sm:py-28">
      <div className="mx-auto max-w-7xl px-5 sm:px-6 lg:px-8">
        <div className="grid items-end gap-8 lg:grid-cols-2">
          <h2 className="max-w-2xl text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            Useful document intelligence, without the clutter.
          </h2>
          <p className="max-w-xl text-base leading-7 text-slate-600 lg:justify-self-end">
            The platform sticks to practical tasks that make document review
            easier to follow and manage.
          </p>
        </div>

        <div className="mt-14 grid gap-px overflow-hidden rounded-2xl border border-slate-200 bg-slate-200 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((feature) => (
            <div key={feature.title} className="bg-white p-7 transition-colors hover:bg-slate-50">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-slate-100">
                <feature.icon size={20} className="text-slate-700" />
              </div>
              <h3 className="mt-6 text-base font-bold text-slate-900">{feature.title}</h3>
              <p className="mt-3 text-sm leading-6 text-slate-600">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function ReviewExample() {
  return (
    <section className="bg-[#101827] py-24 text-white sm:py-28">
      <div className="mx-auto max-w-7xl px-5 sm:px-6 lg:px-8">
        <div className="grid items-center gap-14 lg:grid-cols-2 lg:gap-24">
          <div>
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Turn a document into something easier to understand.
            </h2>

            <p className="mt-6 max-w-xl text-base leading-8 text-slate-300">
              Rather than handing you a file and a long list of fields, the
              platform organizes what it finds into a simple review
              experience.
            </p>

            <div className="mt-8 space-y-4">
              {DIFFERENCE_POINTS.map((point) => (
                <div key={point} className="flex items-start gap-3">
                  <CheckCircle2 size={19} className="mt-0.5 shrink-0 text-[#F4B81A]" />
                  <span className="text-sm leading-6 text-slate-300">{point}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/[0.04] p-5 sm:p-7">
            <div className="flex items-center justify-between border-b border-white/10 pb-5">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/[0.07]">
                  <FileText size={18} className="text-slate-300" />
                </div>
                <div>
                  <p className="text-sm font-semibold">visa_application.pdf</p>
                  <p className="mt-1 text-xs text-slate-500">Analysis results</p>
                </div>
              </div>
              <span className="rounded-full bg-emerald-400/10 px-3 py-1.5 text-xs font-semibold text-emerald-300">
                Completed
              </span>
            </div>

            <div className="py-6">
              <p className="text-xs font-semibold text-slate-500">Document overview</p>
              <div className="mt-5 grid grid-cols-2 gap-3">
                <div className="rounded-xl border border-white/10 bg-white/[0.03] p-4">
                  <p className="text-xs text-slate-500">Document type</p>
                  <p className="mt-2 text-sm font-semibold">Visa document</p>
                </div>
                <div className="rounded-xl border border-white/10 bg-white/[0.03] p-4">
                  <p className="text-xs text-slate-500">Fields found</p>
                  <p className="mt-2 text-sm font-semibold">18 fields</p>
                </div>
              </div>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between rounded-xl border border-emerald-400/10 bg-emerald-400/[0.05] px-4 py-3">
                <div className="flex items-center gap-3">
                  <CheckCircle2 size={17} className="text-emerald-400" />
                  <span className="text-sm text-slate-300">Required information identified</span>
                </div>
                <span className="text-xs text-emerald-300">Checked</span>
              </div>

              <div className="flex items-center justify-between rounded-xl border border-emerald-400/10 bg-emerald-400/[0.05] px-4 py-3">
                <div className="flex items-center gap-3">
                  <CheckCircle2 size={17} className="text-emerald-400" />
                  <span className="text-sm text-slate-300">Identity fields identified</span>
                </div>
                <span className="text-xs text-emerald-300">Checked</span>
              </div>

              <div className="flex items-center justify-between rounded-xl border border-amber-400/10 bg-amber-400/[0.05] px-4 py-3">
                <div className="flex items-center gap-3">
                  <Info size={17} className="text-amber-300" />
                  <span className="text-sm text-slate-300">One item requires attention</span>
                </div>
                <span className="text-xs text-amber-300">Review</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function WhoItsFor({ primaryAction }: { primaryAction: string }) {
  return (
    <section className="bg-slate-50 py-24 sm:py-28">
      <div className="mx-auto max-w-7xl px-5 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">
            Built around real document-review needs.
          </h2>
          <p className="mt-5 text-base leading-7 text-slate-600">
            Whether you're reviewing your own documents or working with them
            professionally, the workflow stays the same.
          </p>
        </div>

        <div className="mt-14 grid gap-5 lg:grid-cols-3">
          {AUDIENCES.map((audience) => (
            <div key={audience.title} className="rounded-2xl border border-slate-200 bg-white p-7 shadow-sm">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-slate-100">
                <audience.icon size={20} className="text-slate-700" />
              </div>
              <h3 className="mt-6 text-lg font-bold text-slate-900">{audience.title}</h3>
              <p className="mt-3 text-sm leading-6 text-slate-600">{audience.description}</p>
              <Link
                to={primaryAction}
                className="group mt-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-800"
              >
                Get started
                <ArrowRight size={16} className="transition-transform group-hover:translate-x-1" />
              </Link>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function ResponsibleAi() {
  return (
    <section className="bg-white py-24 sm:py-28">
      <div className="mx-auto max-w-5xl px-5 sm:px-6 lg:px-8">
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-8 sm:p-10">
          <div className="grid gap-8 md:grid-cols-[auto_1fr] md:items-start">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white shadow-sm">
              <ShieldCheck size={23} className="text-emerald-600" />
            </div>

            <div>
              <p className="text-sm font-bold text-slate-900">
                Designed with responsible document review in mind
              </p>
              <h2 className="mt-2 text-2xl font-bold tracking-tight text-slate-950">
                AI should assist your review — not make the final decision.
              </h2>
              <p className="mt-4 text-sm leading-7 text-slate-600">
                Results from the platform are meant to help you understand
                and review documents. Check them carefully — they're not a
                substitute for official immigration authorities, qualified
                legal professionals, or other appropriate experts.
              </p>

              <div className="mt-6 flex flex-wrap gap-3">
                <span className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600">
                  <LockKeyhole size={14} />
                  Privacy-conscious
                </span>
                <span className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600">
                  <SearchCheck size={14} />
                  Review-focused
                </span>
                <span className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-600">
                  <BadgeCheck size={14} />
                  AI-assisted
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function FinalCta({
  primaryAction,
  isAuthenticated,
}: {
  primaryAction: string;
  isAuthenticated: boolean;
}) {
  return (
    <section className="border-t border-slate-200 bg-[#fafaf8] py-24 sm:py-28">
      <div className="mx-auto max-w-4xl px-5 text-center sm:px-6 lg:px-8">
        <h2 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-5xl">
          Start with the document you already have.
        </h2>
        <p className="mx-auto mt-5 max-w-2xl text-base leading-7 text-slate-600 sm:text-lg">
          Upload a document and see how AI-assisted analysis organizes the
          information and flags what might need a closer look.
        </p>

        <div className="mt-9 flex flex-col justify-center gap-3 sm:flex-row">
          <Link
            to={primaryAction}
            className="inline-flex min-h-12 items-center justify-center gap-2.5 rounded-lg bg-[#F4B81A] px-7 py-3.5 text-sm font-bold text-slate-950 shadow-sm transition hover:bg-[#E8AB0D] hover:shadow-md"
          >
            Start a document review
            <ArrowRight size={18} />
          </Link>

          {!isAuthenticated && (
            <Link
              to="/login"
              className="inline-flex min-h-12 items-center justify-center rounded-lg border border-slate-300 bg-white px-7 py-3.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-50"
            >
              Sign in
            </Link>
          )}
        </div>
      </div>
    </section>
  );
}
