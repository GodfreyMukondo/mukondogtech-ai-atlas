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
            from-white
            via-[#FBF7EF]
            to-[#F6F1E6]
            px-8
            py-16
            text-center
            shadow-[0_30px_80px_rgba(7,20,38,0.25)]
            overflow-hidden
            relative
            border
            border-[#0B1F3A]/10
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
              bg-[#C6A15B]/15
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
              bg-[#4F7DF3]/10
              blur-3xl
            "
          />

          <div className="relative z-10">
            <h2
              className="
                text-4xl
                font-bold
                md:text-5xl
                text-[#0B1F3A]
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
                text-[#1F314A]
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
                from-[#D4B984]
                via-[#D4B984]
                to-[#C6A15B]
                px-10
                py-5
                font-bold
                text-[#071426]
                shadow-[0_12px_35px_rgba(198,161,91,0.35)]
                transition-all
                duration-300
                hover:-translate-y-1
                hover:scale-[1.02]
                hover:shadow-[0_18px_50px_rgba(198,161,91,0.45)]
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
                      border-white
                      bg-gradient-to-br
                      from-[#D4B984]
                      to-[#C6A15B]
                      text-[#071426]
                    "
                  >
                    <UserRound size={18} />
                  </span>
                ))}
              </div>

              <p className="text-sm text-[#1F314A]">
                Real specialists review flagged results —{" "}
                <Link
                  to="/contact"
                  className="font-semibold text-[#A8894D] underline-offset-4 hover:underline"
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