
// src/components/layout/Navbar.tsx

"use client";

import React, {
  useEffect,
  useRef,
  useState,
  type ElementType,
} from "react";

import {
  Link,
  useLocation,
  useNavigate,
} from "react-router-dom";

import {
  AnimatePresence,
  motion,
} from "framer-motion";

import {
  Activity,
  ArrowRight,
  BarChart3,
  Bell,
  BrainCircuit,
  ChevronDown,
  CircleHelp,
  ClipboardCheck,
  CreditCard,
  Database,
  FileCheck,
  FileText,
  Globe2,
  LayoutDashboard,
  LockKeyhole,
  LogOut,
  Menu,
  MessageCircle,
  Rocket,
  Scale,
  Settings,
  ShieldCheck,
  Sparkles,
  UploadCloud,
  UserCircle,
  Users,
  X,
} from "lucide-react";

import { useAuth } from "../../features/auth/hooks/useAuth";

/* ========================================================================
   TYPES
   ======================================================================== */

interface NavbarProps {
  onMenuClick?: () => void;
}

interface NavItem {
  label: string;
  path: string;
  icon: ElementType;
  description?: string;
}

interface DropdownMenu {
  label: string;
  icon: ElementType;
  items: NavItem[];
}

type UserRole = "USER" | "ADMIN";

/* ========================================================================
   PUBLIC NAVIGATION
   ======================================================================== */

const PRODUCT_MENU: DropdownMenu = {
  label: "Product",
  icon: Sparkles,
  items: [
    {
      label: "AI Demo",
      path: "/demo",
      icon: Rocket,
      description: "Experience AI document analysis",
    },
    {
      label: "AI Assistant",
      path: "/chat",
      icon: MessageCircle,
      description: "Ask immigration questions",
    },
    {
      label: "Security",
      path: "/security",
      icon: ShieldCheck,
      description: "Learn how we protect your data",
    },
    {
      label: "Pricing",
      path: "/pricing",
      icon: CreditCard,
      description: "Explore plans and subscriptions",
    },
  ],
};

const RESOURCES_MENU: DropdownMenu = {
  label: "Resources",
  icon: Globe2,
  items: [
    {
      label: "About",
      path: "/about",
      icon: Globe2,
      description: "Learn about MukondoGTech AI",
    },
    {
      label: "Help Center",
      path: "/help",
      icon: CircleHelp,
      description: "Guides and support resources",
    },
    {
      label: "Contact",
      path: "/contact",
      icon: FileText,
      description: "Get in touch with our team",
    },
  ],
};

/* ========================================================================
   USER WORKSPACE
   ======================================================================== */

const WORKSPACE_MENU: DropdownMenu = {
  label: "Workspace",
  icon: LayoutDashboard,
  items: [
    {
      label: "Dashboard",
      path: "/dashboard",
      icon: LayoutDashboard,
      description: "Overview and activity",
    },
    {
      label: "AI Assistant",
      path: "/dashboard/chat",
      icon: MessageCircle,
      description: "Your immigration AI assistant",
    },
    {
      label: "Upload Documents",
      path: "/dashboard/upload",
      icon: UploadCloud,
      description: "Analyze immigration documents",
    },
    {
      label: "My Documents",
      path: "/dashboard/documents",
      icon: FileText,
      description: "Manage uploaded documents",
    },
    {
      label: "Applications",
      path: "/dashboard/applications",
      icon: ClipboardCheck,
      description: "Track your applications",
    },
    {
      label: "Reports",
      path: "/dashboard/reports",
      icon: BarChart3,
      description: "View analysis reports",
    },
  ],
};

/* ========================================================================
   ACCOUNT
   ======================================================================== */

const ACCOUNT_MENU: DropdownMenu = {
  label: "Account",
  icon: UserCircle,
  items: [
    {
      label: "Profile",
      path: "/dashboard/profile",
      icon: UserCircle,
      description: "Manage your personal information",
    },
    {
      label: "Subscription",
      path: "/dashboard/billing/subscription",
      icon: CreditCard,
      description: "Manage your subscription",
    },
    {
      label: "Billing History",
      path: "/dashboard/billing/history",
      icon: FileText,
      description: "View previous payments",
    },
    {
      label: "Settings",
      path: "/dashboard/settings/profile",
      icon: Settings,
      description: "Manage application settings",
    },
  ],
};

