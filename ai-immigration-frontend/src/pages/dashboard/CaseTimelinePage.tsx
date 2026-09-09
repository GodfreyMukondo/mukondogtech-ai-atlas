import { useCallback, useEffect, useState } from "react";

import { Link } from "react-router-dom";

import { motion } from "framer-motion";

import {
  ChevronRight,
  Clock,
  History,
  RefreshCw,
  ShieldQuestion,
  Sparkles,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";

import {
  getCaseContradictionsApi,
  getCaseSignalsApi,
  getCaseTimelineApi,
} from "../../api/caseIntelligenceApi";
import { errorService } from "../../services/errorService";
import { getRiskBandBadgeVariant, getRiskBandLabel } from "../../utils/caseIntelligenceLabels";

import type { CaseContradictions, CaseSignals, CaseTimeline } from "../../types/caseIntelligence";

import FactCard from "../../components/fact/FactCard";
import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";

/**
 * ============================================================================
 * CASE TIMELINE & SIGNALS PAGE
 * ============================================================================
 *
 * Case-wide (not tied to any one Pathway) view built from three Phase 2
 * endpoints: GET /api/cases/{subjectUserId}/timeline, /contradictions, and
 * /signals. The subject is always the authenticated user's own ID, resolved
 * from AuthContext - exactly like DigitalTwinPage, this route takes no
 * subjectUserId parameter of any kind, and the backend independently
 * re-verifies authorization on every call regardless.
 *
 * This page deliberately does NOT duplicate the Immigration Profile page's
 * own conflict-review UI - open contradictions are surfaced here only as a
 * count with a link back to that page, which remains the single place to
 * review and act on them.
 *
 * IMPORTANT: a gap between two timeline entries is a plain day count with
 * no legal significance, and the signals risk band is a non-forensic
 * indicator only - neither is ever presented as a fraud finding.
 * ============================================================================
 */
export default function CaseTimelinePage() {

  const { user } = useAuth();

  const [timeline, setTimeline] = useState<CaseTimeline | null>(null);
  const [contradictions, setContradictions] = useState<CaseContradictions | null>(null);
  const [signals, setSignals] = useState<CaseSignals | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {

    if (!user?.id) {
      setError("Your account could not be identified. Please sign in again.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const [timelineResult, contradictionsResult, signalsResult] = await Promise.all([
        getCaseTimelineApi(user.id),
        getCaseContradictionsApi(user.id),
        getCaseSignalsApi(user.id),
      ]);

      setTimeline(timelineResult);
      setContradictions(contradictionsResult);
      setSignals(signalsResult);

    } catch (err) {
      setError(errorService.getMessage(err));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return (
      <div className="w-full px-4 py-16 sm:px-6 lg:px-8">
        <Loader text="Building your case timeline..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full space-y-4 px-4 py-10 sm:px-6 lg:px-8">
        <ErrorAlert message={error} />
        <Button variant="secondary" onClick={() => void load()}>
          <RefreshCw size={16} />
          Try again
        </Button>
      </div>
    );
  }

  if (!timeline || !signals || !contradictions) {
    return null;
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
          <History size={14} />
          Case Timeline &amp; Signals
        </div>

        <h1 className="text-2xl font-black text-white sm:text-3xl">
          The chronological story of your case
        </h1>

        <p className="mt-2 max-w-2xl text-sm text-slate-400">
          Built fresh from your current case information as of{" "}
          {new Date(timeline.generatedAt).toLocaleString()}. {timeline.note}
        </p>
      </header>

      <SignalsSection signals={signals} />

      {contradictions.openContradictions.length > 0 && (
        <section className="mt-8">
          <Link
            to="/dashboard/immigration-profile"
            className="flex items-center justify-between gap-3 rounded-xl border border-amber-400/20 bg-amber-400/10 p-4 transition hover:border-amber-400/40"
          >
            <div className="flex items-center gap-2 text-sm font-semibold text-amber-300">
              <ShieldQuestion size={18} />
              {contradictions.openContradictions.length} open contradiction
              {contradictions.openContradictions.length === 1 ? "" : "s"} need
              {contradictions.openContradictions.length === 1 ? "s" : ""} your attention
            </div>
            <ChevronRight size={16} className="text-amber-300" />
          </Link>
        </section>
      )}

      {contradictions.potentialOverlaps.length > 0 && (
        <section className="mt-8">
          <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-white">
            <ShieldQuestion size={20} className="text-amber-400" />
            Potential Overlaps
          </h2>

          <ul className="space-y-3">
            {contradictions.potentialOverlaps.map((overlap, index) => (
              <li
                key={`${overlap.factKey}-${overlap.factAId}-${overlap.factBId}-${index}`}
                className="rounded-xl border border-amber-400/20 bg-amber-400/10 p-4"
              >
                <p className="text-sm font-semibold text-amber-300">
                  Potential contradiction in {overlap.factKey}
                </p>
                <p className="mt-1 text-xs text-amber-200/80">{overlap.description}</p>
                <p className="mt-2 text-[11px] text-amber-200/60">
                  Overlap of {overlap.overlapDays} day{overlap.overlapDays === 1 ? "" : "s"} - requires
                  verification, not a finding of fraud.
                </p>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="mt-8">
        <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-white">
          <Clock size={20} className="text-[#C6A15B]" />
          Timeline
        </h2>

        {timeline.events.length === 0 ? (
          <EmptyState
            icon={<Clock size={40} />}
            title="No timeline entries yet"
            description="As employment, education, residence, travel, and immigration history are recorded on your case, they will appear here in order."
          />
        ) : (
          <ul className="space-y-4">
            {timeline.events.map((event) => (
              <li key={event.fact.id}>
                {event.gapDaysBeforeThisEntry != null && (
                  <div className="mb-2 flex items-center gap-2 pl-2 text-xs text-slate-500">
                    <span className="h-px flex-1 bg-white/10" />
                    <span>{event.gapDaysBeforeThisEntry} day{event.gapDaysBeforeThisEntry === 1 ? "" : "s"} since the previous record</span>
                    <span className="h-px flex-1 bg-white/10" />
                  </div>
                )}
                {event.overlapDaysWithPreviousEntry != null && (
                  <div className="mb-2 flex items-center gap-2 pl-2 text-xs text-amber-400">
                    <span className="h-px flex-1 bg-amber-400/20" />
                    <span>
                      Overlaps the previous record by {event.overlapDaysWithPreviousEntry} day
                      {event.overlapDaysWithPreviousEntry === 1 ? "" : "s"} - potential contradiction
                    </span>
                    <span className="h-px flex-1 bg-amber-400/20" />
                  </div>
                )}
                <FactCard fact={event.fact} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </motion.div>
  );
}

/* ============================================================================
   SIGNALS SECTION
============================================================================ */

function SignalsSection({ signals }: { signals: CaseSignals }) {
  return (
    <section className="mt-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="flex items-center gap-2 text-lg font-bold text-white">
          <Sparkles size={20} className="text-[#C6A15B]" />
          Evidence &amp; Anomaly Signals
        </h2>
        <Badge variant={getRiskBandBadgeVariant(signals.riskBand)}>
          {getRiskBandLabel(signals.riskBand)} band
        </Badge>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-7">
        <SignalTile label="Fraud-flagged docs" value={signals.fraudFlaggedDocumentCount} />
        <SignalTile label="High-risk docs" value={signals.highRiskDocumentCount} />
        <SignalTile label="Medium-risk docs" value={signals.mediumRiskDocumentCount} />
        <SignalTile label="Open contradictions" value={signals.openContradictionCount} />
        <SignalTile label="Potential overlaps" value={signals.potentialOverlapContradictionCount} />
        <SignalTile label="Rejected evidence" value={signals.rejectedEvidenceItemCount} />
        <SignalTile label="Failed validation" value={signals.validationFailedEvidenceItemCount} />
      </div>

      <p className="mt-3 text-xs text-slate-500">{signals.note}</p>
    </section>
  );
}

function SignalTile({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-white/10 bg-white/5 p-4">
      <p className="text-2xl font-black tabular-nums text-white">{value}</p>
      <p className="mt-1 text-xs text-slate-500">{label}</p>
    </div>
  );
}
