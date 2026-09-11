import { memo } from "react";

export type StatusType =
  | "success"
  | "warning"
  | "error"
  | "info";

export interface StatusBadgeProps {
  label: string;
  status?: StatusType;
  className?: string;
}

function StatusBadgeComponent({
  label,
  status = "info",
  className = "",
}: StatusBadgeProps) {
  const styles = {
    success: {
      container:
        "border-emerald-500/20 bg-emerald-500/10 text-emerald-400",
      dot: "bg-emerald-400",
    },

    warning: {
      container:
        "border-amber-500/20 bg-amber-500/10 text-amber-400",
      dot: "bg-amber-400",
    },

    error: {
      container:
        "border-red-500/20 bg-red-500/10 text-red-400",
      dot: "bg-red-400",
    },

    info: {
      container:
        "border-sky-500/20 bg-sky-500/10 text-sky-400",
      dot: "bg-sky-400",
    },
  };

  const current = styles[status];

  return (
    <span
      className={`
        inline-flex
        items-center
        gap-2
        rounded-full
        border
        px-4
        py-2
        text-sm
        font-semibold
        ${current.container}
        ${className}
      `}
    >
      <span
        className={`
          h-2.5
          w-2.5
          rounded-full
          ${current.dot}
        `}
      />

      {label}
    </span>
  );
}

export const StatusBadge = memo(StatusBadgeComponent);

StatusBadge.displayName = "StatusBadge";

export default StatusBadge;

