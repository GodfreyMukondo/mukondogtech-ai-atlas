import { useCallback, useEffect, useMemo, useState } from "react";

import { motion } from "framer-motion";

import {
  Archive,
  Ban,
  Compass,
  Eye,
  Pencil,
  Plus,
  RefreshCw,
  Send,
  Undo2,
} from "lucide-react";

import {
  changePathwayStatusApi,
  createPathwayApi,
  getAdminPathwayApi,
  listAdminPathwaysApi,
  listAdminRequirementsApi,
  updatePathwayApi,
} from "../../api/pathwayAdminApi";
import { errorService } from "../../services/errorService";
import { notificationService } from "../../services/notificationService";

import type { Pathway, PathwayStatus } from "../../types/pathwayAssessment";
import type { AdminRequirement, PathwayCreateRequest } from "../../types/pathwayAdmin";

import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";
import Modal from "../../components/common/Modal";
import ConfirmDialog from "../../components/common/ConfirmDialog";

/**
 * ============================================================================
 * PATHWAY MANAGEMENT PAGE (ADMIN)
 * ============================================================================
 *
 * Administrator authoring surface for the Pathway catalogue
 * (Phase 1 spec, sections 1, 3, 5). Reads/writes the real
 * /api/admin/pathways backend - nothing here is hard-coded; the
 * applicant-facing "No pathways available yet" state on
 * RequestPathwayAssessmentPage disappears once a pathway created and
 * published here exists.
 *
 * A PUBLISHED pathway's content is immutable - editing is only available
 * for DRAFT/REVIEW rows, matching PathwayAdminService's own guard.
 * Publishing/archiving always goes through a confirmation dialog.
 * ============================================================================
 */

const STATUS_BADGE_VARIANT: Record<PathwayStatus, "neutral" | "warning" | "success" | "danger"> = {
  DRAFT: "neutral",
  REVIEW: "warning",
  PUBLISHED: "success",
  SUPERSEDED: "neutral",
  ARCHIVED: "danger",
};

const INPUT_CLASS =
  "w-full rounded-xl border border-white/15 bg-white/10 px-4 py-3 text-sm text-white outline-none focus:border-[#C6A15B]/60";

interface PathwayFormState {
  pathwayKey: string;
  name: string;
  description: string;
  jurisdiction: string;
  category: string;
  requirementIds: number[];
  evidenceExpectations: string;
}

const emptyForm: PathwayFormState = {
  pathwayKey: "",
  name: "",
  description: "",
  jurisdiction: "",
  category: "",
  requirementIds: [],
  evidenceExpectations: "",
};

