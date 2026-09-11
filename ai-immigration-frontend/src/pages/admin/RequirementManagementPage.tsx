import { useCallback, useEffect, useState } from "react";

import { motion } from "framer-motion";

import {
  Archive,
  Ban,
  Pencil,
  Plus,
  RefreshCw,
  ScrollText,
  Send,
  Trash2,
  X,
} from "lucide-react";

import {
  changeRequirementStatusApi,
  createRequirementApi,
  getAdminRequirementApi,
  listAdminRequirementsApi,
  listKnownFactTypesApi,
  listRegulatoryVersionsApi,
  updateRequirementApi,
} from "../../api/pathwayAdminApi";
import { errorService } from "../../services/errorService";
import { notificationService } from "../../services/notificationService";

import type {
  AdminRequirement,
  FactTypeSummary,
  RegulatoryVersion,
  RequirementCreateRequest,
  RequirementFactBindingInput,
  RequirementStatus,
  RequirementType,
} from "../../types/pathwayAdmin";

import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";
import Loader from "../../components/common/Loader";
import ErrorAlert from "../../components/common/ErrorAlert";
import EmptyState from "../../components/common/EmptyState";
import Modal from "../../components/common/Modal";
import ConfirmDialog from "../../components/common/ConfirmDialog";

/**
 * ============================================================================
 * REQUIREMENT MANAGEMENT PAGE (ADMIN)
 * ============================================================================
 *
 * Administrator authoring surface for Requirement definitions
 * (Phase 1 spec, section 2). Reads/writes the real /api/admin/requirements
 * backend.
 *
 * LOGIC BUILDER: covers the common, single-predicate cases (fact exists /
 * equals / at-least, and "at least N years of a historical fact") by
 * generating the exact LogicNode JSON the backend already expects - never
 * a second logic format. An "Advanced (raw JSON)" mode is the escape hatch
 * for anything the simple builder doesn't cover, editing the same JSON
 * text the simple modes produce.
 * ============================================================================
 */

const STATUS_BADGE_VARIANT: Record<RequirementStatus, "neutral" | "success" | "warning" | "danger"> = {
  DRAFT: "neutral",
  PUBLISHED: "success",
  DEPRECATED: "warning",
  RETRACTED: "danger",
};

const REQUIREMENT_TYPES: RequirementType[] = [
  "ELIGIBILITY_ATTRIBUTE",
  "CREDENTIAL",
  "EXPERIENCE",
  "CAPACITY",
  "PROFICIENCY",
  "RELATIONSHIP",
  "PROCEDURAL",
  "LEGAL_STANDING",
  "COMPOSITE",
];

type LogicMode = "EXISTS" | "EQUALS" | "AT_LEAST" | "DURATION_YEARS" | "ADVANCED";

const INPUT_CLASS =
  "w-full rounded-xl border border-white/15 bg-white/10 px-4 py-3 text-sm text-white outline-none focus:border-[#C6A15B]/60";

interface FormState {
  requirementKey: string;
  requirementType: RequirementType;
  title: string;
  description: string;
  jurisdiction: string;
  immigrationContext: string;
  regulatoryVersionId: number | null;
  mandatory: boolean;
  bindings: RequirementFactBindingInput[];
  logicMode: LogicMode;
  logicFactKey: string;
  logicOperandValue: string;
  advancedJson: string;
}

function emptyForm(): FormState {
  return {
    requirementKey: "",
    requirementType: "ELIGIBILITY_ATTRIBUTE",
    title: "",
    description: "",
    jurisdiction: "",
    immigrationContext: "",
    regulatoryVersionId: null,
    mandatory: true,
    bindings: [{ factKey: "", requiresVerification: false }],
    logicMode: "EXISTS",
    logicFactKey: "",
    logicOperandValue: "",
    advancedJson: "",
  };
}

