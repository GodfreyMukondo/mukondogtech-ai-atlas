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
      bg: "bg-[#EEF4FF]",
      icon: "text-[#4F7DF3]",
      border: "from-[#4F7DF3] to-[#7EA5FF]"
    }
  },
  {
    icon: UploadCloud,
    title: "Smart Upload",
    desc: "Upload PDFs and images securely.",
    color: {
      bg: "bg-[#F0FFF7]",
      icon: "text-[#10B981]",
      border: "from-[#10B981] to-[#5EEAD4]"
    }
  },
  {
    icon: SearchCheck,
    title: "Risk Detection",
    desc: "Identify missing information and errors.",
    color: {
      bg: "bg-[#FFF4F0]",
      icon: "text-[#F97316]",
      border: "from-[#F97316] to-[#FDBA74]"
    }
  },
  {
    icon: MessageCircle,
    title: "AI Chat",
    desc: "Ask questions about your documents.",
    color: {
      bg: "bg-[#F5F1FF]",
      icon: "text-[#8B5CF6]",
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
        bg-gradient-to-b
        from-[#FAFBFF]
        via-[#F7F8FC]
        to-[#F2F5FB]
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
          bg-[#FFD978]/15
          blur-3xl
        "
      />

      <div className="relative z-10 max-w-7xl mx-auto px-6">
        <h2
          className="
            text-center
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
          Powerful AI Features
        </h2>

        <p
          className="
            text-center
            text-[#66758F]
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
                bg-white/80
                backdrop-blur-xl
                rounded-3xl
                p-8
                border
                border-[#E7ECF5]
                shadow-[0_8px_25px_rgba(15,23,42,0.05)]
                hover:shadow-[0_25px_60px_rgba(15,23,42,0.12)]
                hover:-translate-y-2
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
                    text-[#071330]
                  "
                >
                  {f.title}
                </h3>

                <p
                  className="
                    mt-3
                    text-[#66758F]
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