export default function PathwayManagementPage() {

  const [pathways, setPathways] = useState<Pathway[]>([]);
  const [requirements, setRequirements] = useState<AdminRequirement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<PathwayFormState>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [pendingTransition, setPendingTransition] = useState<{ pathway: Pathway; target: PathwayStatus } | null>(null);
  const [transitioning, setTransitioning] = useState(false);

  const load = useCallback(async () => {

    try {
      setLoading(true);
      setError(null);

      const [pathwayResult, requirementResult] = await Promise.all([
        listAdminPathwaysApi(),
        listAdminRequirementsApi(),
      ]);

      setPathways(pathwayResult);
      setRequirements(requirementResult);

    } catch (err) {
      setError(errorService.getMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const publishableRequirements = useMemo(
    () => requirements.filter((requirement) => requirement.status === "PUBLISHED"),
    [requirements],
  );

  function openCreate() {
    setForm(emptyForm);
    setFormError(null);
    setModalMode("create");
    setEditingId(null);
  }

  async function openEdit(pathway: Pathway) {

    try {

      // requirementIds is derived server-side from the pathway's own
      // compositionLogic (never hand-merged or re-selected from scratch) -
      // see PathwayAdminService.getById.
      const detail = await getAdminPathwayApi(pathway.id);

      setForm({
        pathwayKey: detail.pathwayKey,
        name: detail.name,
        description: detail.description ?? "",
        jurisdiction: detail.jurisdiction,
        category: detail.category,
        requirementIds: detail.requirementIds,
        evidenceExpectations: "",
      });

      setFormError(null);
      setModalMode("edit");
      setEditingId(detail.id);

    } catch (err) {
      notificationService.error(errorService.getMessage(err));
    }
  }

  function toggleRequirement(id: number) {

    setForm((current) => ({
      ...current,
      requirementIds: current.requirementIds.includes(id)
        ? current.requirementIds.filter((existing) => existing !== id)
        : [...current.requirementIds, id],
    }));
  }

  async function handleSubmit() {

    if (form.requirementIds.length === 0) {
      setFormError("Select at least one requirement.");
      return;
    }

    setSaving(true);
    setFormError(null);

    try {

      if (modalMode === "create") {

        const request: PathwayCreateRequest = {
          pathwayKey: form.pathwayKey.trim(),
          name: form.name.trim(),
          description: form.description.trim() || undefined,
          jurisdiction: form.jurisdiction.trim(),
          category: form.category.trim(),
          requirementIds: form.requirementIds,
          evidenceExpectations: form.evidenceExpectations.trim() || undefined,
        };

        await createPathwayApi(request);
        notificationService.success("Pathway created as a draft.");

      } else if (modalMode === "edit" && editingId != null) {

        await updatePathwayApi(editingId, {
          name: form.name.trim(),
          description: form.description.trim() || undefined,
          jurisdiction: form.jurisdiction.trim(),
          category: form.category.trim(),
          requirementIds: form.requirementIds,
          evidenceExpectations: form.evidenceExpectations.trim() || undefined,
        });

        notificationService.success("Pathway updated.");
      }

      setModalMode(null);
      await load();

    } catch (err) {
      setFormError(errorService.getMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function confirmTransition() {

    if (!pendingTransition) {
      return;
    }

    setTransitioning(true);

    try {

      await changePathwayStatusApi(pendingTransition.pathway.id, { targetStatus: pendingTransition.target });

      notificationService.success(`Pathway moved to ${pendingTransition.target}.`);
      setPendingTransition(null);
      await load();

    } catch (err) {
      notificationService.error(errorService.getMessage(err));
    } finally {
      setTransitioning(false);
    }
  }

  const availableTransitions: Record<PathwayStatus, { target: PathwayStatus; label: string; icon: typeof Send }[]> = {
    DRAFT: [
      { target: "REVIEW", label: "Submit for Review", icon: Eye },
      { target: "ARCHIVED", label: "Archive", icon: Archive },
    ],
    REVIEW: [
      { target: "DRAFT", label: "Back to Draft", icon: Undo2 },
      { target: "PUBLISHED", label: "Publish", icon: Send },
      { target: "ARCHIVED", label: "Archive", icon: Archive },
    ],
    PUBLISHED: [{ target: "ARCHIVED", label: "Archive", icon: Archive }],
    SUPERSEDED: [],
    ARCHIVED: [],
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="w-full px-4 py-6 sm:px-6 lg:px-8"
    >
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-black text-white">
            <Compass className="text-[#C6A15B]" size={26} />
            Pathway Catalogue
          </h1>
          <p className="mt-1 max-w-2xl text-sm text-slate-400">
            Create, review, and publish immigration pathways. Only PUBLISHED pathways are visible
            to applicants requesting an assessment.
          </p>
        </div>

        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => void load()}>
            <RefreshCw size={16} />
            Refresh
          </Button>
          <Button variant="primary" onClick={openCreate}>
            <Plus size={16} />
            New Pathway
          </Button>
        </div>
      </div>

      {loading && <Loader text="Loading pathways..." />}

      {!loading && error && <ErrorAlert message={error} />}

      {!loading && !error && pathways.length === 0 && (
        <EmptyState
          icon={<Compass size={40} />}
          title="No pathways yet"
          description="Create the first immigration pathway to make it available for assessment."
          action={
            <Button variant="primary" onClick={openCreate}>
              <Plus size={16} />
              New Pathway
            </Button>
          }
        />
      )}

      {!loading && !error && pathways.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-white/10 bg-white/5">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="bg-white/5 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Pathway</th>
                  <th className="px-4 py-3">Jurisdiction</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pathways.map((pathway) => (
                  <tr key={pathway.id} className="border-t border-white/10">
                    <td className="px-4 py-3">
                      <p className="font-semibold text-white">{pathway.name}</p>
                      <p className="text-xs text-slate-500">{pathway.pathwayKey}</p>
                    </td>
                    <td className="px-4 py-3 text-slate-300">{pathway.jurisdiction}</td>
                    <td className="px-4 py-3 text-slate-300">{pathway.category}</td>
                    <td className="px-4 py-3">
                      <Badge variant={STATUS_BADGE_VARIANT[pathway.status]}>{pathway.status}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap items-center gap-2">
                        {(pathway.status === "DRAFT" || pathway.status === "REVIEW") && (
                          <button
                            type="button"
                            onClick={() => void openEdit(pathway)}
                            title="Edit"
                            className="rounded-lg border border-white/15 p-1.5 text-slate-300 transition hover:border-white/30 hover:text-white"
                          >
                            <Pencil size={14} />
                          </button>
                        )}
                        {availableTransitions[pathway.status].map(({ target, label, icon: Icon }) => (
                          <button
                            key={target}
                            type="button"
                            onClick={() => setPendingTransition({ pathway, target })}
                            title={label}
                            className="inline-flex items-center gap-1 rounded-lg border border-white/15 px-2 py-1 text-xs text-slate-300 transition hover:border-[#C6A15B]/50 hover:text-[#C6A15B]"
                          >
                            <Icon size={13} />
                            {label}
                          </button>
                        ))}
                        {availableTransitions[pathway.status].length === 0 && (
                          <span className="text-xs text-slate-600">
                            <Ban size={13} className="inline" /> No further transitions
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal
        open={modalMode !== null}
        onClose={() => (saving ? undefined : setModalMode(null))}
        title={modalMode === "create" ? "New Pathway" : "Edit Pathway"}
      >
        <div className="space-y-4">
          {modalMode === "create" && (
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-400">Pathway Key</label>
              <input
                value={form.pathwayKey}
                onChange={(event) => setForm({ ...form, pathwayKey: event.target.value })}
                placeholder="e.g. CA_EXPRESS_ENTRY_FSWP"
                className={INPUT_CLASS}
              />
            </div>
          )}

          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-400">Name</label>
            <input
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              className={INPUT_CLASS}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-400">Jurisdiction</label>
              <input
                value={form.jurisdiction}
                onChange={(event) => setForm({ ...form, jurisdiction: event.target.value })}
                className={INPUT_CLASS}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-400">Category</label>
              <input
                value={form.category}
                onChange={(event) => setForm({ ...form, category: event.target.value })}
                className={INPUT_CLASS}
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-400">Description</label>
            <textarea
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              rows={3}
              className={INPUT_CLASS}
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-400">Evidence Expectations</label>
            <textarea
              value={form.evidenceExpectations}
              onChange={(event) => setForm({ ...form, evidenceExpectations: event.target.value })}
              rows={2}
              placeholder="Descriptive guidance only - not evaluated by the requirement engine."
              className={INPUT_CLASS}
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-400">
              Requirements (all selected must be satisfied)
            </label>
            {publishableRequirements.length === 0 ? (
              <p className="text-xs text-amber-300">
                No PUBLISHED requirements exist yet - publish requirements first.
              </p>
            ) : (
              <div className="max-h-48 space-y-1.5 overflow-y-auto rounded-xl border border-white/10 p-2">
                {publishableRequirements.map((requirement) => (
                  <label
                    key={requirement.id}
                    className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-slate-200 hover:bg-white/5"
                  >
                    <input
                      type="checkbox"
                      checked={form.requirementIds.includes(requirement.id)}
                      onChange={() => toggleRequirement(requirement.id)}
                    />
                    {requirement.title}
                    <span className="text-xs text-slate-500">({requirement.requirementKey})</span>
                  </label>
                ))}
              </div>
            )}
          </div>

          {formError && <ErrorAlert message={formError} />}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setModalMode(null)} disabled={saving}>
              Cancel
            </Button>
            <Button variant="primary" onClick={() => void handleSubmit()} loading={saving}>
              {modalMode === "create" ? "Create Pathway" : "Save Changes"}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={pendingTransition !== null}
        title={pendingTransition ? `${pendingTransition.target} this pathway?` : ""}
        description={
          pendingTransition
            ? `"${pendingTransition.pathway.name}" will move from ${pendingTransition.pathway.status} to ${pendingTransition.target}.` +
              (pendingTransition.target === "PUBLISHED"
                ? " Any previously published version of this pathway will be automatically superseded."
                : "")
            : ""
        }
        confirmText={pendingTransition?.target === "ARCHIVED" ? "Archive" : "Confirm"}
        variant={pendingTransition?.target === "ARCHIVED" ? "danger" : "warning"}
        loading={transitioning}
        onConfirm={() => void confirmTransition()}
        onCancel={() => setPendingTransition(null)}
      />
    </motion.div>
  );
}
