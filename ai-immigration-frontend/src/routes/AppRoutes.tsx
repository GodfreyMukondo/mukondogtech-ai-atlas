import React, { lazy, Suspense } from "react";

import {
  Navigate,
  Route,
  Routes,
} from "react-router-dom";

// ======================================================
// LAYOUTS
// ======================================================

import LandingLayout from "../components/layouts/LandingLayout";
import AuthLayout from "../components/layouts/AuthLayout";
import DashboardLayout from "../components/layouts/DashboardLayout";
import AdminLayout from "../components/layouts/AdminLayout";

// ======================================================
// ROUTE GUARDS
// ======================================================

import ProtectedRoute from "./ProtectedRoute";
import AdminRoute from "./AdminRoute";

// ======================================================
// GLOBAL COMPONENTS
// ======================================================

import Loader from "../components/common/Loader";

// ======================================================
// PUBLIC PAGES
// ======================================================

const HomePage = lazy(
  () => import("../pages/HomePage")
);

const DemoPage = lazy(
  () => import("../pages/DemoPage")
);

const PricingPage = lazy(
  () => import("../pages/PricingPage")
);

const SecurityPage = lazy(
  () => import("../pages/SecurityPage")
);

const ChatPage = lazy(
  () => import("../pages/ChatPage")
);

// ======================================================
// SUPPORT PAGES
// ======================================================

const AboutPage = lazy(
  () => import("../pages/support/AboutPage")
);

const ContactPage = lazy(
  () => import("../pages/support/ContactPage")
);

const HelpFaqPage = lazy(
  () => import("../pages/support/HelpFaqPage")
);

// ======================================================
// LEGAL PAGES
// ======================================================

const PrivacyPolicyPage = lazy(
  () => import("../pages/legal/PrivacyPolicyPage")
);

const TermsOfServicePage = lazy(
  () => import("../pages/legal/TermsOfServicePage")
);

// ======================================================
// AUTHENTICATION PAGES
// ======================================================

const LoginPage = lazy(
  () => import("../pages/auth/LoginPage")
);

const RegisterPage = lazy(
  () => import("../pages/auth/RegisterPage")
);

const VerifyEmailPage = lazy(
  () => import("../pages/auth/VerifyEmailPage")
);

const ForgotPasswordPage = lazy(
  () => import("../pages/auth/ForgotPasswordPage")
);

const ResetPasswordPage = lazy(
  () => import("../pages/auth/ResetPasswordPage")
);

const ChangePasswordPage = lazy(
  () => import("../pages/auth/ChangePasswordPage")
);

// ======================================================
// USER DASHBOARD PAGES
// ======================================================

const DashboardPage = lazy(
  () => import("../pages/dashboard/DashboardPage")
);

const DocumentsPage = lazy(
  () => import("../pages/dashboard/DocumentsPage")
);

const AIChatPage = lazy(
  () => import("../pages/dashboard/AIChatPage")
);

const ReportsPage = lazy(
  () => import("../pages/dashboard/ReportsPage")
);

const ProfilePage = lazy(
  () => import("../pages/dashboard/ProfilePage")
);

const ApplicationsPage = lazy(
  () => import("../pages/dashboard/ApplicationsPage")
);

const CaseIntelligencePage = lazy(
  () => import("../pages/dashboard/CaseIntelligencePage")
);

const CaseTimelinePage = lazy(
  () => import("../pages/dashboard/CaseTimelinePage")
);

// ======================================================
// ACCOUNT PAGES
// ======================================================

const AccountPage = lazy(
  () => import("../pages/account/AccountPage")
);

const DeleteAccountPage = lazy(
  () => import("../pages/account/DeleteAccountPage")
);

// ======================================================
// BILLING PAGES
// ======================================================

const SubscriptionPage = lazy(
  () => import("../pages/billing/SubscriptionPage")
);

const BillingHistoryPage = lazy(
  () => import("../pages/billing/BillingHistoryPage")
);

const CheckoutPage = lazy(
  () => import("../pages/billing/CheckoutPage")
);

// ======================================================
// SETTINGS PAGES
// ======================================================

const ProfileSettingsPage = lazy(
  () => import("../pages/settings/ProfileSettingsPage")
);

const SecuritySettingsPage = lazy(
  () => import("../pages/settings/SecuritySettingsPage")
);

const NotificationSettingsPage = lazy(
  () => import("../pages/settings/NotificationSettingsPage")
);

// ======================================================
// ADMIN PAGES
// ======================================================

const AdminDashboardPage = lazy(
  () => import("../pages/admin/AdminDashboardPage")
);

