import {
  ReactNode,
  useEffect,
  useState,
} from "react";

import { Outlet } from "react-router-dom";

import Sidebar from "../../components/Sidebar";
import DashboardHeader from "./DashboardHeader";
import DashboardFooter from "../../components/footer/DashboardFooter";

interface DashboardLayoutProps {
  children?: ReactNode;
}

export default function DashboardLayout({
  children,
}: DashboardLayoutProps) {
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

  return (
    <div
      className="
        min-h-screen
        bg-gradient-to-br
        from-[#F8F6F1]
        via-white
        to-[#EEF3FA]
      "
    >
      {/* Sidebar */}

      <Sidebar
        mobileOpen={mobileSidebar}
        onClose={closeSidebar}
      />

      {/* Page Structure */}

      <div
        className="
          flex
          min-h-screen
          flex-col
          lg:ml-[300px]
          xl:ml-[320px]
        "
      >
        {/* Header */}

        <DashboardHeader
          onMenuClick={openSidebar}
        />

        {/* Content */}

        <main className="flex-1">
          {children ?? <Outlet />}
        </main>

        {/* Footer */}

        <DashboardFooter />
      </div>
    </div>
  );
}