import React, { memo } from "react";
import type { LucideIcon } from "lucide-react";
import {
  TrendingUp,
  TrendingDown,
  Minus,
} from "lucide-react";

export interface KPICardProps {
  title: string;
  value: string | number;
  change?: string;
  icon: LucideIcon;
  className?: string;
}

type KPIState =
  | "success"
  | "warning"
  | "error"
  | "info"
  | "neutral";

function getState(change?: string): KPIState {
  if (!change) return "neutral";

  const normalized = change.trim().toLowerCase();

  if (normalized.startsWith("+")) return "success";

  if (normalized.startsWith("-")) return "error";

  if (
    normalized === "warning" ||
    normalized === "degraded"
  ) {
    return "warning";
  }

  if (
    normalized === "error" ||
    normalized === "offline" ||
    normalized === "critical"
  ) {
    return "error";
  }

  if (normalized === "info") {
    return "info";
  }

  return "neutral";
}

function KPICardComponent({
  title,
  value,
  change,
  icon: Icon,
  className = "",
}: KPICardProps) {
  const state = getState(change);

  const trendColorMap: Record<KPIState, string> = {
    success: "text-emerald-400",
    warning: "text-amber-400",
    error: "text-red-400",
    info: "text-sky-400",
    neutral: "text-slate-400",
  };

  const iconColorMap: Record<KPIState, string> = {
    success: "bg-emerald-500/10 text-emerald-400",
    warning: "bg-amber-500/10 text-amber-400",
    error: "bg-red-500/10 text-red-400",
    info: "bg-sky-500/10 text-sky-400",
    neutral: "bg-slate-500/10 text-slate-400",
  };

  const trendColor = trendColorMap[state];
  const iconBackground = iconColorMap[state];

  const renderTrendIcon = () => {
    if (!change) return null;

    if (state === "success") {
      return <TrendingUp size={16} aria-hidden="true" />;
    }

    if (state === "error" && change.startsWith("-")) {
      return <TrendingDown size={16} aria-hidden="true" />;
    }

    return <Minus size={16} aria-hidden="true" />;
  };

  return (
    <article
      role="region"
      aria-label={title}
      className={`
        group
        relative
        overflow-hidden
        rounded-3xl
        border
        border-slate-800
        bg-[#111827]
        p-6
        shadow-lg
        transition-all
        duration-300
        hover:-translate-y-1
        hover:border-slate-700
        hover:shadow-2xl
        focus-within:ring-2
        focus-within:ring-sky-500
        ${className}
      `}
    >
      <div
        aria-hidden="true"
        className="
          absolute
          inset-0
          bg-gradient-to-br
          from-sky-500/5
          via-transparent
          to-transparent
          opacity-0
          transition-opacity
          duration-300
          group-hover:opacity-100
        "
      />

      <div className="relative z-10 flex items-start justify-between">
        <div className="min-w-0 flex-1">
          <p
            className="
              text-sm
              font-medium
              tracking-wide
              text-slate-400
            "
          >
            {title}
          </p>

          <h3
            className="
              mt-3
              truncate
              text-3xl
              font-black
              tracking-tight
              text-white
            "
          >
            {value}
          </h3>

          {change && (
            <div
              className={`
                mt-4
                inline-flex
                items-center
                gap-1.5
                text-sm
                font-semibold
                ${trendColor}
              `}
            >
              {renderTrendIcon()}
              <span>{change}</span>
            </div>
          )}
        </div>

        <div
          className={`
            flex
            h-14
            w-14
            shrink-0
            items-center
            justify-center
            rounded-2xl
            ${iconBackground}
          `}
          aria-hidden="true"
        >
          <Icon
            size={28}
            strokeWidth={2}
          />
        </div>
      </div>
    </article>
  );
}

KPICardComponent.displayName = "KPICard";

export const KPICard = memo(KPICardComponent);

KPICard.displayName = "MemoizedKPICard";

export default KPICard;