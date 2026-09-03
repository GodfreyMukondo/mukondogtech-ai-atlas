import { Link, useLocation } from "react-router-dom";
import { ChevronRight, Menu, UploadCloud } from "lucide-react";

interface DashboardHeaderProps {
  onMenuClick: () => void;
}

interface PageMeta {
  title: string;
  subtitle: string;
}

const PAGE_META: Record<string, PageMeta> = {
  "/dashboard": {
    title: "Dashboard",
    subtitle: "Your document activity and analysis at a glance.",
  },
  "/dashboard/chat": {
    title: "AI Immigration Assistant",
    subtitle: "Ask questions about your documents and process.",
  },
  "/dashboard/documents": {
    title: "My Documents",
    subtitle: "Upload, review and manage your immigration documents.",
  },
  "/dashboard/applications": {
    title: "Applications",
    subtitle: "Track the status of your immigration applications.",
  },
  "/dashboard/reports": {
    title: "Reports & Analytics",
    subtitle: "Review analysis history and document insights.",
  },
  "/dashboard/billing/subscription": {
    title: "Subscription",
    subtitle: "Manage your plan and billing details.",
  },
  "/dashboard/billing/history": {
    title: "Billing History",
    subtitle: "View previous invoices and payments.",
  },
  "/dashboard/profile": {
    title: "Profile Settings",
    subtitle: "Manage your personal information.",
  },
  "/dashboard/account": {
    title: "Account",
    subtitle: "Manage your account preferences.",
  },
  "/dashboard/settings/profile": {
    title: "Settings",
    subtitle: "Manage your application settings.",
  },
  "/dashboard/settings/security": {
    title: "Security Settings",
    subtitle: "Manage password and account security.",
  },
  "/dashboard/settings/notifications": {
    title: "Notification Settings",
    subtitle: "Choose how we keep you updated.",
  },
};

const DEFAULT_PAGE_META: PageMeta = {
  title: "Workspace",
  subtitle: "Your immigration document workspace.",
};

function resolvePageMeta(pathname: string): PageMeta {
  return PAGE_META[pathname] ?? DEFAULT_PAGE_META;
}

export default function DashboardHeader({ onMenuClick }: DashboardHeaderProps) {
  const location = useLocation();
  const pageMeta = resolvePageMeta(location.pathname);

  return (
    <header
      className="
        sticky top-0 z-30
        border-b border-slate-200/80
        bg-white/95 backdrop-blur-xl
        shadow-[0_4px_20px_rgba(11,23,54,0.04)]
      "
    >
      <div
        className="
          flex min-h-20 items-center justify-between gap-4
          px-4 py-3 sm:px-6 lg:px-8
        "
      >
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <button
            type="button"
            onClick={onMenuClick}
            aria-label="Open navigation"
            className="
              flex h-11 w-11 shrink-0 items-center justify-center
              rounded-xl border border-slate-200 bg-white
              text-[#0B1736] shadow-sm transition
              hover:border-slate-300 hover:bg-slate-50
              focus:outline-none focus:ring-2 focus:ring-[#F4B81A] focus:ring-offset-2
              lg:hidden
            "
          >
            <Menu size={21} />
          </button>

          <div className="min-w-0 flex-1">
            <div className="hidden items-center gap-1.5 text-xs font-semibold text-slate-400 sm:flex">
              <span>Workspace</span>
              <ChevronRight size={13} />
              <span className="truncate text-slate-500">{pageMeta.title}</span>
            </div>

            <h1 className="truncate text-lg font-black tracking-tight text-[#0B1736] sm:text-xl">
              {pageMeta.title}
            </h1>

            <p className="hidden max-w-2xl truncate text-sm text-slate-500 md:block">
              {pageMeta.subtitle}
            </p>
          </div>
        </div>

        <Link
          to="/dashboard/documents"
          className="
            inline-flex h-11 shrink-0 items-center justify-center gap-2
            rounded-xl bg-[#F4B81A] px-4 text-sm font-bold text-[#071330]
            shadow-sm transition-all duration-200
            hover:-translate-y-0.5 hover:bg-[#E8AB0D] hover:shadow-md
          "
        >
          <UploadCloud size={18} />
          <span className="hidden sm:inline">Upload document</span>
        </Link>
      </div>
    </header>
  );
}
