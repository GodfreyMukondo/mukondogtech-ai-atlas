import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import axios from "axios";
import { ArrowLeft, Briefcase, Check, FileText, Search, UserCircle2 } from "lucide-react";

import { getUsers, type AdminUser } from "../../api/userApi";
import {
  createCase,
  getAvailableCaseDocuments,
  type AvailableCaseDocument,
} from "../../api/adminApi";

interface CaseForm {
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth: string;
  country: string;
  visaType: string;
  notes: string;
}

const EMPTY_FORM: CaseForm = {
  fullName: "",
  email: "",
  phone: "",
  dateOfBirth: "",
  country: "",
  visaType: "",
  notes: "",
};

/**
 * Delays a search query until the caller has stopped typing for `delayMs`,
 * so the user picker does not fire a backend request on every keystroke.
 */
function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => setDebounced(value), delayMs);
    return () => window.clearTimeout(timeoutId);
  }, [value, delayMs]);

  return debounced;
}

function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    const fieldErrors = data?.fieldErrors as Record<string, string> | undefined;

    if (fieldErrors && Object.keys(fieldErrors).length > 0) {
      return Object.entries(fieldErrors)
        .map(([field, reason]) => `${field}: ${reason}`)
        .join(" | ");
    }

    return data?.message ?? fallback;
  }
  return fallback;
}