const AnalyticsPage = lazy(
  () => import("../pages/admin/AnalyticsPage")
);

const UserManagementPage = lazy(
  () => import("../pages/admin/UserManagementPage")
);

const CreateUserPage = lazy(
  () => import("../pages/admin/CreateUserPage")
);

const ApplicationReviewPage = lazy(
  () => import("../pages/admin/ApplicationReviewPage")
);

const AIModelMonitoringPage = lazy(
  () => import("../pages/admin/AIModelMonitoringPage")
);

const KnowledgeBasePage = lazy(
  () => import("../pages/admin/KnowledgeBasePage")
);

const ImmigrationRulesPage = lazy(
  () => import("../pages/admin/ImmigrationRulesPage")
);

const SecurityCenterPage = lazy(
  () => import("../pages/admin/SecurityCenterPage")
);

const SystemHealthPage = lazy(
  () => import("../pages/admin/SystemHealthPage")
);

const AuditLogsPage = lazy(
  () => import("../pages/admin/AuditLogsPage")
);

const GlobalSettingsPage = lazy(
  () => import("../pages/admin/GlobalSettingsPage")
);

// ======================================================
// SYSTEM PAGES
// ======================================================

const MaintenancePage = lazy(
  () => import("../pages/MaintenancePage")
);

const ServerErrorPage = lazy(
  () => import("../pages/ServerErrorPage")
);

const NotFoundPage = lazy(
  () => import("../pages/NotFoundPage")
);

// ======================================================
// ROUTE LOADER
// ======================================================

interface RouteLoaderProps {
  children: React.ReactNode;
}

function RouteLoader({
  children,
}: RouteLoaderProps) {
  return (
    <Suspense fallback={<Loader />}>
      {children}
    </Suspense>
  );
}

// ======================================================
// APPLICATION ROUTES
// ======================================================

