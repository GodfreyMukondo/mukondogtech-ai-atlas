import { useCallback, useEffect, useState } from "react";

import { Link, useNavigate, useParams } from "react-router-dom";

import { motion } from "framer-motion";

import {
  ArrowLeft,
  BadgeCheck,
  CheckCircle2,
  Gavel,
  RefreshCw,
  ScrollText,
  ShieldQuestion,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";

import { confirmFactConflictApi, getFactApi, getFactConflictApi } from "../../api/factApi";
import { errorService } from "../../services/errorService";
import { notificationService } from "../../services/notificationService";
import {
  formatFactKeyLabel,
  formatFactValue,
  getConflictResolutionLabel,
} from "../../utils/factLabels";

import type { Fact, FactConflict } from "../../types/fact";

import FactCard from "../../components/fact/FactCard";
import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";
import ConfirmDialog from "../../components/common/ConfirmDialog";

/**
 * ============================================================================
 * FACT CONFLICT DETAIL PAGE
 * ============================================================================
 *
 * Shows one conflict (GET /api/facts/conflicts/{conflictId}) and the two
 * competing Facts it references (GET /api/facts/{factId}), so an applicant
 * can understand exactly what disagrees, on what evidence, and why - and,
 * when the viewer IS the subject of an OPEN conflict, lets them state which
 * value they believe is correct via
 * POST /api/facts/conflicts/{conflictId}/applicant-confirmation.
 *
 * DELIBERATE SCOPE LIMIT:
 *
 * This applicant confirmation is a DIFFERENT, narrower operation from the
 * case-worker-only PATCH /api/facts/conflicts/{conflictId}/resolve (gated
 * by FactAuthorizationService.assertCanVerifyOrResolve, which still
 * excludes the SUBJECT accessor type - "no self-verification, and a
 * subject may not adjudicate their own conflicting evidence"). Applicant
 * confirmation is gated by the separate
 * FactAuthorizationService.assertCanApplicantConfirmConflict grant, is
 * self-reported provenance only, and never sets a Fact's independent
 * verification overlay (isVerified). This page never renders the resulting
 * state as "Verified" or implies fraud from a conflict existing - see
 * FactCard and CONFLICT_RESOLUTION_LABEL for how that distinction is kept
 * visible after resolution.
 *
 * SECURITY:
 *
 * None of GET /api/facts/{factId}, GET /api/facts/conflicts/{conflictId},
 * or POST .../applicant-confirmation accepts a subjectUserId of any kind
 * from the caller - the backend resolves the subject from the fetched
 * record itself and authorizes against it. This page passes only the
 * conflictId already present in its own URL and a winningFactId that must
 * be one of the two fact IDs already returned on the fetched conflict
 * (factAId/factBId, chosen via a selection control, never free text) -
 * there is no field here a user could alter to reach or resolve another
 * subject's conflict. The confirmation control itself is only rendered
 * when the authenticated user's own ID matches the conflict's
 * subjectUserId; the backend re-enforces this independently regardless.
 * ============================================================================
 */

function getConflictErrorMessage(error: unknown): string {

  const status = errorService.getStatus(error);

  if (status === 404) {
    return "We could not find this conflict.";
  }

  if (status === 403) {
    return "You are not authorized to view this conflict.";
  }

  return errorService.getMessage(error);
}

export default function FactConflictDetailPage() {

  const { conflictId } = useParams<{ conflictId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [conflict, setConflict] = useState<FactConflict | null>(null);
  const [factA, setFactA] = useState<Fact | null>(null);
  const [factB, setFactB] = useState<Fact | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Applicant confirmation form state - entirely local to this page, never
  // a second source of truth for the Fact/FactConflict themselves.
  const [selectedFactId, setSelectedFactId] = useState<number | null>(null);
  const [confirmationNotes, setConfirmationNotes] = useState("");
  const [reviewDialogOpen, setReviewDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [justConfirmed, setJustConfirmed] = useState(false);

  const numericConflictId = conflictId ? Number(conflictId) : null;

  const loadConflict = useCallback(async () => {

    if (!numericConflictId || Number.isNaN(numericConflictId)) {
      setError("This conflict link is invalid.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const conflictResult = await getFactConflictApi(numericConflictId);

      const [factAResult, factBResult] = await Promise.all([
        getFactApi(conflictResult.factAId),
        getFactApi(conflictResult.factBId),
      ]);

      setConflict(conflictResult);
      setFactA(factAResult);
      setFactB(factBResult);

    } catch (err) {
      setError(getConflictErrorMessage(err));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conflictId]);

  useEffect(() => {
    void loadConflict();
  }, [loadConflict]);

  const handleConfirmSelection = useCallback(async () => {

    if (!numericConflictId || !selectedFactId) {
      return;
    }

    try {
      setSubmitting(true);
      setSubmitError(null);

      await confirmFactConflictApi(numericConflictId, {
        winningFactId: selectedFactId,
        notes: confirmationNotes.trim() ? confirmationNotes.trim() : undefined,
      });

      setReviewDialogOpen(false);
      setSelectedFactId(null);
      setConfirmationNotes("");
      setJustConfirmed(true);

      notificationService.success(
        "Your confirmation has been recorded. This is your own statement, not independent verification.",
      );

      // Re-read from the backend rather than trusting a locally-built
      // result - the Digital Twin/Profile and this page both stay single-
      // sourced from the server on every subsequent read.
      await loadConflict();

    } catch (err) {
      setSubmitError(errorService.getMessage(err));
      setReviewDialogOpen(false);
    } finally {
      setSubmitting(false);
    }
  }, [numericConflictId, selectedFactId, confirmationNotes, loadConflict]);

  const backToProfilePath = "/dashboard/immigration-profile";

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
        <Loader text="Loading this conflict..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-4 px-4 py-10 sm:px-6">
        <ErrorAlert message={error} />
        <div className="flex gap-3">
          <Button variant="secondary" onClick={() => void loadConflict()}>
            <RefreshCw size={16} />
            Try again
          </Button>
          <Link to={backToProfilePath}>
            <Button variant="outline">Back to profile</Button>
          </Link>
        </div>
      </div>
    );
  }

  if (!conflict || !factA || !factB) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
        <EmptyState
          icon={<ShieldQuestion size={40} />}
          title="Conflict not found"
          description="We could not find the details for this conflict."
          action={
            <Link to={backToProfilePath}>
              <Button variant="primary">Back to profile</Button>
            </Link>
          }
        />
      </div>
    );
  }

  const isResolved = conflict.status === "RESOLVED";

  // The confirmation control is only ever rendered for the conflict's own
  // subject - a case worker (or any other viewer assertCanView permits)
  // sees the same read-only evidence but never this action, matching the
  // backend's subject-only assertCanApplicantConfirmConflict grant.
  const isOwnConflict = user?.id != null && user.id === conflict.subjectUserId;

  const selectedFact =
    selectedFactId === factA.id ? factA : selectedFactId === factB.id ? factB : null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="mx-auto max-w-4xl px-4 py-10 sm:px-6"
    >
      <button
        type="button"
        onClick={() => navigate(backToProfilePath)}
        className="mb-6 inline-flex items-center gap-1 text-sm text-slate-400 transition hover:text-white"
      >
        <ArrowLeft size={16} />
        Back to your Immigration Profile
      </button>

      {justConfirmed && (
        <div className="mb-6 flex items-start gap-3 rounded-xl border border-emerald-400/20 bg-emerald-400/10 p-4">
          <CheckCircle2 size={18} className="mt-0.5 shrink-0 text-emerald-300" />
          <div>
            <p className="text-sm font-semibold text-emerald-300">Confirmation recorded</p>
            <p className="mt-1 text-sm text-emerald-200/80">
              You've confirmed which value you believe is correct. This is your own statement, not
              independent documentary verification and not a case worker's resolution.
            </p>
          </div>
        </div>
      )}

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
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-wide text-slate-500">
              Conflict #{conflict.id}
            </p>
            <h1 className="mt-1 text-2xl font-black text-white">
              Conflicting Information: {formatFactKeyLabel(conflict.factKey)}
            </h1>
          </div>

          <Badge variant={isResolved ? "success" : "warning"}>
            {isResolved ? "Resolved" : "Needs Review"}
          </Badge>
        </div>

        <div className="mt-4 flex items-start gap-2 rounded-lg border border-white/10 bg-black/20 p-3 text-sm text-slate-300">
          <ShieldQuestion size={16} className="mt-0.5 shrink-0 text-[#C6A15B]" />
          <p>
            We found two different, comparably-supported values for this information. This is a{" "}
            <span className="font-semibold text-white">conflict, not a finding of fraud</span> -
            both pieces of evidence are preserved below so the correct one can be confirmed.
          </p>
        </div>

        <p className="mt-3 text-xs text-slate-500">
          Detected {new Date(conflict.detectedAt).toLocaleString()}
        </p>
      </header>

      <section className="mt-8">
        <h2 className="mb-4 text-lg font-bold text-white">Competing Values</h2>

        <div className="grid gap-4 sm:grid-cols-2">
          {([
            { fact: factA, label: "Value A" },
            { fact: factB, label: "Value B" },
          ] as const).map(({ fact, label }) => {

            const isWinner = isResolved && conflict.winningFactId === fact.id;
            const isSelected = !isResolved && selectedFactId === fact.id;
            const canSelect = !isResolved && isOwnConflict;

            return (
              <div key={fact.id}>
                <div className="mb-2 flex items-center gap-2">
                  <span className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                    {label}
                  </span>
                  {isWinner && <Badge variant="success">Confirmed</Badge>}
                </div>

                <div
                  className={
                    isSelected
                      ? "rounded-xl ring-2 ring-[#C6A15B] ring-offset-2 ring-offset-[#0B1220]"
                      : undefined
                  }
                >
                  <FactCard fact={fact} highlight={isWinner ? "success" : undefined} />
                </div>

                {canSelect && (
                  <button
                    type="button"
                    onClick={() => setSelectedFactId(fact.id)}
                    className={`
                      mt-2 flex w-full items-center justify-center gap-2 rounded-lg border px-3 py-2
                      text-xs font-semibold transition
                      ${
                        isSelected
                          ? "border-[#C6A15B]/40 bg-[#C6A15B]/10 text-[#C6A15B]"
                          : "border-white/15 text-slate-300 hover:bg-white/10"
                      }
                    `}
                  >
                    <CheckCircle2 size={14} />
                    {isSelected ? "You believe this is correct" : "I believe this is correct"}
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </section>

      <section className="mt-8">
        {isResolved ? (
          <div className="flex items-start gap-3 rounded-xl border border-emerald-400/20 bg-emerald-400/10 p-5">
            <Gavel size={18} className="mt-0.5 shrink-0 text-emerald-300" />
            <div>
              <p className="text-sm font-semibold text-emerald-300">
                Resolved {conflict.resolvedAt && new Date(conflict.resolvedAt).toLocaleString()}
              </p>
              <p className="mt-1 text-sm text-emerald-200/80">
                {conflict.resolutionType
                  ? getConflictResolutionLabel(conflict.resolutionType)
                  : "This conflict has been resolved."}
              </p>
              {conflict.resolutionNotes && (
                <p className="mt-2 flex items-start gap-2 text-xs italic text-emerald-200/70">
                  <ScrollText size={14} className="mt-0.5 shrink-0" />
                  &ldquo;{conflict.resolutionNotes}&rdquo;
                </p>
              )}
              {conflict.resolutionType === "APPLICANT_CONFIRMATION" && (
                <p className="mt-3 flex items-start gap-2 rounded-lg border border-white/10 bg-black/20 p-2.5 text-xs text-emerald-200/70">
                  <BadgeCheck size={14} className="mt-0.5 shrink-0" />
                  You confirmed this value yourself. This is your own statement, not independent
                  documentary verification and not a case worker's resolution - it will still show
                  as &ldquo;Not independently verified&rdquo; above unless a case worker
                  separately verifies it.
                </p>
              )}
            </div>
          </div>
        ) : isOwnConflict ? (
          <div className="rounded-xl border border-amber-400/20 bg-amber-400/10 p-5">
            <div className="flex items-start gap-3">
              <ShieldQuestion size={18} className="mt-0.5 shrink-0 text-amber-300" />
              <div>
                <p className="text-sm font-semibold text-amber-300">
                  Confirm Which Value Is Correct
                </p>
                <p className="mt-1 text-sm text-amber-200/80">
                  You may state which of the two values above you believe is correct. This
                  records your own statement - it is{" "}
                  <span className="font-semibold text-white">
                    not independent documentary verification
                  </span>{" "}
                  and it is not the same as a case worker resolving this conflict. Either value
                  and its evidence remain on your case regardless of what you select.
                </p>
              </div>
            </div>

            <div className="mt-4">
              <label
                htmlFor="confirmation-notes"
                className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-amber-200/70"
              >
                Add a note (optional)
              </label>
              <textarea
                id="confirmation-notes"
                value={confirmationNotes}
                onChange={(event) => setConfirmationNotes(event.target.value.slice(0, 1000))}
                maxLength={1000}
                rows={3}
                placeholder="Anything that helps explain why this value is correct..."
                className="w-full rounded-lg border border-white/15 bg-black/20 p-3 text-sm text-white placeholder:text-slate-500 focus:border-[#C6A15B]/50 focus:outline-none"
              />
              <p className="mt-1 text-right text-[11px] text-amber-200/50">
                {confirmationNotes.length}/1000
              </p>
            </div>

            {submitError && (
              <div className="mt-3">
                <ErrorAlert message={submitError} />
              </div>
            )}

            <div className="mt-4">
              <Button
                variant="primary"
                disabled={!selectedFactId}
                onClick={() => setReviewDialogOpen(true)}
              >
                <CheckCircle2 size={16} />
                Review &amp; Confirm My Selection
              </Button>
              {!selectedFactId && (
                <p className="mt-2 text-xs text-amber-200/60">
                  Select Value A or Value B above to continue.
                </p>
              )}
            </div>
          </div>
        ) : (
          <div className="flex items-start gap-3 rounded-xl border border-amber-400/20 bg-amber-400/10 p-5">
            <ShieldQuestion size={18} className="mt-0.5 shrink-0 text-amber-300" />
            <div>
              <p className="text-sm font-semibold text-amber-300">Awaiting Review</p>
              <p className="mt-1 text-sm text-amber-200/80">
                This conflict is currently awaiting review by an authorized case worker assigned to
                this case, using the evidence and provenance information shown above.
              </p>
            </div>
          </div>
        )}
      </section>

      <div className="mt-8">
        <Link to={backToProfilePath}>
          <Button variant="secondary">
            <ArrowLeft size={16} />
            Back to your Immigration Profile
          </Button>
        </Link>
      </div>

      <ConfirmDialog
        open={reviewDialogOpen}
        title="Confirm Your Selection"
        description={
          selectedFact
            ? `You are about to state that "${formatFactValue(selectedFact)}" is the correct value for ${formatFactKeyLabel(
                conflict.factKey,
              )}. This records your own statement only - it does not independently verify this value, and it does not remove or hide the other value or its evidence from your case.`
            : "Select a value before continuing."
        }
        confirmText="Yes, Confirm This Value"
        cancelText="Cancel"
        variant="warning"
        loading={submitting}
        onConfirm={() => void handleConfirmSelection()}
        onCancel={() => setReviewDialogOpen(false)}
      />
    </motion.div>
  );
}