function buildSatisfactionLogicJson(form: FormState): string {

  if (form.logicMode === "ADVANCED") {
    return form.advancedJson;
  }

  const factKey = form.logicFactKey || form.bindings[0]?.factKey || "";

  switch (form.logicMode) {
    case "EXISTS":
      return JSON.stringify({
        node: "FACT_PREDICATE",
        factKey,
        operator: "EXISTS",
        operandValue: null,
        operandValues: null,
        operandLow: null,
        operandHigh: null,
      });

    case "EQUALS":
      return JSON.stringify({
        node: "FACT_PREDICATE",
        factKey,
        operator: "EQUALS",
        operandValue: form.logicOperandValue,
        operandValues: null,
        operandLow: null,
        operandHigh: null,
      });

    case "AT_LEAST":
      return JSON.stringify({
        node: "FACT_PREDICATE",
        factKey,
        operator: "AT_LEAST",
        operandValue: form.logicOperandValue,
        operandValues: null,
        operandLow: null,
        operandHigh: null,
      });

    case "DURATION_YEARS":
      return JSON.stringify({
        node: "DERIVED_PREDICATE",
        function: "DURATION_BETWEEN",
        functionArgs: [factKey],
        operator: "AT_LEAST",
        operandValue: form.logicOperandValue,
        operandLow: null,
        operandHigh: null,
      });

    default:
      return "";
  }
}

