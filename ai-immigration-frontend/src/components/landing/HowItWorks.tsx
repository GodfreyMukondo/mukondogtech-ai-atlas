import { UserRound } from "lucide-react";
import { Link } from "react-router-dom";

export default function HowItWorks() {
  const steps = [
    {
      number: "01",
      title: "Upload Document",
      description:
        "Securely upload your immigration documents in PDF or image format.",
      accent: {
        badge: "from-[#4F7DF3] to-[#7EA5FF]",
        glow: "bg-[#4F7DF3]/15",
        border: "from-[#4F7DF3] to-[#7EA5FF]"
      }
    },
    {
      number: "02",
      title: "AI Processing",
      description:
        "Our AI analyzes content, detects issues, and evaluates document quality.",
      accent: {
        badge: "from-[#D4B984] to-[#D4B984]",
        glow: "bg-[#C6A15B]/15",
        border: "from-[#D4B984] to-[#D4B984]"
      }
    },
    {
      number: "03",
      title: "Receive Insights",
      description:
        "Receive detailed insights, recommendations, and next-step guidance instantly.",
      accent: {
        badge: "from-[#10B981] to-[#5EEAD4]",
        glow: "bg-[#10B981]/15",
        border: "from-[#10B981] to-[#5EEAD4]"
      }
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
      {/* Top Glow */}
      <div
        className="
          absolute
          top-0
          right-0
          h-[500px]
          w-[500px]
          rounded-full
          bg-[#5DA9FF]/10
          blur-3xl
        "
      />

      {/* Bottom Glow */}
      <div
        className="
          absolute
          bottom-0
          left-0
          h-[500px]
          w-[500px]
          rounded-full
          bg-[#D4B984]/10
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

      <div className="relative z-10 max-w-7xl mx-auto px-6">
        <h2
          className="
            text-center
            text-5xl
            font-bold
            text-white
          "
        >
          How It Works
        </h2>

        <p
          className="
            mt-5
            max-w-3xl
            mx-auto
            text-center
            text-lg
            text-[#9DBBFF]
            leading-relaxed
          "
        >
          A seamless AI-powered workflow that transforms document verification
          into a fast, intelligent, and stress-free experience.
        </p>

        <div className="relative mt-20">
          {/* Desktop Connection Line */}
          <div
            className="
              hidden
              lg:block
              absolute
              top-16
              left-[18%]
              right-[18%]
              h-[2px]
              bg-gradient-to-r
              from-[#4F7DF3]
              via-[#C6A15B]
              to-[#10B981]
            "
          />

          <div className="grid md:grid-cols-3 gap-8">
            {steps.map((step, i) => (
              <div
                key={i}
                className="
                  group
                  relative
                  overflow-hidden
                  rounded-3xl
                  border
                  border-white/10
                  bg-white/5
                  backdrop-blur-xl
                  p-10
                  shadow-2xl
                  shadow-black/20
                  transition-all
                  duration-500
                  hover:-translate-y-3
                "
              >
                {/* Left Accent Border */}
                <div
                  className={`
                    absolute
                    left-0
                    top-0
                    h-full
                    w-1.5
                    bg-gradient-to-b
                    ${step.accent.border}
                  `}
                />

                {/* Hover Glow */}
                <div
                  className={`
                    absolute
                    inset-0
                    opacity-0
                    transition-all
                    duration-500
                    group-hover:opacity-100
                    ${step.accent.glow}
                  `}
                />

                {/* Floating Decoration */}
                <div
                  className={`
                    absolute
                    -top-16
                    -right-16
                    h-40
                    w-40
                    rounded-full
                    opacity-20
                    blur-3xl
                    ${step.accent.glow}
                  `}
                />

                <div className="relative z-10">
                  {/* Step Badge */}
                  <div
                    className={`
                      inline-flex
                      items-center
                      rounded-full
                      px-4
                      py-2
                      text-xs
                      font-semibold
                      text-white
                      bg-gradient-to-r
                      ${step.accent.badge}
                      shadow-lg
                    `}
                  >
                    Step {i + 1}
                  </div>

                  {/* Number */}
                  <div
                    className={`
                      mt-6
                      flex
                      h-20
                      w-20
                      items-center
                      justify-center
                      rounded-3xl
                      bg-gradient-to-br
                      ${step.accent.badge}
                      text-white
                      text-3xl
                      font-extrabold
                      shadow-xl
                    `}
                  >
                    {step.number}
                  </div>

                  <h3
                    className="
                      mt-8
                      text-2xl
                      font-bold
                      text-[#7EA5FF]
                    "
                  >
                    {step.title}
                  </h3>

                  <p
                    className="
                      mt-4
                      text-[#9DBBFF]
                      leading-relaxed
                    "
                  >
                    {step.description}
                  </p>
                </div>
              </div>
            ))}
          </div>

          <div
            className="
              mt-14
              flex
              flex-col
              items-center
              justify-center
              gap-4
              rounded-2xl
              border
              border-white/10
              bg-white/5
              px-6
              py-5
              text-center
              backdrop-blur-sm
              sm:flex-row
              sm:text-left
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
                    from-[#4F7DF3]
                    to-[#1A4EA1]
                    text-white
                    shadow
                  "
                >
                  <UserRound size={18} />
                </span>
              ))}
            </div>

            <p className="text-sm text-[#9DBBFF]">
              Not sure about a result? A real document specialist can walk
              through it with you —{" "}
              <Link
                to="/contact"
                className="font-semibold text-[#C6A15B] underline-offset-4 hover:underline"
              >
                talk to our team
              </Link>
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}