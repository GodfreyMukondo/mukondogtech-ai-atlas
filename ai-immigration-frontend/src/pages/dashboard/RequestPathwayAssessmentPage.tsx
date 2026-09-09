import {
  useCallback,
  useEffect,
  useState,
} from "react";

import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { motion } from "framer-motion";

import {
  Compass,
  IdCard,
  Send,
  ShieldCheck,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";

import {
  listPathwaysApi,
  requestPathwayAssessmentApi,
} from "../../api/pathwayApi";

import { notificationService } from "../../services/notificationService";
import { getAssessmentErrorMessage } from "../../utils/assessmentErrors";

import type { Pathway } from "../../types/pathwayAssessment";

import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";

/**
 * ============================================================================
 * REQUEST PATHWAY ASSESSMENT PAGE
 * ============================================================================
 *
 * Lets the authenticated user request a personalized assessment of one
 * immigration pathway (POST /api/pathways/{pathwayId}/assessments) and
 * lands on the results page once it has been computed.
 *
 * SECURITY:
 *
 * The subject of the assessment is always the authenticated user's own
 * ID, resolved from AuthContext - it is never a free-text or otherwise
 * editable field. The backend independently re-verifies this is an
 * authorized subject for the caller before computing anything.
 * ============================================================================
 */
export default function RequestPathwayAssessmentPage() {

  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();

  // Pre-selects the pathway the user arrived with from Pathway Discovery
  // (?pathwayId=...) - purely a starting value, the dropdown below still
  // lets them change it before requesting the formal assessment.
  const preselectedPathwayId = Number(searchParams.get("pathwayId")) || null;

  const [pathways, setPathways] = useState<Pathway[]>([]);
  const [selectedPathwayId, setSelectedPathwayId] = useState<number | null>(preselectedPathwayId);

  const [loadingPathways, setLoadingPathways] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const loadPathways = useCallback(async () => {

    try {
      setLoadingPathways(true);
      setLoadError(null);

      const result = await listPathwaysApi();

      setPathways(result);

      setSelectedPathwayId((current) => current ?? result[0]?.id ?? null);

    } catch (error) {
      setLoadError(getAssessmentErrorMessage(error));
    } finally {
      setLoadingPathways(false);
    }
  }, []);

  useEffect(() => {
    void loadPathways();
  }, [loadPathways]);

  const selectedPathway = pathways.find((pathway) => pathway.id === selectedPathwayId) ?? null;

  const handleSubmit = useCallback(async () => {

    if (!selectedPathwayId) {
      return;
    }

    if (!user?.id) {
      setSubmitError("Your account could not be identified. Please sign in again.");
      return;
    }

    try {
      setSubmitting(true);
      setSubmitError(null);

      const assessment = await requestPathwayAssessmentApi(selectedPathwayId, user.id);

      notificationService.success("Your pathway assessment is ready.");

      navigate(`/dashboard/pathways/assessments/${assessment.id}`);

    } catch (error) {
      const message = getAssessmentErrorMessage(error);
      setSubmitError(message);
      notificationService.error(message);
    } finally {
      setSubmitting(false);
    }
  }, [navigate, selectedPathwayId, user?.id]);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="w-full px-4 py-10 sm:px-6 lg:px-8"
    >
      <header className="mb-8">
        <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-[#C6A15B]/20 bg-[#C6A15B]/10 px-3 py-1 text-xs font-semibold text-[#C6A15B]">
          <Compass size={14} />
          Immigration Pathways
        </div>

        <h1 className="text-2xl font-black text-white sm:text-3xl">
          Request a Pathway Assessment
        </h1>

        <p className="mt-2 max-w-2xl text-sm text-slate-400">
          MukondoGTech AI will evaluate this pathway's requirements against your
          current, authorized case information and show you exactly where you
          stand on each one.
        </p>

        <Link
          to="/dashboard/immigration-profile"
          className="mt-3 inline-flex items-center gap-1.5 text-sm text-[#C6A15B] transition hover:text-[#A8894D]"
        >
          <IdCard size={14} />
          Review your Immigration Profile first
        </Link>
      </header>

      <section
        className="
          max-w-2xl
          rounded-2xl
          border
          border-white/10
          bg-white/5
          p-6
          shadow-sm
        "
      >
        {loadingPathways && (
          <div className="py-12">
            <Loader text="Loading available pathways..." />
          </div>
        )}

        {!loadingPathways && loadError && (
          <div className="space-y-4">
            <ErrorAlert message={loadError} />
            <Button variant="secondary" onClick={() => void loadPathways()}>
              Try again
            </Button>
          </div>
        )}

        {!loadingPathways && !loadError && pathways.length === 0 && (
          <EmptyState
            icon={<Compass size={40} />}
            title="No pathways available yet"
            description="There are currently no published immigration pathways to assess. Please check back later."
          />
        )}

        {!loadingPathways && !loadError && pathways.length > 0 && (
          <div className="space-y-6">
            <div>
              <label
                htmlFor="pathway-select"
                className="mb-2 block text-sm font-semibold text-white"
              >
                Choose a pathway
              </label>

              <select
                id="pathway-select"
                value={selectedPathwayId ?? ""}
                onChange={(event) => setSelectedPathwayId(Number(event.target.value))}
                className="
                  w-full
                  rounded-xl
                  border
                  border-white/15
                  bg-white/10
                  px-4
                  py-3
                  text-white
                  outline-none
                  focus:border-[#C6A15B]/60
                "
              >
                {pathways.map((pathway) => (
                  <option key={pathway.id} value={pathway.id} className="bg-[#0B0F1A]">
                    {pathway.name} · {pathway.jurisdiction}
                  </option>
                ))}
              </select>
            </div>

            {selectedPathway && (
              <div className="rounded-xl border border-white/10 bg-black/20 p-4">
                <p className="text-sm font-semibold text-white">{selectedPathway.name}</p>

                {selectedPathway.description && (
                  <p className="mt-1 text-sm text-slate-400">
                    {selectedPathway.description}
                  </p>
                )}

                <p className="mt-2 text-xs uppercase tracking-wide text-slate-500">
                  {selectedPathway.category} · {selectedPathway.jurisdiction}
                </p>
              </div>
            )}

            <div className="flex items-start gap-2 rounded-xl border border-white/10 bg-black/20 p-3 text-xs text-slate-400">
              <ShieldCheck size={16} className="mt-0.5 shrink-0 text-[#C6A15B]" />
              <p>
                This assessment only uses information already on your case and is
                subject to the same access controls as the rest of your account.
              </p>
            </div>

            {submitError && <ErrorAlert message={submitError} />}

            <Button
              variant="primary"
              size="lg"
              loading={submitting}
              disabled={!selectedPathwayId}
              onClick={() => void handleSubmit()}
              className="w-full sm:w-auto"
            >
              <Send size={18} />
              Request Assessment
            </Button>
          </div>
        )}
      </section>
    </motion.div>
  );
}
