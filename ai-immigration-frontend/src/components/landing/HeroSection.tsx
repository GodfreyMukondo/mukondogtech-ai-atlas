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
  Users,
  type LucideIcon,
} from "lucide-react";

interface Feature {
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

export default function HeroSection() {
  const isAuthenticated =
    typeof window !== "undefined" &&
    Boolean(window.localStorage.getItem("token"));
  const primaryAction = isAuthenticated ? "/upload" : "/register";

  return (
    <main>
      <Hero primaryAction={primaryAction} />
      <WhyThisExists primaryAction={primaryAction} />
      <Features />
      <WhoItsFor primaryAction={primaryAction} />
    </main>
  );
}

function Hero({ primaryAction }: { primaryAction: string }) {
  return (
    <section
      id="home"
      aria-labelledby="hero-heading"
      className="relative overflow-hidden border-b border-white/10 bg-gradient-to-br from-[#0B1F3A] via-[#1F314A] to-[#071426]"
    >
      <div aria-hidden className="pointer-events-none absolute inset-0">
        <div className="absolute -right-40 -top-40 h-[32rem] w-[32rem] rounded-full bg-[#C6A15B]/10 blur-3xl" />
        <div className="absolute -left-40 bottom-0 h-[24rem] w-[24rem] rounded-full bg-blue-500/10 blur-3xl" />
        <div className="absolute left-1/3 top-1/4 h-[20rem] w-[20rem] rounded-full bg-emerald-500/10 blur-3xl" />
        <div className="absolute -bottom-24 right-1/4 h-[18rem] w-[18rem] rounded-full bg-pink-500/10 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-7xl px-5 pb-20 pt-28 sm:px-6 lg:px-8 lg:pb-28 lg:pt-36">
        <div className="grid items-center gap-14 lg:grid-cols-[1.05fr_0.95fr] lg:gap-20">
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="order-2 max-w-2xl lg:order-1"
          >
            <p className="text-sm font-semibold text-[#C6A15B]">
              AI-assisted immigration document review
            </p>

            <h1
              id="hero-heading"
              className="font-heading mt-4 text-4xl font-bold leading-[1.08] tracking-tight text-white sm:text-5xl md:text-6xl lg:text-[4.25rem]"
            >
              A simpler way to review your immigration documents.
            </h1>

            <p className="mt-7 max-w-xl text-base leading-8 text-slate-300 sm:text-lg">
              Upload your documents and use AI-assisted analysis to
              understand important information, spot inconsistencies, and
              organize your review before moving forward.
            </p>

            <div className="mt-9 flex flex-col gap-3 sm:flex-row">
              <Link
                to={primaryAction}
                className="group inline-flex min-h-12 items-center justify-center gap-2.5 rounded-lg bg-[#C6A15B] px-6 py-3.5 text-sm font-bold text-slate-950 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:bg-[#A8894D] hover:shadow-md focus:outline-none focus:ring-2 focus:ring-[#C6A15B] focus:ring-offset-2"
              >
                Start a document review
                <ArrowRight
                  size={18}
                  className="transition-transform group-hover:translate-x-1"
                />
              </Link>

              <a
                href="#how-it-works"
                className="inline-flex min-h-12 items-center justify-center gap-2 rounded-lg border border-white/15 bg-white/5 px-6 py-3.5 text-sm font-semibold text-white transition hover:border-white/25 hover:bg-white/10"
              >
                How it works
                <ChevronRight size={17} />
              </a>
            </div>

            <div className="mt-7 flex flex-wrap gap-x-6 gap-y-3 text-sm text-slate-400">
              <span className="flex items-center gap-2">
                <ShieldCheck size={16} className="text-emerald-400" />
                Secure processing
              </span>
              <span className="flex items-center gap-2">
                <Clock3 size={16} className="text-slate-400" />
                Fast analysis
              </span>
              <span className="flex items-center gap-2">
                <BadgeCheck size={16} className="text-slate-400" />
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
            <div className="rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-4 shadow-[0_24px_70px_rgba(7, 20, 38,0.35)] sm:p-6">
              <div className="flex items-center justify-between border-b border-white/10 pb-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-white/10">
                    <FileText size={19} className="text-slate-300" />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-white">passport_scan.pdf</p>
                    <p className="mt-1 text-xs text-slate-400">Document review</p>
                  </div>
                </div>
                <span className="rounded-full bg-emerald-500/15 px-3 py-1.5 text-xs font-semibold text-emerald-300">
                  In review
                </span>
              </div>

              <div className="py-6">
                <div className="flex items-end justify-between">
                  <div>
                    <p className="text-xs font-medium text-slate-400">Review status</p>
                    <h2 className="mt-1 text-xl font-bold text-white">
                      Initial review complete
                    </h2>
                  </div>
                  <div className="text-right">
                    <p className="text-2xl font-bold text-white">98.7%</p>
                    <p className="text-[11px] text-slate-400">analysis confidence</p>
                  </div>
                </div>

                <div className="mt-4 h-2 overflow-hidden rounded-full bg-white/10">
                  <div className="h-full w-[98.7%] rounded-full bg-[#C6A15B]" />
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
                          ? "border-white/10 bg-white/5"
                          : "border-amber-500/30 bg-amber-500/10"
                      }`}
                    >
                      <div className="flex gap-3">
                        <div
                          className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${
                            checked
                              ? "bg-emerald-500/15 text-emerald-300"
                              : "bg-amber-500/15 text-amber-300"
                          }`}
                        >
                          {checked ? <CheckCircle2 size={17} /> : <Info size={17} />}
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-white">{item.title}</p>
                          <p className="mt-1 text-xs leading-5 text-slate-400">
                            {item.description}
                          </p>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="mt-5 flex items-center justify-between border-t border-white/10 pt-5">
                <span className="text-xs text-slate-400">Last updated just now</span>
                <span className="flex items-center gap-1.5 text-xs font-medium text-slate-300">
                  <LockKeyhole size={14} />
                  Protected workflow
                </span>
              </div>
            </div>