export default function RequirementManagementPage() {

  const [requirements, setRequirements] = useState<AdminRequirement[]>([]);
  const [regulatoryVersions, setRegulatoryVersions] = useState<RegulatoryVersion[]>([]);
  const [factTypes, setFactTypes] = useState<FactTypeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [pendingTransition, setPendingTransition] = useState<{
    requirement: AdminRequirement;
    target: RequirementStatus;
  } | null>(null);
  const [transitioning, setTransitioning] = useState(false);

  const load = useCallback(async () => {

    try {
      setLoading(true);
      setError(null);

      const [requirementResult, versionResult, factTypeResult] = await Promise.all([
        listAdminRequirementsApi(),
        listRegulatoryVersionsApi(),
        listKnownFactTypesApi(),
      ]);

      setRequirements(requirementResult);
      setRegulatoryVersions(versionResult);
      setFactTypes(factTypeResult);

    } catch (err) {
      setError(errorService.getMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  function openCreate() {
    setForm(emptyForm());
    setFormError(null);
    setModalMode("create");
    setEditingId(null);
  }

  async function openEdit(requirement: AdminRequirement) {

    try {

      const detail = await getAdminRequirementApi(requirement.id);

      setForm({
        requirementKey: detail.requirementKey,
        requirementType: detail.requirementType,
        title: detail.title,
        description: detail.description ?? "",
        jurisdiction: detail.jurisdiction,
        immigrationContext: detail.immigrationContext ?? "",
        regulatoryVersionId: detail.regulatoryVersionId,
        mandatory: detail.mandatory,
        bindings: detail.factBindings.length > 0 ? detail.factBindings : [{ factKey: "", requiresVerification: false }],
        logicMode: "ADVANCED",
        logicFactKey: detail.factBindings[0]?.factKey ?? "",
        logicOperandValue: "",
        advancedJson: detail.satisfactionLogicJson,
      });

      setFormError(null);
      setModalMode("edit");
      setEditingId(requirement.id);

    } catch (err) {
      notificationService.error(errorService.getMessage(err));
    }
  }

  function updateBinding(index: number, patch: Partial<RequirementFactBindingInput>) {

    setForm((current) => ({
      ...current,
      bindings: current.bindings.map((binding, i) => (i === index ? { ...binding, ...patch } : binding)),
    }));
  }

  function addBinding() {
    setForm((current) => ({
      ...current,
      bindings: [...current.bindings, { factKey: "", requiresVerification: false }],
    }));
  }

  function removeBinding(index: number) {
    setForm((current) => ({
      ...current,
      bindings: current.bindings.filter((_, i) => i !== index),
    }));
  }

  async function handleSubmit() {

    if (!form.regulatoryVersionId) {
      setFormError("Select a regulatory version.");
      return;
    }

    const satisfactionLogicJson = buildSatisfactionLogicJson(form);

    if (!satisfactionLogicJson.trim()) {
      setFormError("Satisfaction logic could not be built - check the logic builder fields.");
      return;
    }

    setSaving(true);
    setFormError(null);

    try {

      const payload = {
        requirementType: form.requirementType,
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        jurisdiction: form.jurisdiction.trim(),
        immigrationContext: form.immigrationContext.trim() || undefined,
        regulatoryVersionId: form.regulatoryVersionId,
        mandatory: form.mandatory,
        satisfactionLogicJson,
        factBindings: form.bindings.filter((binding) => binding.factKey),
      };

      if (modalMode === "create") {

        const createPayload: RequirementCreateRequest = { ...payload, requirementKey: form.requirementKey.trim() };
        await createRequirementApi(createPayload);
        notificationService.success("Requirement created as a draft.");

      } else if (modalMode === "edit" && editingId != null) {

        await updateRequirementApi(editingId, payload);
        notificationService.success("Requirement updated.");
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

      await changeRequirementStatusApi(pendingTransition.requirement.id, { targetStatus: pendingTransition.target });
      notificationService.success(`Requirement moved to ${pendingTransition.target}.`);
      setPendingTransition(null);
      await load();

    } catch (err) {
      notificationService.error(errorService.getMessage(err));
    } finally {
      setTransitioning(false);
    }
  }

  const availableTransitions: Record<RequirementStatus, { target: RequirementStatus; label: string; icon: typeof Send }[]> = {
    DRAFT: [
      { target: "PUBLISHED", label: "Publish", icon: Send },
      { target: "RETRACTED", label: "Retract", icon: Trash2 },
    ],
    PUBLISHED: [
      { target: "DEPRECATED", label: "Deprecate", icon: Archive },
      { target: "RETRACTED", label: "Retract", icon: Trash2 },
    ],
    DEPRECATED: [],
    RETRACTED: [],
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
            <ScrollText className="text-[#C6A15B]" size={26} />
            Requirement Definitions
          </h1>
          <p className="mt-1 max-w-2xl text-sm text-slate-400">
            Author reusable, regulatory-sourced requirements. Only PUBLISHED requirements can be
            composed into a pathway.
          </p>
        </div>

        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => void load()}>
            <RefreshCw size={16} />
            Refresh
          </Button>
          <Button
            variant="primary"
            onClick={openCreate}
            disabled={regulatoryVersions.length === 0}
            title={regulatoryVersions.length === 0 ? "Record a regulatory version first" : undefined}
          >
            <Plus size={16} />
            New Requirement
          </Button>
        </div>
      </div>

      {loading && <Loader text="Loading requirements..." />}

      {!loading && error && <ErrorAlert message={error} />}

      {!loading && !error && regulatoryVersions.length === 0 && (
        <div className="mb-4 rounded-xl border border-amber-400/20 bg-amber-400/10 p-3 text-sm text-amber-200">
          No regulatory versions are on file yet. A Requirement cannot exist without a traceable
          regulatory source - record one via <code>POST /api/admin/regulatory-versions</code> first.
        </div>
      )}

      {!loading && !error && requirements.length === 0 && (
        <EmptyState
          icon={<ScrollText size={40} />}
          title="No requirements yet"
          description="Create the first requirement definition."
        />
      )}

      {!loading && !error && requirements.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-white/10 bg-white/5">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[900px] text-left text-sm">
              <thead className="bg-white/5 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Requirement</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Jurisdiction</th>
                  <th className="px-4 py-3">Mandatory</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {requirements.map((requirement) => (
                  <tr key={requirement.id} className="border-t border-white/10">
                    <td className="px-4 py-3">
                      <p className="font-semibold text-white">{requirement.title}</p>
                      <p className="text-xs text-slate-500">{requirement.requirementKey}</p>
                    </td>
                    <td className="px-4 py-3 text-slate-300">{requirement.requirementType}</td>
                    <td className="px-4 py-3 text-slate-300">{requirement.jurisdiction}</td>
                    <td className="px-4 py-3 text-slate-300">{requirement.mandatory ? "Yes" : "No"}</td>
                    <td className="px-4 py-3">
                      <Badge variant={STATUS_BADGE_VARIANT[requirement.status]}>{requirement.status}</Badge>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap items-center gap-2">
                        {requirement.status === "DRAFT" && (
                          <button
                            type="button"
                            onClick={() => void openEdit(requirement)}
                            title="Edit"
                            className="rounded-lg border border-white/15 p-1.5 text-slate-300 transition hover:border-white/30 hover:text-white"
                          >
                            <Pencil size={14} />
                          </button>
                        )}
                        {availableTransitions[requirement.status].map(({ target, label, icon: Icon }) => (
                          <button
                            key={target}
                            type="button"
                            onClick={() => setPendingTransition({ requirement, target })}
                            title={label}
                            className="inline-flex items-center gap-1 rounded-lg border border-white/15 px-2 py-1 text-xs text-slate-300 transition hover:border-[#C6A15B]/50 hover:text-[#C6A15B]"
                          >
                            <Icon size={13} />
                            {label}
                          </button>
                        ))}
                        {availableTransitions[requirement.status].length === 0 && (
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
        title={modalMode === "create" ? "New Requirement" : "Edit Requirement"}
      >
        <div className="max-h-[70vh] space-y-4 overflow-y-auto pr-1">
          {modalMode === "create" && (
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-400">Requirement Key</label>
              <input
                value={form.requirementKey}
                onChange={(event) => setForm({ ...form, requirementKey: event.target.value })}
                placeholder="e.g. CA_FSWP.LANGUAGE_ABILITY"
                className={INPUT_CLASS}
              />
            </div>
          )}

          <div>
            <label className="mb-1 block text-xs font-semibold text-slate-400">Title</label>
            <input
              value={form.title}
              onChange={(event) => setForm({ ...form, title: event.target.value })}
              className={INPUT_CLASS}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-400">Type</label>
              <select
                value={form.requirementType}
                onChange={(event) => setForm({ ...form, requirementType: event.target.value as RequirementType })}
                className={INPUT_CLASS}
              >
                {REQUIREMENT_TYPES.map((type) => (
                  <option key={type} value={type} className="bg-[#0B0F1A]">
                    {type}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold text-slate-400">Jurisdiction</label>
              <input
                value={form.jurisdiction}
                onChange={(event) => setForm({ ...form, jurisdiction: event.target.value })}
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
            <label className="mb-1 block text-xs font-semibold text-slate-400">Regulatory Version</label>
            <select
              value={form.regulatoryVersionId ?? ""}
              onChange={(event) => setForm({ ...form, regulatoryVersionId: Number(event.target.value) })}
              className={INPUT_CLASS}
            >
              <option value="" className="bg-[#0B0F1A]">
                Select a source...
              </option>
              {regulatoryVersions.map((version) => (
                <option key={version.id} value={version.id} className="bg-[#0B0F1A]">
                  {version.sourceAuthority} — {version.regulationIdentity}
                </option>
              ))}
            </select>
          </div>

          <label className="flex items-center gap-2 text-sm text-slate-200">
            <input
              type="checkbox"
              checked={form.mandatory}
              onChange={(event) => setForm({ ...form, mandatory: event.target.checked })}
            />
            Mandatory requirement
          </label>

          <div className="rounded-xl border border-white/10 p-3">
            <label className="mb-1 block text-xs font-semibold text-slate-400">Fact Bindings</label>
            {form.bindings.map((binding, index) => (
              <div key={index} className="mb-2 flex items-center gap-2">
                <select
                  value={binding.factKey}
                  onChange={(event) => updateBinding(index, { factKey: event.target.value })}
                  className={INPUT_CLASS}
                >
                  <option value="" className="bg-[#0B0F1A]">
                    Select a fact key...
                  </option>
                  {factTypes.map((factType) => (
                    <option key={factType.factKey} value={factType.factKey} className="bg-[#0B0F1A]">
                      {factType.factKey}
                    </option>
                  ))}
                </select>
                <label className="flex shrink-0 items-center gap-1 text-xs text-slate-300">
                  <input
                    type="checkbox"
                    checked={binding.requiresVerification}
                    onChange={(event) => updateBinding(index, { requiresVerification: event.target.checked })}
                  />
                  Requires verification
                </label>
                {form.bindings.length > 1 && (
                  <button type="button" onClick={() => removeBinding(index)} className="text-slate-500 hover:text-red-300">
                    <X size={16} />
                  </button>
                )}
              </div>
            ))}
            <button type="button" onClick={addBinding} className="text-xs font-semibold text-[#C6A15B] hover:text-[#dbb877]">
              + Add another fact binding
            </button>
          </div>

          <div className="rounded-xl border border-white/10 p-3">
            <label className="mb-1 block text-xs font-semibold text-slate-400">Satisfaction Logic</label>
            <select
              value={form.logicMode}
              onChange={(event) => setForm({ ...form, logicMode: event.target.value as LogicMode })}
              className={`${INPUT_CLASS} mb-2`}
            >
              <option value="EXISTS" className="bg-[#0B0F1A]">
                Fact must exist
              </option>
              <option value="EQUALS" className="bg-[#0B0F1A]">
                Fact must equal a value
              </option>
              <option value="AT_LEAST" className="bg-[#0B0F1A]">
                Fact must be at least a value (numeric)
              </option>
              <option value="DURATION_YEARS" className="bg-[#0B0F1A]">
                At least N years since the fact's effective date
              </option>
              <option value="ADVANCED" className="bg-[#0B0F1A]">
                Advanced (raw JSON)
              </option>
            </select>

            {form.logicMode !== "ADVANCED" && (
              <>
                <select
                  value={form.logicFactKey || form.bindings[0]?.factKey || ""}
                  onChange={(event) => setForm({ ...form, logicFactKey: event.target.value })}
                  className={`${INPUT_CLASS} mb-2`}
                >
                  <option value="" className="bg-[#0B0F1A]">
                    Fact key this logic checks...
                  </option>
                  {factTypes.map((factType) => (
                    <option key={factType.factKey} value={factType.factKey} className="bg-[#0B0F1A]">
                      {factType.factKey}
                    </option>
                  ))}
                </select>
                {(form.logicMode === "EQUALS" || form.logicMode === "AT_LEAST" || form.logicMode === "DURATION_YEARS") && (
                  <input
                    value={form.logicOperandValue}
                    onChange={(event) => setForm({ ...form, logicOperandValue: event.target.value })}
                    placeholder={form.logicMode === "DURATION_YEARS" ? "e.g. 1 (years)" : "Value to compare against"}
                    className={INPUT_CLASS}
                  />
                )}
              </>
            )}

            {form.logicMode === "ADVANCED" && (
              <textarea
                value={form.advancedJson}
                onChange={(event) => setForm({ ...form, advancedJson: event.target.value })}
                rows={4}
                placeholder='{"node":"FACT_PREDICATE","factKey":"...","operator":"EXISTS", ...}'
                className={`${INPUT_CLASS} font-mono text-xs`}
              />
            )}

            <p className="mt-2 text-[11px] text-slate-500">
              Preview: <code className="break-all">{buildSatisfactionLogicJson(form) || "—"}</code>
            </p>
          </div>

          {formError && <ErrorAlert message={formError} />}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setModalMode(null)} disabled={saving}>
              Cancel
            </Button>
            <Button variant="primary" onClick={() => void handleSubmit()} loading={saving}>
              {modalMode === "create" ? "Create Requirement" : "Save Changes"}
            </Button>
          </div>
        </div>
      </Modal>

      <ConfirmDialog
        open={pendingTransition !== null}
        title={pendingTransition ? `${pendingTransition.target} this requirement?` : ""}
        description={
          pendingTransition
            ? `"${pendingTransition.requirement.title}" will move from ${pendingTransition.requirement.status} to ${pendingTransition.target}. This may affect any pathway that references it.`
            : ""
        }
        confirmText={pendingTransition?.target === "RETRACTED" ? "Retract" : "Confirm"}
        variant={pendingTransition?.target === "RETRACTED" ? "danger" : "warning"}
        loading={transitioning}
        onConfirm={() => void confirmTransition()}
        onCancel={() => setPendingTransition(null)}
      />
    </motion.div>
  );
}
