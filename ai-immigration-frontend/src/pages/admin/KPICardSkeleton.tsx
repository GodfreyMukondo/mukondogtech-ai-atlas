export function KPICardSkeleton() {
  return (
    <div
      className="
        animate-pulse
        rounded-3xl
        border
        border-slate-800
        bg-[#111827]
        p-6
      "
    >
      <div className="h-4 w-32 rounded bg-slate-700" />

      <div className="mt-5 h-10 w-40 rounded bg-slate-700" />

      <div className="mt-4 h-4 w-24 rounded bg-slate-700" />
    </div>
  );
}