/* ========================================================================
   ADMIN CENTER
   ======================================================================== */

const ADMIN_MENU: DropdownMenu = {
  label: "Admin Center",
  icon: ShieldCheck,
  items: [
    {
      label: "Admin Dashboard",
      path: "/admin",
      icon: LayoutDashboard,
      description: "Platform overview",
    },
    {
      label: "Analytics",
      path: "/admin/analytics",
      icon: BarChart3,
      description: "Business intelligence",
    },
    {
      label: "User Management",
      path: "/admin/users",
      icon: Users,
      description: "Manage platform users",
    },
    {
      label: "Application Review",
      path: "/admin/applications",
      icon: FileCheck,
      description: "Review immigration applications",
    },
    {
      label: "AI Monitoring",
      path: "/admin/ai",
      icon: BrainCircuit,
      description: "Monitor AI services",
    },
    {
      label: "Knowledge Base",
      path: "/admin/knowledge",
      icon: Database,
      description: "Manage AI knowledge",
    },
    {
      label: "Immigration Rules",
      path: "/admin/rules",
      icon: Scale,
      description: "Manage visa policies",
    },
    {
      label: "Security Center",
      path: "/admin/security",
      icon: LockKeyhole,
      description: "Security monitoring",
    },
    {
      label: "System Health",
      path: "/admin/system",
      icon: Activity,
      description: "Infrastructure health",
    },
    {
      label: "Audit Logs",
      path: "/admin/audit",
      icon: Bell,
      description: "Review system activity",
    },
    {
      label: "Global Settings",
      path: "/admin/settings",
      icon: Settings,
      description: "Platform configuration",
    },
  ],
};

/* ========================================================================
   NAVBAR
   ======================================================================== */