export default function CreateCasePage() {
  const navigate = useNavigate();

  /* ==========================================================================
   * STEP 1: PICK A USER
   * ======================================================================== */

  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 350);

  const [userResults, setUserResults] = useState<AdminUser[]>([]);
  const [searchingUsers, setSearchingUsers] = useState(false);

  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);

  useEffect(() => {
    if (selectedUser) return;

    let cancelled = false;

    (async () => {
      setSearchingUsers(true);

      try {
        const page = await getUsers({
          search: debouncedSearch,
          page: 0,
          size: 10,
        });

        if (!cancelled) {
          setUserResults(page.content);
        }
      } catch (err) {
        console.error("[CreateCase] User search failed:", err);
        if (!cancelled) setUserResults([]);
      } finally {
        if (!cancelled) setSearchingUsers(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [debouncedSearch, selectedUser]);

  const handleSelectUser = useCallback((user: AdminUser) => {
    setSelectedUser(user);
    setForm((prev) => ({
      ...prev,
      fullName: user.name,
      email: user.email,
      phone: user.phone,
      country: user.country,
    }));
  }, []);

  const handleChangeUser = useCallback(() => {
    setSelectedUser(null);
    setDocuments([]);
    setSelectedDocumentIds([]);
  }, []);

  /* ==========================================================================
   * STEP 2: PICK SUPPORTING DOCUMENTS
   * ======================================================================== */

  const [documents, setDocuments] = useState<AvailableCaseDocument[]>([]);
  const [documentsLoading, setDocumentsLoading] = useState(false);
  const [selectedDocumentIds, setSelectedDocumentIds] = useState<number[]>([]);

  useEffect(() => {
    if (!selectedUser) return;

    let cancelled = false;

    (async () => {
      setDocumentsLoading(true);

      try {
        const available = await getAvailableCaseDocuments(selectedUser.id);
        if (!cancelled) setDocuments(available);
      } catch (err) {
        console.error("[CreateCase] Failed to load available documents:", err);
        if (!cancelled) setDocuments([]);
      } finally {
        if (!cancelled) setDocumentsLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [selectedUser]);

  const toggleDocument = useCallback((documentId: number) => {
    setSelectedDocumentIds((prev) =>
      prev.includes(documentId)
        ? prev.filter((id) => id !== documentId)
        : [...prev, documentId],
    );
  }, []);

  /* ==========================================================================
   * STEP 3: APPLICATION DETAILS
   * ======================================================================== */

  const [form, setForm] = useState<CaseForm>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (submitting) return;

    if (!selectedUser) {
      toast.error("Select a user to create this case for.");
      return;
    }

    if (selectedDocumentIds.length === 0) {
      toast.error("Select at least one supporting document.");
      return;
    }

    setSubmitting(true);
    const loadingToast = toast.loading("Creating case...");

    try {
      await createCase({
        userId: selectedUser.id,
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || undefined,
        dateOfBirth: form.dateOfBirth || undefined,
        country: form.country.trim(),
        visaType: form.visaType.trim(),
        notes: form.notes.trim() || undefined,
        documentIds: selectedDocumentIds,
      });

      toast.success("Case created successfully.", { id: loadingToast });
      navigate("/admin/applications");
    } catch (err) {
      const message = getApiErrorMessage(err, "Failed to create case.");
      console.error("[CreateCase] Case creation failed:", message);
      if (axios.isAxiosError(err)) {
        console.error("[CreateCase] Raw response body:", JSON.stringify(err.response?.data));
      }
      toast.error(message, {
        id: loadingToast,
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen px-6 py-12">
      <div className="mx-auto max-w-3xl">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="mb-6 flex items-center gap-2 rounded-xl border border-white/15 bg-white/5 text-slate-200 px-4 py-2 shadow-sm hover:bg-white/10 transition"
        >
          <ArrowLeft size={18} />
          Back
        </button>

        <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-8 shadow-xl">
          <div className="mb-8 flex items-center gap-4">
            <div className="rounded-2xl bg-[#C6A15B]/10 p-4 text-[#C6A15B]">
              <Briefcase size={30} />
            </div>

            <div>
              <h1 className="text-3xl font-black text-white">Create Case</h1>
              <p className="text-slate-400">
                Submit a new immigration application on behalf of a user.
              </p>
            </div>
          </div>

          {/* STEP 1: USER PICKER */}
          <div className="mb-6">
            <label className="mb-2 block text-sm font-semibold text-slate-200">
              1. Select user
            </label>

            {selectedUser ? (
              <div className="flex items-center justify-between rounded-xl border border-[#C6A15B]/40 bg-[#C6A15B]/10 p-4">
                <div className="flex items-center gap-3">
                  <UserCircle2 size={22} className="text-[#C6A15B]" />
                  <div>
                    <p className="font-semibold text-white">{selectedUser.name}</p>
                    <p className="text-xs text-slate-400">{selectedUser.email}</p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={handleChangeUser}
                  className="text-sm font-semibold text-slate-300 hover:text-white"
                >
                  Change
                </button>
              </div>
            ) : (
              <>
                <div className="relative">
                  <Search
                    size={16}
                    className="absolute left-3 top-3.5 text-slate-500"
                  />
                  <input
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="Search users by name or email..."
                    className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 py-3 pl-10 pr-4"
                  />
                </div>

                <div className="mt-2 max-h-60 overflow-y-auto rounded-xl border border-white/10">
                  {searchingUsers ? (
                    <p className="p-4 text-sm text-slate-400">Searching...</p>
                  ) : userResults.length === 0 ? (
                    <p className="p-4 text-sm text-slate-400">No users found.</p>
                  ) : (
                    userResults.map((user) => (
                      <button
                        key={user.id}
                        type="button"
                        onClick={() => handleSelectUser(user)}
                        className="flex w-full items-center justify-between px-4 py-3 text-left hover:bg-white/5"
                      >
                        <div>
                          <p className="text-sm font-semibold text-white">
                            {user.name}
                          </p>
                          <p className="text-xs text-slate-400">{user.email}</p>
                        </div>
                        <span className="text-xs text-slate-500">
                          {user.applications} application
                          {user.applications === 1 ? "" : "s"}
                        </span>
                      </button>
                    ))
                  )}
                </div>
              </>
            )}
          </div>

          {selectedUser && (
            <form onSubmit={handleSubmit} className="space-y-5">
              {/* STEP 2: APPLICATION DETAILS */}
              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  2. Application details
                </label>

                <div className="space-y-4">
                  <input
                    name="fullName"
                    value={form.fullName}
                    onChange={handleChange}
                    placeholder="Full name"
                    required
                    className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"
                  />

                  <input
                    name="email"
                    type="email"
                    value={form.email}
                    onChange={handleChange}
                    placeholder="Email address"
                    required
                    className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"
                  />

                  <div className="grid grid-cols-2 gap-4">
                    <input
                      name="phone"
                      type="tel"
                      value={form.phone}
                      onChange={handleChange}
                      placeholder="Phone (optional)"
                      className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"
                    />

                    <input
                      name="dateOfBirth"
                      type="date"
                      value={form.dateOfBirth}
                      onChange={handleChange}
                      className="w-full rounded-xl border border-white/15 bg-white/5 text-white p-3"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <input
                      name="country"
                      value={form.country}
                      onChange={handleChange}
                      placeholder="Destination country"
                      required
                      className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"
                    />

                    <input
                      name="visaType"
                      value={form.visaType}
                      onChange={handleChange}
                      placeholder="Visa type"
                      required
                      className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"
                    />
                  </div>

                  <textarea
                    name="notes"
                    value={form.notes}
                    onChange={handleChange}
                    placeholder="Notes (optional)"
                    rows={3}
                    className="w-full rounded-xl border border-white/15 bg-white/5 text-white placeholder:text-slate-500 p-3"
                  />
                </div>
              </div>

              {/* STEP 3: SUPPORTING DOCUMENTS */}
              <div>
                <label className="mb-2 block text-sm font-semibold text-slate-200">
                  3. Supporting documents
                </label>

                {documentsLoading ? (
                  <p className="text-sm text-slate-400">Loading documents...</p>
                ) : documents.length === 0 ? (
                  <p className="rounded-xl border border-dashed border-white/15 p-4 text-sm text-slate-400">
                    This user has no unattached uploaded documents. Ask them to
                    upload supporting documents before creating this case.
                  </p>
                ) : (
                  <div className="space-y-2">
                    {documents.map((doc) => {
                      const checked = selectedDocumentIds.includes(doc.id);

                      return (
                        <button
                          key={doc.id}
                          type="button"
                          onClick={() => toggleDocument(doc.id)}
                          className={`flex w-full items-center justify-between rounded-xl border p-3 text-left transition ${
                            checked
                              ? "border-[#C6A15B]/50 bg-[#C6A15B]/10"
                              : "border-white/10 bg-white/5 hover:bg-white/10"
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <FileText size={16} className="text-slate-400" />
                            <div>
                              <p className="text-sm font-semibold text-white">
                                {doc.fileName}
                              </p>
                              <p className="text-xs text-slate-400">
                                {doc.documentType}
                              </p>
                            </div>
                          </div>

                          {checked && (
                            <Check size={16} className="text-[#C6A15B]" />
                          )}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="w-full rounded-xl bg-[#C6A15B] py-3 font-bold text-[#071426] disabled:opacity-50"
              >
                {submitting ? "Creating..." : "Create Case"}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
