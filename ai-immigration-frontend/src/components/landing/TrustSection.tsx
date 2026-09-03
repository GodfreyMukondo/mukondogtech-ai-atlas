
// src/components/landing/TrustSection.tsx

import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import {
  ArrowRight,
  BrainCircuit,
  CheckCircle2,
  Cloud,
  FileCheck,
  Globe2,
  LockKeyhole,
  ShieldCheck,
  Sparkles,
  type LucideIcon,
} from "lucide-react";

interface TrustFeature {
  icon: LucideIcon;
  title: string;
  description: string;
}

const TRUST_FEATURES: TrustFeature[] = [
  {
    icon: ShieldCheck,
    title: "Secure Platform Architecture",
    description:
      "Built around secure workflows designed to protect sensitive immigration documents throughout the analysis process.",
  },
  {
    icon: LockKeyhole,
    title: "Privacy-First Protection",
    description:
      "Sensitive information is handled through controlled access and privacy-conscious practices designed to reduce unnecessary exposure.",
  },
  {
    icon: BrainCircuit,
    title: "Advanced AI Intelligence",
    description:
      "AI-assisted analysis helps identify relevant information, potential issues, and patterns within immigration documents.",
  },
  {
    icon: FileCheck,
    title: "Smart Document Verification",
    description:
      "Review document information, identify potential inconsistencies, and highlight areas that may require additional attention.",
  },
  {
    icon: Cloud,
    title: "Reliable Cloud Workspace",
    description:
      "Manage document analysis workflows through a centralized cloud-based environment designed for convenient access.",
  },
  {
    icon: Globe2,
    title: "Global Immigration Support",
    description:
      "Designed to support applicants, consultants, legal professionals, and organizations working across different immigration processes.",
  },
];

const SECURITY_BENEFITS = [
  "Encrypted document processing workflows",
  "AI-assisted immigration insights",
  "Responsible and transparent AI technology",
  "Secure cloud-based document management",
];

