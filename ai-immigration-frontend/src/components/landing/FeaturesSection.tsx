import {
  Brain,
  UploadCloud,
  SearchCheck,
  MessageCircle
} from "lucide-react";

const features = [
  {
    icon: Brain,
    title: "AI Analysis",
    desc: "Advanced AI models understand complex documents.",
    color: {
      bg: "bg-[#4F7DF3]/20 border border-[#7EA5FF]/30",
      icon: "text-[#9DBBFF]",
      border: "from-[#4F7DF3] to-[#7EA5FF]"
    }
  },
  {
    icon: UploadCloud,
    title: "Smart Upload",
    desc: "Upload PDFs and images securely.",
    color: {
      bg: "bg-[#10B981]/20 border border-[#5EEAD4]/30",
      icon: "text-[#7EF0DE]",
      border: "from-[#10B981] to-[#5EEAD4]"
    }
  },
  {
    icon: SearchCheck,
    title: "Risk Detection",
    desc: "Identify missing information and errors.",
    color: {
      bg: "bg-[#F97316]/20 border border-[#FDBA74]/30",
      icon: "text-[#FDC48D]",
      border: "from-[#F97316] to-[#FDBA74]"
    }
  },
  {
    icon: MessageCircle,
    title: "AI Chat",
    desc: "Ask questions about your documents.",
    color: {
      bg: "bg-[#8B5CF6]/20 border border-[#C4B5FD]/30",
      icon: "text-[#D2C5FE]",
      border: "from-[#8B5CF6] to-[#C4B5FD]"
    }
  }
];

export default function FeaturesSection() {
  return (
    <section
      className="
        relative
        overflow-hidden
        py-24
      "
    >
      {/* Background Glow */}
      <div
        className="
          absolute
          top-0
          right-0
          h-96
          w-96
          rounded-full
          bg-[#5DA9FF]/10
          blur-3xl
        "
      />

      <div
        className="
          absolute
          bottom-0
          left-0
          h-96
          w-96
          rounded-full
          bg-[#D4B984]/15
          blur-3xl
        "
      />

      <div className="relative z-10 max-w-7xl mx-auto px-6">
        <h2
          className="
            text-center
            text-5xl
            font-bold
            text-[#7EA5FF]
          "
        >
          Powerful AI Features
        </h2>

        <p
          className="
            text-center
            text-[#9DBBFF]
            mt-4
            text-lg
          "
        >
          Everything required for document verification.
        </p>

        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8 mt-16">
          {features.map((f, i) => (
            <div
              key={i}
              className="
                group
                relative
                overflow-hidden
                bg-[#16283F]
                rounded-3xl
                p-8
                border
                border-white/10
                shadow-2xl
                shadow-black/30
                hover:-translate-y-2
                hover:border-white/20
                hover:bg-[#1C3350]
                transition-all
                duration-300
              "
            >
              {/* Premium Left Border */}
              <div
                className={`
                  absolute
                  left-0
                  top-0
                  h-full
                  w-1.5
                  rounded-l-3xl
                  bg-gradient-to-b
                  ${f.color.border}
                `}
              />

              {/* Hover Glow */}
              <div
                className={`
                  absolute
                  left-0
                  top-0
                  h-full
                  w-24
                  opacity-0
                  blur-3xl
                  transition-opacity
                  duration-300
                  group-hover:opacity-100
                  bg-gradient-to-b
                  ${f.color.border}
                `}
              />

              <div className="relative z-10">
                <div
                  className={`
                    w-14
                    h-14
                    rounded-2xl
                    flex
                    items-center
                    justify-center
                    ${f.color.bg}
                  `}
                >
                  <f.icon
                    className={`
                      w-6
                      h-6
                      ${f.color.icon}
                    `}
                  />
                </div>

                <h3
                  className="
                    mt-6
                    text-xl
                    font-bold
                    text-white
                  "
                >
                  {f.title}
                </h3>

                <p
                  className="
                    mt-3
                    text-slate-200
                    leading-relaxed
                  "
                >
                  {f.desc}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}