export default function AppRoutes() {
  return (
    <RouteLoader>
      <Routes>

        {/* ==================================================
            PUBLIC APPLICATION
        ================================================== */}

        <Route element={<LandingLayout />}>

          {/* --------------------------------------------------
              HOME
          -------------------------------------------------- */}

          <Route
            path="/"
            element={<HomePage />}
          />

          {/* --------------------------------------------------
              PRODUCT
          -------------------------------------------------- */}

          <Route
            path="/demo"
            element={<DemoPage />}
          />

          <Route
            path="/pricing"
            element={<PricingPage />}
          />

          <Route
            path="/security"
            element={<SecurityPage />}
          />

          {/* --------------------------------------------------
              PUBLIC AI ASSISTANT
          -------------------------------------------------- */}

          <Route
            path="/chat"
            element={<ChatPage />}
          />

          {/* --------------------------------------------------
              SUPPORT
          -------------------------------------------------- */}

          <Route
            path="/about"
            element={<AboutPage />}
          />

          <Route
            path="/contact"
            element={<ContactPage />}
          />

          <Route
            path="/help"
            element={<HelpFaqPage />}
          />

          {/* --------------------------------------------------
              LEGAL
          -------------------------------------------------- */}

          <Route
            path="/privacy-policy"
            element={<PrivacyPolicyPage />}
          />

          <Route
            path="/terms-of-service"
            element={<TermsOfServicePage />}
          />

        </Route>

        {/* ==================================================
            AUTHENTICATION
        ================================================== */}

        <Route element={<AuthLayout />}>

          <Route
            path="/login"
            element={<LoginPage />}
          />

          <Route
            path="/register"
            element={<RegisterPage />}
          />

          <Route
            path="/verify-email"
            element={<VerifyEmailPage />}
          />

          <Route
            path="/forgot-password"
            element={<ForgotPasswordPage />}
          />

          <Route
            path="/reset-password"
            element={<ResetPasswordPage />}
          />

        </Route>

        {/* ==================================================
            PROTECTED APPLICATION
        ================================================== */}

        <Route element={<ProtectedRoute />}>

          {/* ==================================================
              USER WORKSPACE
          ================================================== */}

          <Route element={<DashboardLayout />}>

            {/* ------------------------------------------------
                ACCOUNT SECURITY
            ------------------------------------------------ */}

            <Route
              path="/change-password"
              element={<ChangePasswordPage />}
            />

            {/* ------------------------------------------------
                DASHBOARD
            ------------------------------------------------ */}

            <Route
              path="/dashboard"
              element={<DashboardPage />}
            />

            <Route
              path="/dashboard/chat"
              element={<AIChatPage />}
            />

            <Route
              path="/dashboard/documents"
              element={<DocumentsPage />}
            />

            <Route
              path="/dashboard/reports"
              element={<ReportsPage />}
            />

            <Route
              path="/dashboard/applications"
              element={<ApplicationsPage />}
            />

            <Route
              path="/dashboard/pathways/assessments/:assessmentId/intelligence"
              element={<CaseIntelligencePage />}
            />

            <Route
              path="/dashboard/case-timeline"
              element={<CaseTimelinePage />}
            />

            <Route
              path="/dashboard/profile"
              element={<ProfilePage />}
            />

            {/* ------------------------------------------------
                ACCOUNT
            ------------------------------------------------ */}

            <Route
              path="/dashboard/account"
              element={<AccountPage />}
            />

            <Route
              path="/dashboard/account/delete"
              element={<DeleteAccountPage />}
            />

            {/* ------------------------------------------------
                BILLING
            ------------------------------------------------ */}

            <Route
              path="/dashboard/billing"
              element={
                <Navigate
                  to="/dashboard/billing/subscription"
                  replace
                />
              }
            />

            <Route
              path="/dashboard/billing/subscription"
              element={<SubscriptionPage />}
            />

            <Route
              path="/dashboard/billing/history"
              element={<BillingHistoryPage />}
            />

            <Route
              path="/dashboard/billing/checkout"
              element={<CheckoutPage />}
            />

            {/* ------------------------------------------------
                SETTINGS
            ------------------------------------------------ */}

            <Route
              path="/dashboard/settings/profile"
              element={<ProfileSettingsPage />}
            />

            <Route
              path="/dashboard/settings/security"
              element={<SecuritySettingsPage />}
            />

            <Route
              path="/dashboard/settings/notifications"
              element={<NotificationSettingsPage />}
            />

          </Route>

          {/* ==================================================
              ADMIN APPLICATION
          ================================================== */}

          <Route element={<AdminRoute />}>

            <Route element={<AdminLayout />}>

              {/* ------------------------------------------------
                  ADMIN DASHBOARD
              ------------------------------------------------ */}

              <Route
                path="/admin"
                element={<AdminDashboardPage />}
              />

              {/* ------------------------------------------------
                  ANALYTICS
              ------------------------------------------------ */}

              <Route
                path="/admin/analytics"
                element={<AnalyticsPage />}
              />

              {/* ------------------------------------------------
                  USER MANAGEMENT
              ------------------------------------------------ */}

              <Route
                path="/admin/users"
                element={<UserManagementPage />}
              />

              <Route
                path="/admin/users/create"
                element={<CreateUserPage />}
              />

              {/* ------------------------------------------------
                  APPLICATION REVIEW
              ------------------------------------------------ */}

              <Route
                path="/admin/applications"
                element={<ApplicationReviewPage />}
              />

              {/* ------------------------------------------------
                  AI
              ------------------------------------------------ */}

              <Route
                path="/admin/ai"
                element={<AIModelMonitoringPage />}
              />

              {/* ------------------------------------------------
                  KNOWLEDGE BASE
              ------------------------------------------------ */}

              <Route
                path="/admin/knowledge"
                element={<KnowledgeBasePage />}
              />

              {/* ------------------------------------------------
                  IMMIGRATION RULES
              ------------------------------------------------ */}

              <Route
                path="/admin/rules"
                element={<ImmigrationRulesPage />}
              />

              {/* ------------------------------------------------
                  SECURITY
              ------------------------------------------------ */}

              <Route
                path="/admin/security"
                element={<SecurityCenterPage />}
              />

              {/* ------------------------------------------------
                  SYSTEM
              ------------------------------------------------ */}

              <Route
                path="/admin/system"
                element={<SystemHealthPage />}
              />

              {/* ------------------------------------------------
                  AUDIT
              ------------------------------------------------ */}

              <Route
                path="/admin/audit"
                element={<AuditLogsPage />}
              />

              {/* ------------------------------------------------
                  GLOBAL SETTINGS
              ------------------------------------------------ */}

              <Route
                path="/admin/settings"
                element={<GlobalSettingsPage />}
              />

            </Route>

          </Route>

        </Route>

        {/* ==================================================
            SYSTEM ROUTES
        ================================================== */}

        <Route
          path="/maintenance"
          element={<MaintenancePage />}
        />

        <Route
          path="/server-error"
          element={<ServerErrorPage />}
        />

        {/* ==================================================
            CATCH-ALL / 404
        ================================================== */}

        <Route
          path="*"
          element={<NotFoundPage />}
        />

      </Routes>
    </RouteLoader>
  );
}