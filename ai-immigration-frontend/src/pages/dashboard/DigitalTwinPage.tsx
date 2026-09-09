import { useCallback, useEffect, useState } from "react";

import { Link } from "react-router-dom";

import { motion } from "framer-motion";

import {
  ChevronRight,
  Clock,
  Compass,
  IdCard,
  RefreshCw,
  ShieldQuestion,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";

import { getDigitalTwinApi } from "../../api/digitalTwinApi";
import { errorService } from "../../services/errorService";

import { formatFactKeyLabel, getCategoryLabel } from "../../utils/factLabels";

import type { DigitalTwin } from "../../types/digitalTwin";
import type { Fact, FactCategory } from "../../types/fact";

import FactCard from "../../components/fact/FactCard";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";

/**
 * ============================================================================
 * DIGITAL TWIN / IMMIGRATION PROFILE PAGE
 * ============================================================================
 *
 * Renders GET /api/twin/{subjectUserId} - "what does MukondoGTech AI
 * currently know about this case." This page NEVER stores, diffs, or
 * persists the Fact data it receives: every visit re-fetches the live
 * projection from the backend, which itself is a pure read projection over
 * FACTS/FACT_CONFLICTS, never a second source of truth.
 *
 * SECURITY:
 *
 * The subject is always the authenticated user's own ID, resolved from
 * AuthContext. This route takes no subjectUserId parameter of any kind
 * (URL, query string, or otherwise) - there is no frontend surface through
 * which a user could request another subject's twin. The backend
 * independently re-verifies authorization on every call regardless.
 * ============================================================================
 */

/** Fixed, human-sensible section order - only categories present in the data are rendered. */
const CATEGORY_ORDER: FactCategory[] = [
  "IDENTITY",
  "NATIONALITY_CITIZENSHIP",
  "RESIDENCE",
  "IMMIGRATION_STATUS",
  "IMMIGRATION_HISTORY",
  "EDUCATION",
  "EMPLOYMENT",
  "PROFESSIONAL_CREDENTIALS",
  "LANGUAGE_PROFICIENCY",
  "FINANCES",
  "FAMILY_DEPENDANTS",
  "RELATIONSHIPS",
  "TRAVEL_HISTORY",
  "LEGAL_ADMINISTRATIVE_HISTORY",
  "HEALTH_IMMIGRATION_RELEVANT",
  "REGULATORY_CONTEXT",
  "CASE_PROCESS_INTERACTION",
];

function getDigitalTwinErrorMessage(error: unknown): string {

  const status = errorService.getStatus(error);

  if (status === 404) {
    return "We could not find an immigration profile for your account.";
  }

  if (status === 403) {
    return "You are not authorized to view this profile.";
  }

  return errorService.getMessage(error);
}

export default function DigitalTwinPage() {

  const { user } = useAuth();

  const [twin, setTwin] = useState<DigitalTwin | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadTwin = useCallback(async () => {

    if (!user?.id) {
      setError("Your account could not be identified. Please sign in again.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const result = await getDigitalTwinApi(user.id);

      setTwin(result);

    } catch (err) {
      setError(getDigitalTwinErrorMessage(err));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  useEffect(() => {
    void loadTwin();
  }, [loadTwin]);

  if (loading) {
    return (
      <div className="w-full px-4 py-16 sm:px-6 lg:px-8">
        <Loader text="Loading your immigration profile..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full space-y-4 px-4 py-10 sm:px-6 lg:px-8">
        <ErrorAlert message={error} />
        <Button variant="secondary" onClick={() => void loadTwin()}>
          <RefreshCw size={16} />
          Try again
        </Button>
      </div>
    );
  }

  if (!twin) {
    return null;
  }

  const hasAnything = twin.currentFacts.length > 0 || twin.openConflicts.length > 0;

  const factsByCategory = new Map<FactCategory, Fact[]>();
  for (const fact of twin.currentFacts) {
    const bucket = factsByCategory.get(fact.category) ?? [];
    bucket.push(fact);
    factsByCategory.set(fact.category, bucket);
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="w-full px-4 py-10 sm:px-6 lg:px-8"
    >
      <header
        className="
          rounded-2xl
          border
          border-white/10
          bg-white/5
          p-6
          shadow-sm
        "
      >
        <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#C6A15B]/20 bg-[#C6A15B]/10 px-3 py-1 text-xs font-semibold text-[#C6A15B]">
          <IdCard size={14} />
          Your Immigration Profile
        </div>

        <h1 className="text-2xl font-black text-white sm:text-3xl">
          What MukondoGTech AI knows about your case
        </h1>

        <p className="mt-2 max-w-2xl text-sm text-slate-400">
          This is a live view, generated fresh from your case information as of{" "}
          {new Date(twin.generatedAt).toLocaleString()}. {twin.note}
        </p>

        <div className="mt-5 flex flex-wrap gap-3">
          <Link to="/dashboard/pathways/assessments/new">
            <Button variant="primary">
              <Compass size={16} />
              Discover Pathways You May Qualify For
            </Button>
          </Link>

          <Link to="/dashboard/case-timeline">
            <Button variant="outline">
              <Clock size={16} />
              Case Timeline &amp; Signals
            </Button>
          </Link>
        </div>
      </header>

      {!hasAnything && (
        <div className="mt-8">
          <EmptyState
            icon={<IdCard size={40} />}
            title="No immigration information yet"
            description="Your immigration profile doesn't contain any information yet. As documents are processed and details are recorded on your case, they will appear here."
          />
        </div>
      )}

      {twin.openConflicts.length > 0 && (
        <section className="mt-8">
          <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-white">
            <ShieldQuestion size={20} className="text-amber-400" />
            Needs Your Attention
          </h2>

          <ul className="space-y-3">
            {twin.openConflicts.map((conflict) => (
              <li
                key={conflict.id}
                className="rounded-xl border border-amber-400/20 bg-amber-400/10 p-4"
              >
                <p className="text-sm font-semibold text-amber-300">
                  Conflicting information for {formatFactKeyLabel(conflict.factKey)}
                </p>
                <p className="mt-1 text-xs text-amber-200/80">
                  We found two different values and this needs review. This is not a finding of
                  fraud - it simply means the available evidence disagrees.
                </p>
                <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                  <p className="text-[11px] text-amber-200/60">
                    Detected {new Date(conflict.detectedAt).toLocaleDateString()}
                  </p>

                  <Link
                    to={`/dashboard/immigration-profile/conflicts/${conflict.id}`}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-amber-300 transition hover:text-amber-200"
                  >
                    Review this conflict
                    <ChevronRight size={14} />
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      {factsByCategory.size > 0 && (
        <div className="mt-8 space-y-8">
          {CATEGORY_ORDER.filter((category) => factsByCategory.has(category)).map((category) => (
            <section key={category}>
              <h2 className="mb-4 text-lg font-bold text-white">{getCategoryLabel(category)}</h2>

              <ul className="space-y-3">
                {(factsByCategory.get(category) ?? []).map((fact) => (
                  <li key={fact.id}>
                    <FactCard fact={fact} />
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}
    </motion.div>
  );
}
