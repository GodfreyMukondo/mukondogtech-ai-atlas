import { ArrowRight, UserRound } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "../../features/auth/hooks/useAuth";

export default function CTASection() {
  const { user } = useAuth();

  const analysisRoute = user
    ? "/dashboard/upload"
    : "/register";

  return (
    <section
      className="
        py-24
        bg-white
      "
    >
      <div
        className="
          max-w-7xl
          mx-auto
          px-6
        "
      >
        <div
          className="
            rounded-3xl
            bg-gradient-to-br
            from-[#071330]
            via-[#0D1F4D]
            to-[#162B63]
            px-8
            py-16
            text-center
            shadow-[0_30px_80px_rgba(7,19,48,0.35)]
            overflow-hidden
            relative
            border
            border-white/10
          "
        >
          {/* Top Right Glow */}
          <div
            className="
              absolute
              top-0
              right-0
              h-64
              w-64
              rounded-full
              bg-[#FFD978]/30
              blur-3xl
            "
          />

          {/* Bottom Left Glow */}
          <div
            className="
              absolute
              bottom-0
              left-0
              h-72
              w-72
              rounded-full
              bg-[#5DA9FF]/20
              blur-3xl
            "
          />

          <div className="relative z-10">
            <h2
              className="
                text-4xl
                font-bold
                md:text-5xl
                bg-gradient-to-r
                from-white
                via-[#FFF9EC]
                to-[#FFE8A3]
                bg-clip-text
                text-transparent
              "
            >
              Ready to simplify document verification?
            </h2>

            <p
              className="
                mx-auto
                mt-5
                max-w-2xl
                text-lg
                leading-relaxed
                text-[#D7E2FF]
              "
            >
              Experience faster immigration document
              analysis with MukondoGTech AI.
            </p>

            <Link
              to={analysisRoute}
              className="
                mt-8
                inline-flex
                items-center
                gap-2
                rounded-2xl
                bg-gradient-to-r
                from-[#F7C948]
                via-[#FFD978]
                to-[#F4B81A]
                px-10
                py-5
                font-bold
                text-[#071330]
                shadow-[0_12px_35px_rgba(244,184,26,0.35)]
                transition-all
                duration-300
                hover:-translate-y-1
                hover:scale-[1.02]
                hover:shadow-[0_18px_50px_rgba(244,184,26,0.45)]
              "
            >
              Start Your Analysis
              <ArrowRight size={20} />
            </Link>

            <div
              className="
                mt-8
                flex
                flex-col
                items-center
                justify-center
                gap-3
                sm:flex-row
              "
            >
              <div className="flex -space-x-3">
                {[0, 1, 2].map((i) => (
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
                      border-[#0D1F4D]
                      bg-gradient-to-br
                      from-[#F7C948]
                      to-[#F4B81A]
                      text-[#071330]
                    "
                  >
                    <UserRound size={18} />
                  </span>
                ))}
              </div>

              <p className="text-sm text-[#D7E2FF]">
                Real specialists review flagged results —{" "}
                <Link
                  to="/contact"
                  className="font-semibold text-[#FFE8A3] underline-offset-4 hover:underline"
                >
                  talk to our team
                </Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}