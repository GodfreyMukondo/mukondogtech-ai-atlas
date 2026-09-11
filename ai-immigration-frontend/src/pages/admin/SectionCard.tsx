import { memo, type ReactNode } from "react";

export interface SectionCardProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
  className?: string;
}

function SectionCardComponent({
  title,
  subtitle,
  children,
  className = "",
}: SectionCardProps) {
  return (
    <section
      className={`
        rounded-3xl
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        p-6
        shadow-xl
        shadow-black/20
        transition-all
        duration-300
        hover:border-white/20
        ${className}
      `}
    >
      <div className="mb-6">
        <h2
          className="
            text-xl
            font-bold
            text-white
          "
        >
          {title}
        </h2>

        {subtitle && (
          <p
            className="
              mt-2
              text-sm
              text-slate-400
            "
          >
            {subtitle}
          </p>
        )}
      </div>

      <div>{children}</div>
    </section>
  );
}

export const SectionCard = memo(SectionCardComponent);

SectionCard.displayName = "SectionCard";

export default SectionCard;

