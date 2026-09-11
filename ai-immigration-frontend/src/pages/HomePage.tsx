// src/pages/HomePage.tsx

import HeroSection from "../components/landing/HeroSection";
import TrustSection from "../components/landing/TrustSection";
import FeaturesSection from "../components/landing/FeaturesSection";
import HowItWorks from "../components/landing/HowItWorks";
import DemoSection from "../components/landing/DemoSection";
import UseCasesSection from "../components/landing/UseCasesSection";
import SecuritySection from "../components/landing/SecuritySection";
import PricingPreview from "../components/landing/PricingPreview";
import CTASection from "../components/landing/CTASection";

/**
 * ================================================================
 * HOME PAGE
 * ================================================================
 *
 * Public landing page for the MukondoGTech AI Immigration platform.
 *
 * Responsibilities:
 * - Compose landing-page sections in the correct product flow.
 * - Provide semantic page structure.
 * - Provide consistent section spacing and scroll behaviour.
 * - Establish the global visual background.
 * - Keep individual section components isolated and maintainable.
 *
 * Each child section owns its:
 * - Content
 * - Internal layout
 * - Responsive behaviour
 * - Animation
 * - Component-level styling
 *
 * This page intentionally remains lightweight.
 * ================================================================
 */

export default function HomePage() {
  return (
    <div
      className="
        min-h-screen
        overflow-x-hidden
        text-slate-100
        antialiased
        selection:bg-[#C6A15B]/30
        selection:text-[#071426]
      "
    >
      <main className="relative flex flex-col">

        {/* =========================================================
            HERO
            ========================================================= */}

        <section
          id="home"
          aria-label="MukondoGTech AI Immigration platform"
          className="
            relative
            scroll-mt-24
            overflow-hidden
            bg-[#071426]
          "
        >
          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              inset-0
              bg-[radial-gradient(circle_at_top_right,_rgba(198, 161, 91,0.14),_transparent_34%)]
            "
          />

          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              bottom-0
              left-0
              h-72
              w-72
              bg-[radial-gradient(circle,_rgba(60, 76, 97,0.45),_transparent_70%)]
            "
          />

          <div className="relative z-10">
            <HeroSection />
          </div>
        </section>

        {/* =========================================================
            TRUST & CREDIBILITY
            ========================================================= */}

        <section
          aria-label="Platform trust and credibility"
          className="
            relative
            border-b
            border-white/10
          "
        >
          <TrustSection />
        </section>

        {/* =========================================================
            FEATURES
            ========================================================= */}

        <section
          id="features"
          aria-label="MukondoGTech AI platform features"
          className="
            relative
            scroll-mt-24
            overflow-hidden
            bg-[#F7F9FC]
          "
        >
          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              left-0
              top-1/4
              h-80
              w-80
              bg-[radial-gradient(circle,_rgba(60, 76, 97,0.05),_transparent_70%)]
            "
          />

          <div className="relative z-10">
            <FeaturesSection />
          </div>
        </section>

        {/* =========================================================
            HOW IT WORKS
            ========================================================= */}

        <section
          id="how-it-works"
          aria-label="How MukondoGTech AI works"
          className="
            relative
            scroll-mt-24
            overflow-hidden
            border-y
            border-slate-200
            bg-white
          "
        >
          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              right-0
              top-0
              h-96
              w-96
              bg-[radial-gradient(circle,_rgba(198, 161, 91,0.07),_transparent_70%)]
            "
          />

          <div className="relative z-10">
            <HowItWorks />
          </div>
        </section>

        {/* =========================================================
            PRODUCT DEMONSTRATION
            ========================================================= */}

        <section
          id="demo"
          aria-label="AI document analysis demonstration"
          className="
            relative
            scroll-mt-24
            overflow-hidden
            bg-[#071426]
          "
        >
          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              inset-0
              bg-[radial-gradient(circle_at_bottom_left,_rgba(60, 76, 97,0.5),_transparent_42%)]
            "
          />

          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              right-0
              top-0
              h-96
              w-96
              bg-[radial-gradient(circle,_rgba(198, 161, 91,0.08),_transparent_70%)]
            "
          />

          <div className="relative z-10">
            <DemoSection />
          </div>
        </section>

        {/* =========================================================
            USE CASES
            ========================================================= */}

        <section
          id="use-cases"
          aria-label="MukondoGTech AI use cases"
          className="
            relative
            scroll-mt-24
            border-y
            border-white/10
          "
        >
          <UseCasesSection />
        </section>

        {/* =========================================================
            SECURITY & PRIVACY
            ========================================================= */}

        <section
          id="security"
          aria-label="Security and privacy"
          className="
            relative
            scroll-mt-24
            overflow-hidden
            bg-white
          "
        >
          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              left-0
              top-0
              h-96
              w-96
              bg-[radial-gradient(circle,_rgba(60, 76, 97,0.05),_transparent_70%)]
            "
          />

          <div className="relative z-10">
            <SecuritySection />
          </div>
        </section>

        {/* =========================================================
            PRICING
            ========================================================= */}

        <section
          id="pricing"
          aria-label="MukondoGTech AI pricing plans"
          className="
            relative
            scroll-mt-24
            overflow-hidden
            border-y
            border-white/10
          "
        >
          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              right-0
              top-0
              h-96
              w-96
              bg-[radial-gradient(circle,_rgba(198, 161, 91,0.06),_transparent_70%)]
            "
          />

          <div className="relative z-10">
            <PricingPreview />
          </div>
        </section>

        {/* =========================================================
            FINAL CALL TO ACTION
            ========================================================= */}

        <section
          aria-label="Get started with MukondoGTech AI"
          className="
            relative
            overflow-hidden
            bg-[#071426]
          "
        >
          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              inset-0
              bg-[radial-gradient(circle_at_center,_rgba(198, 161, 91,0.12),_transparent_48%)]
            "
          />

          <div
            aria-hidden="true"
            className="
              pointer-events-none
              absolute
              bottom-0
              left-0
              h-80
              w-80
              bg-[radial-gradient(circle,_rgba(60, 76, 97,0.5),_transparent_70%)]
            "
          />

          <div className="relative z-10">
            <CTASection />
          </div>
        </section>

      </main>
    </div>
  );
}