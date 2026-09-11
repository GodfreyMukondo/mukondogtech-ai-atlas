import { ShieldCheck, Lock, FileCheck, Fingerprint, UserRound } from "lucide-react";
import { Link } from "react-router-dom";

export default function SecuritySection() {
  const securityFeatures = [
    {
      icon: Lock,
      title: "Encrypted Storage"
    },
    {
      icon: Fingerprint,
      title: "Secure Authentication"
    },
    {
      icon: FileCheck,
      title: "Audit & Compliance"
    }
  ];

  return (
    <section
      className="
        relative
        overflow-hidden
        py-28
      "
    >
      {/* Top Right Glow */}
      <div
        className="
          absolute
          top-0
          right-0
          h-[450px]
          w-[450px]
          rounded-full
          bg-[#5DA9FF]/10
          blur-3xl
        "
      />

      {/* Bottom Left Glow */}
      <div
        className="
          absolute
          bottom-0
          left-0
          h-[450px]
          w-[450px]
          rounded-full
          bg-[#D4B984]/15
          blur-3xl
        "
      />

      {/* Decorative Grid */}
      <div
        className="
          absolute
          inset-0
          opacity-[0.03]
          [background-image:linear-gradient(#fff_1px,transparent_1px),linear-gradient(to_right,#fff_1px,transparent_1px)]
          [background-size:48px_48px]
        "
      />

      <div className="relative z-10 max-w-6xl mx-auto px-6">
        <div
          className="
            relative
            overflow-hidden
            rounded-[32px]
            border
            border-white/10
            bg-[#16283F]
            p-12
            md:p-16
            text-center
            shadow-2xl
            shadow-black/30
          "
        >
          {/* Card Glow */}
          <div
            className="
              absolute
              top-0
              right-0
              h-72
              w-72
              rounded-full
              bg-[#D4B984]/10
              blur-3xl
            "
          />

          <div
            className="
              absolute
              bottom-0
              left-0
              h-72
              w-72
              rounded-full
              bg-[#5DA9FF]/10
              blur-3xl
            "
          />

          <div className="relative z-10">
            {/* Security Badge */}
            <div
              className="
                inline-flex
                items-center
                gap-2
                rounded-full
                border
                border-[#C6A15B]/30
                bg-[#C6A15B]/10
                px-5
                py-2
                text-sm
                font-semibold
                text-[#C6A15B]
              "
            >
              <ShieldCheck size={16} />
              Security First
            </div>

            {/* Main Icon */}
            <div
              className="
                mt-8
                inline-flex
                items-center
                justify-center
                h-24
                w-24
                rounded-3xl
                bg-gradient-to-br
                from-[#D4B984]
                via-[#D4B984]
                to-[#C6A15B]
                shadow-[0_15px_40px_rgba(198,161,91,0.30)]
              "
            >
              <ShieldCheck
                size={42}
                className="text-[#071426]"
              />
            </div>

            {/* Heading */}
            <h2
              className="
                mt-10
                text-5xl
                font-bold
                text-white
              "
            >
              Enterprise Grade Security
            </h2>

            {/* Description */}
            <p
              className="
                mt-6
                max-w-3xl
                mx-auto
                text-xl
                leading-relaxed
                text-slate-200
              "
            >
              Your documents are protected by modern security
              practices including encrypted storage, secure
              authentication, audit logging, compliance controls,
              and protected AI processing from upload through
              analysis.
            </p>

            {/* Security Features */}
            <div
              className="
                mt-12
                grid
                gap-6
                md:grid-cols-3
              "
            >
              {securityFeatures.map((feature, index) => (
                <div
                  key={index}
                  className="
                    group
                    rounded-2xl
                    border
                    border-white/10
                    bg-[#1F314A]
                    p-6
                    transition-all
                    duration-300
                    hover:-translate-y-1
                    hover:border-white/20
                    hover:bg-[#28395A]
                  "
                >
                  <div
                    className="
                      mx-auto
                      flex
                      h-14
                      w-14
                      items-center
                      justify-center
                      rounded-2xl
                      bg-[#4F7DF3]/20
                      border
                      border-[#7EA5FF]/30
                    "
                  >
                    <feature.icon
                      className="
                        h-6
                        w-6
                        text-[#9DBBFF]
                      "
                    />
                  </div>

                  <h3
                    className="
                      mt-4
                      font-semibold
                      text-white
                    "
                  >
                    {feature.title}
                  </h3>
                </div>
              ))}
            </div>

            {/* Trust Statement */}
            <div
              className="
                mt-12
                inline-flex
                items-center
                rounded-full
                border
                border-white/10
                bg-[#1F314A]
                px-6
                py-3
                text-sm
                font-medium
                text-slate-200
              "
            >
              Trusted security practices for sensitive immigration
              and document verification workflows.
            </div>

            {/* Human Trust Element */}
            <div
              className="
                mt-8
                flex
                flex-col
                items-center
                justify-center
                gap-4
                rounded-2xl
                border
                border-white/10
                bg-[#1F314A]
                px-6
                py-5
                sm:flex-row
              "
            >
              <div className="flex -space-x-3">
                {[0, 1].map((i) => (
                  <span
                    key={i}
                    className="
                      flex
                      h-10
                      w-10
                      items-center
                      justify-center
                      rounded-full
                      border-2
                      border-[#1F314A]
                      bg-gradient-to-br
                      from-[#D4B984]
                      to-[#C6A15B]
                      text-[#071426]
                      shadow
                    "
                  >
                    <UserRound size={18} />
                  </span>
                ))}
              </div>

              <p className="text-sm text-slate-200">
                Our security and compliance team reviews how documents are
                handled —{" "}
                <Link
                  to="/contact"
                  className="font-semibold text-[#C6A15B] underline-offset-4 hover:underline"
                >
                  talk to someone
                </Link>{" "}
                about how your data is protected.
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}