export default function Navbar({
  onMenuClick,
}: NavbarProps) {
  const location = useLocation();
  const navigate = useNavigate();

  const { logout } = useAuth();

  const [authenticated, setAuthenticated] = useState(false);
  const [userRole, setUserRole] = useState<UserRole>("USER");
  const [mobileOpen, setMobileOpen] = useState(false);
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);

  const dropdownRef = useRef<HTMLDivElement | null>(null);

  /* ======================================================================
     AUTHENTICATION
     ====================================================================== */

  const syncAuth = () => {
    const token = window.localStorage.getItem("token");
    const role = window.localStorage.getItem("role");

    setAuthenticated(Boolean(token));
    setUserRole(role === "ADMIN" ? "ADMIN" : "USER");
  };

  useEffect(() => {
    syncAuth();

    window.addEventListener("storage", syncAuth);

    return () => {
      window.removeEventListener("storage", syncAuth);
    };
  }, []);

  /* ======================================================================
     ROUTE CHANGE
     ====================================================================== */

  useEffect(() => {
    setMobileOpen(false);
    setOpenDropdown(null);
  }, [location.pathname]);

  /* ======================================================================
     ESCAPE KEY
     ====================================================================== */

  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setOpenDropdown(null);
        setMobileOpen(false);
      }
    };

    window.addEventListener("keydown", handleEscape);

    return () => {
      window.removeEventListener("keydown", handleEscape);
    };
  }, []);

  /* ======================================================================
     OUTSIDE CLICK
     ====================================================================== */

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setOpenDropdown(null);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);

    return () => {
      document.removeEventListener(
        "mousedown",
        handleOutsideClick,
      );
    };
  }, []);

  /* ======================================================================
     BODY SCROLL LOCK
     ====================================================================== */

  useEffect(() => {
    document.body.style.overflow = mobileOpen
      ? "hidden"
      : "";

    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileOpen]);

  /* ======================================================================
     HELPERS
     ====================================================================== */

  const closeMenus = () => {
    setMobileOpen(false);
    setOpenDropdown(null);
  };

  const toggleDropdown = (label: string) => {
    setOpenDropdown((current) =>
      current === label ? null : label,
    );
  };

  const isActive = (path: string) => {
    return (
      location.pathname === path ||
      location.pathname.startsWith(`${path}/`)
    );
  };

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      window.localStorage.removeItem("token");
      window.localStorage.removeItem("role");

      setAuthenticated(false);
      setUserRole("USER");

      closeMenus();
      navigate("/");
    }
  };

  /* ======================================================================
     DESKTOP DROPDOWN
     ====================================================================== */

  const renderDropdown = (menu: DropdownMenu) => {
    const Icon = menu.icon;
    const opened = openDropdown === menu.label;

    const menuIsActive = menu.items.some((item) =>
      isActive(item.path),
    );

    return (
      <div
        key={menu.label}
        className="relative"
      >
        <button
          type="button"
          aria-haspopup="menu"
          aria-expanded={opened}
          onClick={() => toggleDropdown(menu.label)}
          className={`
            group
            inline-flex
            items-center
            gap-2
            rounded-xl
            px-3.5
            py-2.5
            text-sm
            font-semibold
            transition-all
            duration-200

            ${
              opened || menuIsActive
                ? "bg-white/10 text-[#FFD45A]"
                : "text-slate-200 hover:bg-white/[0.07] hover:text-white"
            }
          `}
        >
          <Icon
            size={16}
            strokeWidth={2}
            aria-hidden="true"
            className={
              opened || menuIsActive
                ? "text-[#F4B81A]"
                : "text-slate-400 group-hover:text-[#FFD45A]"
            }
          />

          <span>{menu.label}</span>

          <ChevronDown
            size={15}
            aria-hidden="true"
            className={`
              transition-transform
              duration-200
              ${
                opened
                  ? "rotate-180 text-[#F4B81A]"
                  : "text-slate-500"
              }
            `}
          />
        </button>

        <AnimatePresence>
          {opened && (
            <motion.div
              initial={{
                opacity: 0,
                y: -8,
                scale: 0.97,
              }}
              animate={{
                opacity: 1,
                y: 0,
                scale: 1,
              }}
              exit={{
                opacity: 0,
                y: -8,
                scale: 0.97,
              }}
              transition={{
                duration: 0.16,
              }}
              role="menu"
              className="
                absolute
                left-0
                top-[calc(100%+10px)]
                z-50
                w-[340px]
                overflow-hidden
                rounded-2xl
                border
                border-slate-200
                bg-white
                p-2
                shadow-[0_20px_60px_rgba(15,23,42,0.22)]
              "
            >
              <div className="px-3 pb-2 pt-2">
                <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-slate-400">
                  {menu.label}
                </p>
              </div>

              <div className="space-y-1">
                {menu.items.map((item) => {
                  const ItemIcon = item.icon;
                  const active = isActive(item.path);

                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      role="menuitem"
                      onClick={closeMenus}
                      className={`
                        group
                        flex
                        items-center
                        gap-3
                        rounded-xl
                        p-3
                        transition-colors
                        duration-200

                        ${
                          active
                            ? "bg-[#FFF6D8]"
                            : "hover:bg-slate-50"
                        }
                      `}
                    >
                      <span
                        className={`
                          flex
                          h-10
                          w-10
                          shrink-0
                          items-center
                          justify-center
                          rounded-xl
                          border
                          transition-colors

                          ${
                            active
                              ? "border-[#F4B81A]/30 bg-[#F4B81A]/15 text-[#B67A00]"
                              : "border-slate-200 bg-slate-50 text-slate-500 group-hover:border-[#F4B81A]/30 group-hover:bg-[#FFF6D8] group-hover:text-[#B67A00]"
                          }
                        `}
                      >
                        <ItemIcon
                          size={18}
                          strokeWidth={2}
                          aria-hidden="true"
                        />
                      </span>

                      <span className="min-w-0 flex-1">
                        <span
                          className={`
                            block
                            text-sm
                            font-bold

                            ${
                              active
                                ? "text-[#8B6100]"
                                : "text-slate-800"
                            }
                          `}
                        >
                          {item.label}
                        </span>

                        {item.description && (
                          <span className="mt-0.5 block truncate text-xs text-slate-500">
                            {item.description}
                          </span>
                        )}
                      </span>

                      <ArrowRight
                        size={15}
                        aria-hidden="true"
                        className="
                          opacity-0
                          text-[#B67A00]
                          transition-all
                          duration-200
                          group-hover:translate-x-0.5
                          group-hover:opacity-100
                        "
                      />
                    </Link>
                  );
                })}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    );
  };

  /* ======================================================================
     MOBILE SECTION
     ====================================================================== */

  const renderMobileSection = (
    menu: DropdownMenu,
  ) => {
    const SectionIcon = menu.icon;

    return (
      <section
        key={menu.label}
        aria-labelledby={`mobile-${menu.label}`}
        className="space-y-2"
      >
        <div className="flex items-center gap-2 px-2 pt-2">
          <SectionIcon
            size={15}
            className="text-[#F4B81A]"
            aria-hidden="true"
          />

          <h2
            id={`mobile-${menu.label}`}
            className="
              text-[10px]
              font-black
              uppercase
              tracking-[0.2em]
              text-slate-400
            "
          >
            {menu.label}
          </h2>
        </div>

        <div className="space-y-1">
          {menu.items.map((item) => {
            const ItemIcon = item.icon;
            const active = isActive(item.path);

            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={closeMenus}
                className={`
                  flex
                  items-center
                  gap-3
                  rounded-xl
                  px-3
                  py-3
                  transition-colors

                  ${
                    active
                      ? "bg-[#F4B81A]/15 text-[#FFD45A]"
                      : "text-slate-200 hover:bg-white/[0.06]"
                  }
                `}
              >
                <span
                  className={`
                    flex
                    h-9
                    w-9
                    shrink-0
                    items-center
                    justify-center
                    rounded-lg

                    ${
                      active
                        ? "bg-[#F4B81A]/15 text-[#F4B81A]"
                        : "bg-white/[0.05] text-slate-400"
                    }
                  `}
                >
                  <ItemIcon
                    size={17}
                    aria-hidden="true"
                  />
                </span>

                <span className="min-w-0">
                  <span className="block text-sm font-semibold">
                    {item.label}
                  </span>

                  {item.description && (
                    <span className="mt-0.5 block truncate text-xs text-slate-500">
                      {item.description}
                    </span>
                  )}
                </span>
              </Link>
            );
          })}
        </div>
      </section>
    );
  };

  /* ======================================================================
     RENDER
     ====================================================================== */

  return (
    <motion.nav
      initial={{
        opacity: 0,
        y: -18,
      }}
      animate={{
        opacity: 1,
        y: 0,
      }}
      transition={{
        duration: 0.45,
        ease: "easeOut",
      }}
      aria-label="Main navigation"
      className="
        fixed
        inset-x-0
        top-0
        z-50
        border-b
        border-white/10
        bg-[#071330]/95
        shadow-[0_8px_30px_rgba(0,0,0,0.18)]
        backdrop-blur-xl
      "
    >
      {/* ==================================================================
          NAVBAR CONTAINER
          ================================================================== */}

      <div
        className="
          flex
          h-[76px]
          w-full
          items-center
          justify-between
          gap-6
          px-4
          sm:px-6
          lg:px-8
        "
      >
        {/* ================================================================
            BRAND
            ================================================================ */}

        <Link
          to="/"
          onClick={closeMenus}
          aria-label="MukondoGTech AI home"
          className="
            group
            flex
            shrink-0
            items-center
            gap-3
          "
        >
          <span
            className="
              relative
              flex
              h-11
              w-11
              items-center
              justify-center
              overflow-hidden
              rounded-xl
              border
              border-[#FFD45A]/40
              bg-gradient-to-br
              from-[#F4B81A]
              to-[#FFD96A]
              shadow-[0_8px_25px_rgba(244,184,26,0.20)]
              transition-transform
              duration-300
              group-hover:scale-[1.03]
            "
          >
            <BrainCircuit
              size={24}
              strokeWidth={2}
              className="text-[#071330]"
              aria-hidden="true"
            />

            <span
              aria-hidden="true"
              className="
                absolute
                inset-0
                bg-white/10
                opacity-0
                transition-opacity
                duration-300
                group-hover:opacity-100
              "
            />
          </span>

          <span className="hidden sm:block">
            <span className="block text-[17px] font-black tracking-tight text-white">
              MukondoGTech
              <span className="ml-1 text-[#F4B81A]">
                AI
              </span>
            </span>

            <span className="block text-[9px] font-semibold uppercase tracking-[0.18em] text-slate-400">
              Immigration Intelligence
            </span>
          </span>
        </Link>

        {/* ================================================================
            DESKTOP NAVIGATION
            ================================================================ */}

        <div
          ref={dropdownRef}
          className="
            hidden
            flex-1
            items-center
            justify-center
            gap-1
            xl:flex
          "
        >
          {!authenticated ? (
            <>
              {renderDropdown(PRODUCT_MENU)}
              {renderDropdown(RESOURCES_MENU)}
            </>
          ) : (
            <>
              {renderDropdown(WORKSPACE_MENU)}
              {renderDropdown(ACCOUNT_MENU)}

              {userRole === "ADMIN" &&
                renderDropdown(ADMIN_MENU)}
            </>
          )}
        </div>

        {/* ================================================================
            DESKTOP ACTIONS
            ================================================================ */}

        <div className="hidden items-center gap-2 lg:flex">
          {!authenticated ? (
            <>
              <Link
                to="/login"
                className="
                  rounded-xl
                  px-4
                  py-2.5
                  text-sm
                  font-semibold
                  text-slate-200
                  transition-colors
                  hover:bg-white/[0.06]
                  hover:text-[#FFD45A]
                "
              >
                Sign In
              </Link>

              <Link
                to="/register"
                className="
                  group
                  inline-flex
                  items-center
                  gap-2
                  rounded-xl
                  border
                  border-[#FFD45A]
                  bg-gradient-to-r
                  from-[#F4B81A]
                  to-[#FFD96A]
                  px-5
                  py-2.5
                  text-sm
                  font-extrabold
                  text-[#071330]
                  shadow-[0_8px_25px_rgba(244,184,26,0.18)]
                  transition-all
                  duration-200
                  hover:-translate-y-0.5
                  hover:shadow-[0_12px_32px_rgba(244,184,26,0.28)]
                "
              >
                <span>Start Free</span>

                <ArrowRight
                  size={16}
                  aria-hidden="true"
                  className="
                    transition-transform
                    duration-200
                    group-hover:translate-x-0.5
                  "
                />
              </Link>
            </>
          ) : (
            <>
              <Link
                to="/dashboard"
                className="
                  inline-flex
                  items-center
                  gap-2
                  rounded-xl
                  border
                  border-white/10
                  bg-white/[0.04]
                  px-4
                  py-2.5
                  text-sm
                  font-semibold
                  text-slate-200
                  transition-all
                  hover:border-white/20
                  hover:bg-white/[0.08]
                  hover:text-white
                "
              >
                <LayoutDashboard
                  size={17}
                  aria-hidden="true"
                />

                Dashboard
              </Link>

              <button
                type="button"
                onClick={handleLogout}
                className="
                  inline-flex
                  items-center
                  gap-2
                  rounded-xl
                  px-3.5
                  py-2.5
                  text-sm
                  font-semibold
                  text-red-300
                  transition-colors
                  hover:bg-red-500/10
                  hover:text-red-200
                "
              >
                <LogOut
                  size={17}
                  aria-hidden="true"
                />

                Logout
              </button>
            </>
          )}
        </div>

        {/* ================================================================
            MOBILE MENU BUTTON
            ================================================================ */}

        <button
          type="button"
          aria-label={
            mobileOpen
              ? "Close navigation menu"
              : "Open navigation menu"
          }
          aria-expanded={mobileOpen}
          onClick={() => {
            onMenuClick?.();
            setMobileOpen((current) => !current);
          }}
          className="
            flex
            h-10
            w-10
            items-center
            justify-center
            rounded-xl
            border
            border-white/10
            bg-white/[0.04]
            text-slate-200
            transition-colors
            hover:bg-white/[0.08]
            hover:text-[#FFD45A]
            lg:hidden
          "
        >
          <AnimatePresence
            mode="wait"
            initial={false}
          >
            {mobileOpen ? (
              <motion.span
                key="close"
                initial={{
                  opacity: 0,
                  rotate: -90,
                }}
                animate={{
                  opacity: 1,
                  rotate: 0,
                }}
                exit={{
                  opacity: 0,
                  rotate: 90,
                }}
              >
                <X
                  size={22}
                  aria-hidden="true"
                />
              </motion.span>
            ) : (
              <motion.span
                key="menu"
                initial={{
                  opacity: 0,
                  rotate: 90,
                }}
                animate={{
                  opacity: 1,
                  rotate: 0,
                }}
                exit={{
                  opacity: 0,
                  rotate: -90,
                }}
              >
                <Menu
                  size={22}
                  aria-hidden="true"
                />
              </motion.span>
            )}
          </AnimatePresence>
        </button>
      </div>

      {/* ==================================================================
          MOBILE DRAWER
          ================================================================== */}

      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{
              opacity: 0,
              height: 0,
            }}
            animate={{
              opacity: 1,
              height: "auto",
            }}
            exit={{
              opacity: 0,
              height: 0,
            }}
            transition={{
              duration: 0.25,
              ease: "easeOut",
            }}
            className="
              overflow-hidden
              border-t
              border-white/10
              bg-[#071330]
              lg:hidden
            "
          >
            <div
              className="
                max-h-[calc(100vh-76px)]
                overflow-y-auto
                px-4
                pb-6
                pt-4
                sm:px-6
              "
            >
              {/* ==========================================================
                  MOBILE BRAND CONTEXT
                  ========================================================== */}

              <div
                className="
                  mb-5
                  flex
                  items-center
                  gap-3
                  rounded-2xl
                  border
                  border-white/10
                  bg-white/[0.03]
                  p-3
                "
              >
                <div
                  className="
                    flex
                    h-10
                    w-10
                    items-center
                    justify-center
                    rounded-xl
                    bg-gradient-to-br
                    from-[#F4B81A]
                    to-[#FFD96A]
                  "
                >
                  <BrainCircuit
                    size={21}
                    className="text-[#071330]"
                    aria-hidden="true"
                  />
                </div>

                <div>
                  <p className="text-sm font-bold text-white">
                    MukondoGTech AI
                  </p>

                  <p className="text-xs text-slate-500">
                    Immigration Intelligence
                  </p>
                </div>
              </div>

              {/* ==========================================================
                  MOBILE NAVIGATION
                  ========================================================== */}

              <div className="space-y-6">
                {!authenticated ? (
                  <>
                    {renderMobileSection(
                      PRODUCT_MENU,
                    )}

                    {renderMobileSection(
                      RESOURCES_MENU,
                    )}
                  </>
                ) : (
                  <>
                    {renderMobileSection(
                      WORKSPACE_MENU,
                    )}

                    {renderMobileSection(
                      ACCOUNT_MENU,
                    )}

                    {userRole === "ADMIN" &&
                      renderMobileSection(
                        ADMIN_MENU,
                      )}
                  </>
                )}
              </div>

              {/* ==========================================================
                  MOBILE ACTIONS
                  ========================================================== */}

              <div className="mt-6 border-t border-white/10 pt-5">
                {!authenticated ? (
                  <div className="grid grid-cols-2 gap-3">
                    <Link
                      to="/login"
                      onClick={closeMenus}
                      className="
                        inline-flex
                        items-center
                        justify-center
                        rounded-xl
                        border
                        border-white/10
                        px-4
                        py-3
                        text-sm
                        font-bold
                        text-white
                        transition-colors
                        hover:bg-white/[0.06]
                      "
                    >
                      Sign In
                    </Link>

                    <Link
                      to="/register"
                      onClick={closeMenus}
                      className="
                        inline-flex
                        items-center
                        justify-center
                        gap-2
                        rounded-xl
                        bg-gradient-to-r
                        from-[#F4B81A]
                        to-[#FFD96A]
                        px-4
                        py-3
                        text-sm
                        font-extrabold
                        text-[#071330]
                      "
                    >
                      Start Free

                      <ArrowRight
                        size={16}
                        aria-hidden="true"
                      />
                    </Link>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="
                      flex
                      w-full
                      items-center
                      justify-center
                      gap-2
                      rounded-xl
                      border
                      border-red-400/20
                      bg-red-500/[0.06]
                      px-4
                      py-3
                      text-sm
                      font-bold
                      text-red-300
                      transition-colors
                      hover:bg-red-500/10
                    "
                  >
                    <LogOut
                      size={17}
                      aria-hidden="true"
                    />

                    Sign Out
                  </button>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.nav>
  );
}

