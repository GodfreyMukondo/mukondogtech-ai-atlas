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
        bg-gradient-to-b
        from-[#F8FAFF]
        via-[#F5F8FF]
        to-[#EEF4FF]
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
          bg-[#FFD978]/15
          blur-3xl
        "
      />

      {/* Decorative Grid */}
      <div
        className="
          absolute
          inset-0
          opacity-[0.03]
          [background-image:linear-gradient(#0B1736_1px,transparent_1px),linear-gradient(to_right,#0B1736_1px,transparent_1px)]
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
            border-[#E7ECF5]
            bg-white/80
            backdrop-blur-xl
            p-12
            md:p-16
            text-center
            shadow-[0_20px_60px_rgba(15,23,42,0.08)]
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
              bg-[#FFD978]/10
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
                border-[#F4D27A]/40
                bg-[#FFF8E6]
                px-5
                py-2
                text-sm
                font-semibold
                text-[#B8860B]
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
                from-[#F7C948]
                via-[#FFD978]
                to-[#F4B81A]
                shadow-[0_15px_40px_rgba(244,184,26,0.30)]
              "
            >
              <ShieldCheck
                size={42}
                className="text-[#071330]"
              />
            </div>

            {/* Heading */}
            <h2
              className="
                mt-10
                text-5xl
                font-bold
                bg-gradient-to-r
                from-[#071330]
                via-[#12306D]
                to-[#1A4EA1]
                bg-clip-text
                text-transparent
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
                text-[#66758F]
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
                    border-[#E7ECF5]
                    bg-white/70
                    p-6
                    backdrop-blur-sm
                    transition-all
                    duration-300
                    hover:-translate-y-1
                    hover:shadow-lg
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
                      bg-gradient-to-br
                      from-[#EEF4FF]
                      to-[#F5F8FF]
                    "
                  >
                    <feature.icon
                      className="
                        h-6
                        w-6
                        text-[#1A4EA1]
                      "
                    />
                  </div>

                  <h3
                    className="
                      mt-4
                      font-semibold
                      text-[#071330]
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
                border-[#E7ECF5]
                bg-white/70
                px-6
                py-3
                text-sm
                font-medium
                text-[#66758F]
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
                border-[#E7ECF5]
                bg-white/70
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
                      border-white
                      bg-gradient-to-br
                      from-[#F7C948]
                      to-[#F4B81A]
                      text-[#071330]
                      shadow
                    "
                  >
                    <UserRound size={18} />
                  </span>
                ))}
              </div>

              <p className="text-sm text-[#66758F]">
                Our security and compliance team reviews how documents are
                handled —{" "}
                <Link
                  to="/contact"
                  className="font-semibold text-[#1A4EA1] underline-offset-4 hover:underline"
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