            <div className="absolute -bottom-5 -left-5 hidden rounded-xl border border-white/10 bg-[#1F314A] px-4 py-3 shadow-lg sm:block">
              <div className="flex items-center gap-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#C6A15B]/15">
                  <FileCheck2 size={17} className="text-[#C6A15B]" />
                </div>
                <div>
                  <p className="text-xs font-bold text-white">Review organized</p>
                  <p className="mt-0.5 text-[11px] text-slate-400">Ready to inspect</p>
                </div>
              </div>
            </div>
          </motion.div>
        </div>

        <div className="mt-20 border-t border-white/10 pt-8">
          <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <p className="text-sm font-medium text-slate-400">
              Review common immigration documents including:
            </p>
            <div className="flex flex-wrap gap-2">
              {DOCUMENT_TYPES.map((item) => (
                <span
                  key={item}
                  className="rounded-full border border-white/15 bg-white/5 px-3.5 py-2 text-xs font-medium text-slate-300"
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
    <section className="bg-blue-950 py-24 sm:py-28">
      <div className="mx-auto max-w-7xl px-5 sm:px-6 lg:px-8">
        <div className="grid gap-14 lg:grid-cols-[0.8fr_1.2fr] lg:gap-24">
          <h2 className="max-w-lg text-3xl font-bold tracking-tight text-white sm:text-4xl">
            Immigration paperwork can be hard to review on your own.
          </h2>

          <div className="max-w-2xl">
            <p className="text-base leading-8 text-blue-100 sm:text-lg">
              Immigration documents contain names, dates, numbers and
              conditions that all need careful attention. Reviewing several
              documents by hand takes time, and it's not always obvious what
              deserves a closer look.
            </p>

            <p className="mt-5 text-base leading-8 text-blue-100">
              MukondoGTech AI Platform adds a layer of document intelligence
              on top of your own judgment — not instead of it. It organizes
              information and points out what might be worth a second look.
            </p>

            <Link
              to={primaryAction}
              className="group mt-7 inline-flex items-center gap-2 text-sm font-bold text-white"
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

function Features() {
  return (
    <section className="py-24 sm:py-28">
      <div className="mx-auto max-w-7xl px-5 sm:px-6 lg:px-8">
        <div className="grid items-end gap-8 lg:grid-cols-2">
          <h2 className="max-w-2xl text-3xl font-bold tracking-tight text-white sm:text-4xl">
            Useful document intelligence, without the clutter.
          </h2>
          <p className="max-w-xl text-base leading-7 text-slate-300 lg:justify-self-end">
            The platform sticks to practical tasks that make document review
            easier to follow and manage.
          </p>
        </div>

        <div className="mt-14 grid gap-px overflow-hidden rounded-2xl border border-white/10 bg-white/10 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((feature) => (
            <div key={feature.title} className="bg-[#0B1F3A] p-7 transition-colors hover:bg-white/5">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-white/10">
                <feature.icon size={20} className="text-slate-300" />
              </div>
              <h3 className="mt-6 text-base font-bold text-white">{feature.title}</h3>
              <p className="mt-3 text-sm leading-6 text-slate-300">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function WhoItsFor({ primaryAction }: { primaryAction: string }) {
  return (
    <section className="bg-gradient-to-l from-[#1F314A] to-[#071426] py-24 sm:py-28">
      <div className="mx-auto max-w-7xl px-5 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-bold tracking-tight text-white sm:text-4xl">
            Built around real document-review needs.
          </h2>
          <p className="mt-5 text-base leading-7 text-slate-300">
            Whether you're reviewing your own documents or working with them
            professionally, the workflow stays the same.
          </p>
        </div>

        <div className="mt-14 grid gap-5 lg:grid-cols-3">
          {AUDIENCES.map((audience) => (
            <div key={audience.title} className="rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-7 shadow-sm">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-white/10">
                <audience.icon size={20} className="text-slate-300" />
              </div>
              <h3 className="mt-6 text-lg font-bold text-white">{audience.title}</h3>
              <p className="mt-3 text-sm leading-6 text-slate-300">{audience.description}</p>
              <Link
                to={primaryAction}
                className="group mt-6 inline-flex items-center gap-2 text-sm font-semibold text-white"
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