export default function TrustSection() {
  return (
    <section
      id="security"
      aria-labelledby="trust-heading"
      className="
        relative
        isolate
        overflow-hidden
        bg-[#07111F]
        py-24
        text-white
        sm:py-28
        lg:py-32
      "
    >
      {/* =========================================================
          BACKGROUND ATMOSPHERE
          ========================================================= */}

      <div
        aria-hidden="true"
        className="
          pointer-events-none
          absolute
          -left-40
          top-0
          -z-10
          h-[32rem]
          w-[32rem]
          rounded-full
          bg-[#F4B81A]/[0.07]
          blur-[130px]
        "
      />

      <div
        aria-hidden="true"
        className="
          pointer-events-none
          absolute
          -bottom-48
          -right-40
          -z-10
          h-[34rem]
          w-[34rem]
          rounded-full
          bg-emerald-500/[0.06]
          blur-[140px]
        "
      />

      <div
        aria-hidden="true"
        className="
          pointer-events-none
          absolute
          left-1/2
          top-1/3
          -z-10
          h-[22rem]
          w-[22rem]
          -translate-x-1/2
          rounded-full
          bg-sky-500/[0.035]
          blur-[120px]
        "
      />

      {/* =========================================================
          SUBTLE GRID
          ========================================================= */}

      <div
        aria-hidden="true"
        className="
          pointer-events-none
          absolute
          inset-0
          -z-10
          opacity-[0.025]
          [background-image:linear-gradient(rgba(255,255,255,0.8)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.8)_1px,transparent_1px)]
          [background-size:64px_64px]
        "
      />

      {/* =========================================================
          TOP EDGE
          ========================================================= */}

      <div
        aria-hidden="true"
        className="
          pointer-events-none
          absolute
          left-1/2
          top-0
          h-px
          w-full
          -translate-x-1/2
          bg-gradient-to-r
          from-transparent
          via-[#F4B81A]/20
          to-transparent
        "
      />

      {/* =========================================================
          CONTENT
          ========================================================= */}

      <div
        className="
          relative
          mx-auto
          w-full
          max-w-7xl
          px-5
          sm:px-6
          lg:px-8
        "
      >
        {/* =======================================================
            SECTION HEADER
            ======================================================= */}

        <motion.div
          initial={{
            opacity: 0,
            y: 24,
          }}
          whileInView={{
            opacity: 1,
            y: 0,
          }}
          viewport={{
            once: true,
            amount: 0.2,
          }}
          transition={{
            duration: 0.6,
            ease: "easeOut",
          }}
          className="
            mx-auto
            max-w-3xl
            text-center
          "
        >
          {/* Section label */}

          <div
            className="
              inline-flex
              items-center
              gap-2.5
              rounded-full
              border
              border-[#F4B81A]/25
              bg-[#F4B81A]/[0.06]
              px-4
              py-2
              text-xs
              font-bold
              uppercase
              tracking-[0.18em]
              text-[#FFE08A]
              backdrop-blur-sm
              sm:text-sm
            "
          >
            <Sparkles
              size={15}
              aria-hidden="true"
            />

            <span>
              Trusted AI Immigration Technology
            </span>
          </div>

          {/* Heading */}

          <h2
            id="trust-heading"
            className="
              mt-7
              text-3xl
              font-extrabold
              leading-[1.12]
              tracking-[-0.025em]
              text-white
              sm:text-4xl
              md:text-5xl
            "
          >
            Technology designed around

            <span
              className="
                mt-2
                block
                bg-gradient-to-r
                from-[#F4B81A]
                via-[#FFE08A]
                to-emerald-300
                bg-clip-text
                text-transparent
              "
            >
              trust, security and intelligence
            </span>
          </h2>

          {/* Description */}

          <p
            className="
              mt-6
              text-base
              leading-8
              text-slate-300
              sm:text-lg
            "
          >
            MukondoGTech AI combines document intelligence,
            artificial intelligence, and secure cloud
            technology to make immigration document analysis
            more efficient while keeping responsible data
            handling at the center of the experience.
          </p>
        </motion.div>

        {/* =======================================================
            FEATURE GRID
            ======================================================= */}

        <div
          className="
            mt-16
            grid
            gap-5
            sm:gap-6
            md:grid-cols-2
            lg:mt-20
            lg:grid-cols-3
          "
        >
          {TRUST_FEATURES.map((feature, index) => {
            const Icon = feature.icon;

            return (
              <motion.article
                key={feature.title}
                initial={{
                  opacity: 0,
                  y: 28,
                }}
                whileInView={{
                  opacity: 1,
                  y: 0,
                }}
                viewport={{
                  once: true,
                  amount: 0.15,
                }}
                transition={{
                  duration: 0.5,
                  delay: index * 0.07,
                  ease: "easeOut",
                }}
                whileHover={{
                  y: -5,
                }}
                className="
                  group
                  relative
                  overflow-hidden
                  rounded-2xl
                  border
                  border-white/[0.09]
                  bg-white/[0.025]
                  p-7
                  backdrop-blur-sm
                  transition-colors
                  duration-300
                  hover:border-[#F4B81A]/25
                  hover:bg-white/[0.045]
                  sm:p-8
                "
              >
                {/* Card glow */}

                <div
                  aria-hidden="true"
                  className="
                    pointer-events-none
                    absolute
                    -right-12
                    -top-12
                    h-32
                    w-32
                    rounded-full
                    bg-[#F4B81A]/[0.07]
                    blur-3xl
                    transition-opacity
                    duration-300
                    group-hover:bg-[#F4B81A]/[0.11]
                  "
                />

                {/* Icon */}

                <div
                  className="
                    relative
                    flex
                    h-12
                    w-12
                    items-center
                    justify-center
                    rounded-xl
                    border
                    border-[#F4B81A]/20
                    bg-gradient-to-br
                    from-[#F4B81A]
                    to-[#FFE08A]
                    shadow-[0_8px_25px_rgba(244,184,26,0.12)]
                    transition-transform
                    duration-300
                    group-hover:scale-105
                  "
                >
                  <Icon
                    size={22}
                    className="text-[#07111F]"
                    aria-hidden="true"
                  />
                </div>

                {/* Content */}

                <div className="relative">
                  <h3
                    className="
                      mt-6
                      text-lg
                      font-bold
                      tracking-tight
                      text-white
                      sm:text-xl
                    "
                  >
                    {feature.title}
                  </h3>

                  <p
                    className="
                      mt-3
                      text-sm
                      leading-7
                      text-slate-400
                      sm:text-base
                    "
                  >
                    {feature.description}
                  </p>
                </div>

                {/* Bottom accent */}

                <div
                  aria-hidden="true"
                  className="
                    relative
                    mt-6
                    h-px
                    w-10
                    bg-[#F4B81A]/60
                    transition-all
                    duration-300
                    group-hover:w-16
                    group-hover:bg-[#FFE08A]
                  "
                />
              </motion.article>
            );
          })}
        </div>

        {/* =======================================================
            SECURITY CTA
            ======================================================= */}

        <motion.div
          initial={{
            opacity: 0,
            y: 28,
          }}
          whileInView={{
            opacity: 1,
            y: 0,
          }}
          viewport={{
            once: true,
            amount: 0.2,
          }}
          transition={{
            duration: 0.6,
            ease: "easeOut",
          }}
          className="
            relative
            mt-16
            overflow-hidden
            rounded-3xl
            border
            border-[#F4B81A]/20
            bg-gradient-to-br
            from-[#FFF9E8]
            via-[#F8F6F1]
            to-[#ECFDF5]
            p-7
            text-[#07111F]
            shadow-[0_25px_80px_rgba(0,0,0,0.22)]
            sm:p-10
            lg:mt-20
            lg:p-12
          "
        >
          {/* Decorative glow */}

          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              -right-20
              -top-20
              h-64
              w-64
              rounded-full
              bg-[#F4B81A]/20
              blur-[90px]
            "
          />

          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              -bottom-24
              left-1/3
              h-56
              w-56
              rounded-full
              bg-emerald-400/10
              blur-[80px]
            "
          />

          <div
            className="
              relative
              flex
              flex-col
              gap-10
              lg:flex-row
              lg:items-center
              lg:justify-between
            "
          >
            {/* CTA content */}

            <div className="max-w-2xl">
              <div
                className="
                  inline-flex
                  items-center
                  gap-2
                  text-xs
                  font-bold
                  uppercase
                  tracking-[0.16em]
                  text-emerald-700
                "
              >
                <ShieldCheck
                  size={16}
                  aria-hidden="true"
                />

                <span>
                  Security &amp; Responsible AI
                </span>
              </div>

              <h3
                className="
                  mt-4
                  text-2xl
                  font-extrabold
                  leading-tight
                  tracking-tight
                  sm:text-3xl
                "
              >
                Your immigration documents
                deserve trusted technology.
              </h3>

              <p
                className="
                  mt-4
                  max-w-xl
                  text-sm
                  leading-7
                  text-slate-600
                  sm:text-base
                "
              >
                We combine secure workflows, responsible
                AI practices, and transparent technology
                to help users review immigration documents
                with greater confidence.
              </p>

              {/* Benefits */}

              <div
                className="
                  mt-7
                  grid
                  gap-3
                  sm:grid-cols-2
                "
              >
                {SECURITY_BENEFITS.map((benefit) => (
                  <div
                    key={benefit}
                    className="
                      flex
                      items-start
                      gap-2.5
                      text-sm
                      font-medium
                      text-slate-700
                    "
                  >
                    <CheckCircle2
                      size={18}
                      className="
                        mt-0.5
                        shrink-0
                        text-emerald-600
                      "
                      aria-hidden="true"
                    />

                    <span>{benefit}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* CTA */}

            <div className="shrink-0">
              <Link
                to="/security"
                className="
                  group
                  inline-flex
                  min-h-13
                  items-center
                  justify-center
                  gap-3
                  rounded-xl
                  border
                  border-[#07111F]
                  bg-[#07111F]
                  px-7
                  py-4
                  text-sm
                  font-bold
                  text-white
                  shadow-[0_12px_30px_rgba(7,17,31,0.18)]
                  transition-all
                  duration-300
                  hover:-translate-y-1
                  hover:bg-[#101D31]
                  hover:shadow-[0_18px_40px_rgba(7,17,31,0.25)]
                  focus:outline-none
                  focus:ring-2
                  focus:ring-[#F4B81A]
                  focus:ring-offset-2
                  focus:ring-offset-[#F8F6F1]
                "
              >
                <span>
                  Explore Security
                </span>

                <ArrowRight
                  size={18}
                  aria-hidden="true"
                  className="
                    transition-transform
                    duration-300
                    group-hover:translate-x-1
                  "
                />
              </Link>
            </div>
          </div>
        </motion.div>
      </div>

      {/* =========================================================
          BOTTOM EDGE
          ========================================================= */}

      <div
        aria-hidden="true"
        className="
          pointer-events-none
          absolute
          bottom-0
          left-0
          right-0
          h-px
          bg-gradient-to-r
          from-transparent
          via-white/10
          to-transparent
        "
      />
    </section>
  );
}

