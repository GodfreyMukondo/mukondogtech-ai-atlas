import {
  useEffect,
  useState,
} from "react";

import type {
  CSSProperties,
  ReactNode,
} from "react";

import { Outlet } from "react-router-dom";

import Sidebar from "../../components/Sidebar";
import DashboardHeader from "./DashboardHeader";
import DashboardFooter from "../../components/footer/DashboardFooter";

interface DashboardLayoutProps {
  children?: ReactNode;
}

/**
 * Sidebar width when expanded/collapsed.
 *
 * Kept in one place and mirrored below so the content column's offset
 * always matches the sidebar's actual rendered width - previously this
 * layout used a fixed lg:ml-[300px]/xl:ml-[320px] offset that never
 * matched Sidebar.tsx's real widths (w-[290px] / w-[88px]) and never
 * reacted to the sidebar being collapsed, leaving a large dead gap
 * between the collapsed sidebar and the page content.
 */
const SIDEBAR_WIDTH_EXPANDED = "290px";
const SIDEBAR_WIDTH_COLLAPSED = "88px";

const SIDEBAR_COLLAPSED_STORAGE_KEY =
  "mgt-sidebar-collapsed";

export default function DashboardLayout({
  children,
}: DashboardLayoutProps) {
  /* ==========================================================================
     MOBILE SIDEBAR
     ========================================================================== */

  const [
    mobileSidebar,
    setMobileSidebar,
  ] = useState(false);

  const openSidebar = () => {
    setMobileSidebar(true);
  };

  const closeSidebar = () => {
    setMobileSidebar(false);
  };

  useEffect(() => {
    document.body.style.overflow =
      mobileSidebar ? "hidden" : "";

    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileSidebar]);

  /* ==========================================================================
     DESKTOP SIDEBAR COLLAPSE

     Owned here (rather than inside Sidebar) so the content column can size
     its own offset to match, instead of guessing with a fixed margin.
     ========================================================================== */

  const [
    sidebarCollapsed,
    setSidebarCollapsed,
  ] = useState(false);

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(
        SIDEBAR_COLLAPSED_STORAGE_KEY,
      );

      if (stored === "true") {
        setSidebarCollapsed(true);
      }
    } catch {
      // Ignore storage failures - default to expanded.
    }
  }, []);

  const contentOffset = sidebarCollapsed
    ? SIDEBAR_WIDTH_COLLAPSED
    : SIDEBAR_WIDTH_EXPANDED;

  /* ==========================================================================
     LAYOUT
     ========================================================================== */

  return (
    <div className="min-h-screen">
      {/* ====================================================================
          SKIP LINK

          Lets keyboard/screen-reader users bypass the sidebar and header
          navigation and jump straight to the page content.
          ==================================================================== */}

      <a
        href="#dashboard-content"
        className="
          sr-only
          focus:not-sr-only
          focus:fixed
          focus:left-4
          focus:top-4
          focus:z-[60]
          focus:rounded-xl
          focus:bg-[#0B1F3A]
          focus:px-4
          focus:py-2.5
          focus:text-sm
          focus:font-bold
          focus:text-white
          focus:shadow-lg
        "
      >
        Skip to content
      </a>

      {/* ====================================================================
          SIDEBAR
          ==================================================================== */}

      <Sidebar
        mobileOpen={mobileSidebar}
        onClose={closeSidebar}
        collapsed={sidebarCollapsed}
        onCollapsedChange={setSidebarCollapsed}
      />

      {/* ====================================================================
          CONTENT COLUMN

          The left offset tracks the sidebar's real, current width (see
          contentOffset above) and transitions in step with the sidebar's
          own width animation.
          ==================================================================== */}

      <div
        className="
          flex
          min-h-screen
          flex-col
          pt-20
          transition-[margin]
          duration-300
          lg:ml-[var(--dashboard-content-offset)]
        "
        style={
          {
            "--dashboard-content-offset":
              contentOffset,
          } as CSSProperties
        }
      >
        {/* Header */}

        <DashboardHeader
          onMenuClick={openSidebar}
        />

        {/* Page content */}

        <main
          id="dashboard-content"
          className="flex-1"
        >
          {children ?? <Outlet />}
        </main>

        {/* Footer */}

        <DashboardFooter />
      </div>
    </div>
  );
}
