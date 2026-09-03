import { ReactNode } from "react";
import { Outlet } from "react-router-dom";

import Navbar from "../../components/landing/Navbar";
import Footer from "../../components/landing/Footer";
import AIChatWidget from "../../components/chatbot/AIChatWidget";

interface LandingLayoutProps {
  children?: ReactNode;
}

export default function LandingLayout({
  children,
}: LandingLayoutProps) {
  return (
    <div
      className="
        min-h-screen
        flex
        flex-col
        bg-white
      "
    >
      <Navbar />

      <main
        className="
          flex-1
          w-full
        "
      >
        {children ?? <Outlet />}
      </main>

      <Footer />

      <AIChatWidget />
    </div>
  );
}