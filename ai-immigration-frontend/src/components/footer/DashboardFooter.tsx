import {
  ShieldCheck,
  Sparkles,
} from "lucide-react";

export default function DashboardFooter() {
  return (
    <footer
      className="
        border-t
        border-[#E7EAF0]
        bg-white/90
        backdrop-blur-sm
      "
    >
      <div
        className="
          mx-auto
          flex
          max-w-7xl
          flex-col
          items-center
          justify-between
          gap-4
          px-6
          py-5
          text-sm
          text-[#667085]
          md:flex-row
        "
      >
        <div>
          © {new Date().getFullYear()}{" "}
          <span className="font-semibold text-[#0B1736]">
            MukondoGTech AI
          </span>
          . All rights reserved.
        </div>

        <div
          className="
            flex
            flex-wrap
            items-center
            gap-5
          "
        >
          <div className="flex items-center gap-2">
            <Sparkles
              size={14}
              className="text-[#F4B81A]"
            />
            AI Immigration Intelligence
          </div>

          <div className="flex items-center gap-2">
            <ShieldCheck
              size={14}
              className="text-[#F4B81A]"
            />
            Secure Processing
          </div>
        </div>
      </div>
    </footer>
  );
}