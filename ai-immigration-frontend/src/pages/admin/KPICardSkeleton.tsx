export function KPICardSkeleton() {
  return (
    <div
      className="
        animate-pulse
        rounded-3xl
        border
        border-white/10
        bg-white/5
        backdrop-blur-xl
        p-6
      "
    >
      <div className="h-4 w-32 rounded bg-white/10" />

      <div className="mt-5 h-10 w-40 rounded bg-white/10" />

      <div className="mt-4 h-4 w-24 rounded bg-white/10" />
    </div>
  );
}