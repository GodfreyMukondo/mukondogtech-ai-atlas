import React, { memo, ReactNode } from "react";

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
        border-slate-800
        bg-[#111827]
        p-6
        shadow-xl
        transition-all
        duration-300
        hover:border-slate-700
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

