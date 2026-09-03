import { AlertTriangle } from "lucide-react";

export function DashboardError() {
  return (
    <div
      className="
        rounded-3xl
        border
        border-red-500/20
        bg-red-500/5
        p-8
      "
    >
      <div className="flex items-center gap-3">
        <AlertTriangle
          className="text-red-400"
          size={24}
        />

        <h2 className="font-bold text-red-400">
          Failed to load dashboard data
        </h2>
      </div>

      <p className="mt-3 text-slate-400">
        Please try again later.
      </p>
    </div>
  );
}