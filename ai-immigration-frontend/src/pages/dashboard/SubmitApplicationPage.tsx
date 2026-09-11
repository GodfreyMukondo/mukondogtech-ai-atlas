import {
  useCallback,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react";

import { useNavigate } from "react-router-dom";

import { motion } from "framer-motion";

import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  FileSearch,
  FileText,
  Loader2,
  Mail,
  Send,
  ShieldCheck,
  Trash2,
  UploadCloud,
} from "lucide-react";

import { useAuth } from "../../context/AuthContext";

import { uploadDocumentApi } from "../../api/documentApi";
import { submitApplicationApi } from "../../api/applicationApi";

import { notificationService } from "../../services/notificationService";
import { errorService } from "../../services/errorService";

import type { Application } from "../../types/application";

import Input from "../../components/common/Input";
import Button from "../../components/common/Button";
import Modal from "../../components/common/Modal";

/**
 * ============================================================================
 * SUBMIT APPLICATION PAGE
 * ============================================================================
 *
 * Applicant-facing form used to submit a new immigration application:
 * personal details plus one or more supporting documents.
 *
 * Flow:
 *
 * 1. The applicant fills in personal details.
 * 2. Each attached file is uploaded immediately via the existing document
 *    upload pipeline (POST /api/documents/upload), which returns a
 *    document ID.
 * 3. On submit, the personal details plus the collected document IDs are
 *    sent to POST /api/applications, which links those documents to the
 *    new application.
 * 4. The submitted application then appears on the admin review page
 *    (GET /api/admin/applications).
 *
 * SECURITY:
 *
 * This page never supplies a user ID. The backend resolves the
 * authenticated user from the JWT for both document uploads and the
 * application submission itself.
 * ============================================================================
 */

const DOCUMENT_TYPES = [
  { value: "PASSPORT", label: "Passport" },
  { value: "VISA", label: "Visa" },
  { value: "BIRTH_CERTIFICATE", label: "Birth Certificate" },
  { value: "MARRIAGE_CERTIFICATE", label: "Marriage Certificate" },
  { value: "EMPLOYMENT_LETTER", label: "Employment Letter" },
  { value: "BANK_STATEMENT", label: "Bank Statement" },
  { value: "EDUCATIONAL_CERTIFICATE", label: "Educational Certificate" },
  { value: "OTHER", label: "Other Supporting Document" },
] as const;

const MAX_DATE_OF_BIRTH = new Date(
  Date.now() - 24 * 60 * 60 * 1000,
)
  .toISOString()
  .slice(0, 10);

const VISA_TYPES = [
  "Work Visa",
  "Student Visa",
  "Tourist Visa",
  "Family / Spousal Visa",
  "Permanent Residency",
  "Business Visa",
  "Asylum / Refugee",
  "Other",
];

interface AttachedDocument {
  documentId: number;
  fileName: string;
  documentType: string;
  documentTypeLabel: string;
}

interface UploadingFile {
  key: string;
  fileName: string;
  progress: number;
}

interface FormErrors {
  fullName?: string;
  email?: string;
  country?: string;
  visaType?: string;
  documents?: string;
}

function formatSubmittedDate(value?: string | null): string {
  if (!value) {
    return "—";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function buildReferenceId(id: number): string {
  return `APP-${String(id).padStart(6, "0")}`;
}

export default function SubmitApplicationPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [fullName, setFullName] = useState(user?.fullName ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [phone, setPhone] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [country, setCountry] = useState("");
  const [visaType, setVisaType] = useState("");
  const [notes, setNotes] = useState("");

  const [documentType, setDocumentType] = useState<string>(
    DOCUMENT_TYPES[0].value,
  );

  const [attachedDocuments, setAttachedDocuments] = useState<
    AttachedDocument[]
  >([]);

  const [uploadingFiles, setUploadingFiles] = useState<UploadingFile[]>([]);

  const [errors, setErrors] = useState<FormErrors>({});

  const [submitting, setSubmitting] = useState(false);

  const [submittedApplication, setSubmittedApplication] =
    useState<Application | null>(null);

  const [successModalOpen, setSuccessModalOpen] = useState(false);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  /**
   * ==========================================================================
   * RESET FORM
   * ==========================================================================
   *
   * Clears the form back to a blank state after a successful submission, so
   * the previously-submitted documents cannot accidentally be resubmitted.
   */
  const resetForm = useCallback(() => {
    setFullName(user?.fullName ?? "");
    setEmail(user?.email ?? "");
    setPhone("");
    setDateOfBirth("");
    setCountry("");
    setVisaType("");
    setNotes("");
    setAttachedDocuments([]);
    setErrors({});
  }, [user]);

  /**
   * ==========================================================================
   * ATTACH DOCUMENT
   * ==========================================================================
   *
   * Uploads the selected file immediately through the existing document
   * pipeline and, on success, records its returned ID for submission.
   */
  const handleFileChange = useCallback(
    async (event: ChangeEvent<HTMLInputElement>) => {
      const file = event.target.files?.[0];

      event.target.value = "";

      if (!file) {
        return;
      }

      const selectedType = documentType;

      const selectedTypeLabel =
        DOCUMENT_TYPES.find((type) => type.value === selectedType)?.label ??
        selectedType;

      const uploadKey = `${Date.now()}-${file.name}`;

      setUploadingFiles((previous) => [
        ...previous,
        { key: uploadKey, fileName: file.name, progress: 0 },
      ]);

      try {
        const uploaded = await uploadDocumentApi(
          {
            file,
            documentType: selectedType,
          },
          (progress) => {
            setUploadingFiles((previous) =>
              previous.map((item) =>
                item.key === uploadKey ? { ...item, progress } : item,
              ),
            );
          },
        );

        setAttachedDocuments((previous) => [
          ...previous,
          {
            documentId: uploaded.id,
            fileName: uploaded.fileName,
            documentType: selectedType,
            documentTypeLabel: selectedTypeLabel,
          },
        ]);

        setErrors((previous) => ({ ...previous, documents: undefined }));
      } catch (uploadError: unknown) {
        const appError = errorService.log(
          uploadError,
          "Submit Application - Document Upload",
        );

        notificationService.error(appError.message);
      } finally {
        setUploadingFiles((previous) =>
          previous.filter((item) => item.key !== uploadKey),
        );
      }
    },
    [documentType],
  );

  const handleRemoveDocument = useCallback((documentId: number) => {
    setAttachedDocuments((previous) =>
      previous.filter((document) => document.documentId !== documentId),
    );
  }, []);

  /**
   * ==========================================================================
   * VALIDATION
   * ==========================================================================
   */
  const validate = useCallback((): FormErrors => {
    const nextErrors: FormErrors = {};

    if (!fullName.trim()) {
      nextErrors.fullName = "Full name is required.";
    }

    if (!email.trim()) {
      nextErrors.email = "Email is required.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      nextErrors.email = "Enter a valid email address.";
    }

    if (!country.trim()) {
      nextErrors.country = "Country is required.";
    }

    if (!visaType.trim()) {
      nextErrors.visaType = "Visa type is required.";
    }

    if (attachedDocuments.length === 0) {
      nextErrors.documents =
        "Attach at least one supporting document.";
    }

    return nextErrors;
  }, [fullName, email, country, visaType, attachedDocuments]);

  /**
   * ==========================================================================
   * SUBMIT
   * ==========================================================================
   */
  const handleSubmit = useCallback(
    async (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();

      const validationErrors = validate();

      setErrors(validationErrors);

      if (Object.keys(validationErrors).length > 0) {
        return;
      }

      setSubmitting(true);

      try {
        const created = await submitApplicationApi({
          fullName: fullName.trim(),
          email: email.trim(),
          phone: phone.trim() || undefined,
          dateOfBirth: dateOfBirth || undefined,
          country: country.trim(),
          visaType: visaType.trim(),
          notes: notes.trim() || undefined,
          documentIds: attachedDocuments.map(
            (document) => document.documentId,
          ),
        });

        setSubmittedApplication(created);
        setSuccessModalOpen(true);
        resetForm();
      } catch (submitError: unknown) {
        const appError = errorService.log(
          submitError,
          "Submit Application",
        );

        notificationService.error(appError.message);
      } finally {
        setSubmitting(false);
      }
    },
    [
      validate,
      fullName,
      email,
      phone,
      dateOfBirth,
      country,
      visaType,
      notes,
      attachedDocuments,
      resetForm,
    ],
  );

  /**
   * ==========================================================================
   * SUCCESS MODAL ACTIONS
   * ==========================================================================
   */
  const handleCloseSuccessModal = useCallback(() => {
    setSuccessModalOpen(false);
  }, []);

  const handleViewApplications = useCallback(() => {
    setSuccessModalOpen(false);
    navigate("/dashboard/applications");
  }, [navigate]);

  return (
    <div className="min-h-screen">
      <main className="w-full px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        {/* ==================================================================
            PAGE HEADER
        ================================================================== */}

        <motion.section
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="mb-8"
        >
          <button
            type="button"
            onClick={() => navigate("/dashboard/applications")}
            className="
              mb-4
              inline-flex
              items-center
              gap-2
              text-sm
              font-semibold
              text-slate-400
              transition
              hover:text-white
            "
          >
            <ArrowLeft size={16} aria-hidden="true" />
            Back to Applications
          </button>

          <div className="mb-3 flex items-center gap-2 text-sm font-medium text-[#C6A15B]">
            <Send size={18} aria-hidden="true" />
            <span>New submission</span>
          </div>

          <h1 className="text-3xl font-bold tracking-tight text-white sm:text-4xl">
            Submit Application
          </h1>

          <p className="mt-2 max-w-2xl text-base leading-7 text-slate-400">
            Provide your personal details and attach the supporting
            documents required for your immigration application. Our team
            will review your submission once it is sent.
          </p>
        </motion.section>

        <form onSubmit={handleSubmit} noValidate>
          <div className="grid gap-6 xl:grid-cols-3">
            {/* ================================================================
                PERSONAL DETAILS
            ================================================================ */}

            <motion.section
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: 0.05 }}
              className="
                xl:col-span-2
                rounded-2xl
                border
                border-white/10
                bg-white/5
                backdrop-blur-xl
                p-6
                shadow-sm
              "
            >
              <h2 className="text-lg font-semibold text-white">
                Personal details
              </h2>

              <p className="mt-1 text-sm text-slate-400">
                These details will be reviewed alongside your submitted
                documents.
              </p>

              <div className="mt-6 grid gap-5 sm:grid-cols-2">
                <Input
                  label="Full name"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                  placeholder="Jane Doe"
                  error={errors.fullName}
                />

                <Input
                  label="Email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  error={errors.email}
                />

                <Input
                  label="Phone (optional)"
                  type="tel"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  placeholder="+1 555 123 4567"
                />

                <Input
                  label="Date of birth (optional)"
                  type="date"
                  max={MAX_DATE_OF_BIRTH}
                  value={dateOfBirth}
                  onChange={(event) => setDateOfBirth(event.target.value)}
                />

                <Input
                  label="Destination country"
                  value={country}
                  onChange={(event) => setCountry(event.target.value)}
                  placeholder="e.g. United Kingdom"
                  error={errors.country}
                />

                <div className="space-y-2">
                  <label
                    htmlFor="visa-type"
                    className="block text-sm font-medium text-slate-200"
                  >
                    Visa type
                  </label>

                  <select
                    id="visa-type"
                    value={visaType}
                    onChange={(event) => setVisaType(event.target.value)}
                    className={`
                      w-full
                      rounded-xl
                      border
                      bg-white/5
                      px-4
                      py-3
                      text-sm
                      text-white
                      outline-none
                      transition
                      focus:ring-2
                      ${
                        errors.visaType
                          ? "border-red-400 focus:ring-red-300"
                          : "border-white/15 focus:ring-[#C6A15B]"
                      }
                    `}
                  >
                    <option value="" className="bg-[#1F314A]">
                      Select a visa type
                    </option>

                    {VISA_TYPES.map((type) => (
                      <option
                        key={type}
                        value={type}
                        className="bg-[#1F314A]"
                      >
                        {type}
                      </option>
                    ))}
                  </select>

                  {errors.visaType && (
                    <p className="text-sm text-red-500">{errors.visaType}</p>
                  )}
                </div>
              </div>

              <div className="mt-5 space-y-2">
                <label
                  htmlFor="notes"
                  className="block text-sm font-medium text-slate-200"
                >
                  Additional notes (optional)
                </label>

                <textarea
                  id="notes"
                  rows={4}
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                  placeholder="Anything else the reviewing team should know..."
                  className="
                    w-full
                    rounded-xl
                    border
                    border-white/15
                    bg-white/5
                    px-4
                    py-3
                    text-sm
                    text-white
                    outline-none
                    transition
                    placeholder:text-slate-500
                    focus:ring-2
                    focus:ring-[#C6A15B]
                  "
                />
              </div>
            </motion.section>

            {/* ================================================================
                SUPPORTING DOCUMENTS
            ================================================================ */}

            <motion.section
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.3, delay: 0.1 }}
              className="
                rounded-2xl
                border
                border-white/10
                bg-white/5
                backdrop-blur-xl
                p-6
                shadow-sm
              "
            >
              <h2 className="text-lg font-semibold text-white">
                Supporting documents
              </h2>

              <p className="mt-1 text-sm text-slate-400">
                Attach at least one document, such as a passport or visa.
              </p>

              <div className="mt-5 space-y-2">
                <label
                  htmlFor="document-type"
                  className="block text-sm font-medium text-slate-200"
                >
                  Document type
                </label>

                <select
                  id="document-type"
                  value={documentType}
                  onChange={(event) => setDocumentType(event.target.value)}
                  className="
                    w-full
                    rounded-xl
                    border
                    border-white/15
                    bg-white/5
                    px-4
                    py-3
                    text-sm
                    text-white
                    outline-none
                    transition
                    focus:ring-2
                    focus:ring-[#C6A15B]
                  "
                >
                  {DOCUMENT_TYPES.map((type) => (
                    <option
                      key={type.value}
                      value={type.value}
                      className="bg-[#1F314A]"
                    >
                      {type.label}
                    </option>
                  ))}
                </select>
              </div>

              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
                className="hidden"
                onChange={handleFileChange}
              />

              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="
                  mt-4
                  flex
                  w-full
                  flex-col
                  items-center
                  justify-center
                  gap-2
                  rounded-xl
                  border-2
                  border-dashed
                  border-white/15
                  px-4
                  py-8
                  text-center
                  transition
                  hover:border-[#C6A15B]
                  hover:bg-[#C6A15B]/5
                "
              >
                <UploadCloud
                  size={28}
                  className="text-[#C6A15B]"
                  aria-hidden="true"
                />

                <span className="text-sm font-semibold text-white">
                  Choose a file to attach
                </span>

                <span className="text-xs text-slate-400">
                  PDF, PNG or JPEG, up to 10MB
                </span>
              </button>

              {errors.documents && (
                <p className="mt-3 flex items-center gap-2 text-sm text-red-400">
                  <AlertTriangle size={14} aria-hidden="true" />
                  {errors.documents}
                </p>
              )}

              {/* Uploading */}
              {uploadingFiles.length > 0 && (
                <div className="mt-4 space-y-2">
                  {uploadingFiles.map((item) => (
                    <div
                      key={item.key}
                      className="
                        flex
                        items-center
                        gap-3
                        rounded-xl
                        border
                        border-white/10
                        bg-white/5
                        px-4
                        py-3
                      "
                    >
                      <Loader2
                        size={16}
                        className="shrink-0 animate-spin text-[#C6A15B]"
                        aria-hidden="true"
                      />

                      <span className="min-w-0 flex-1 truncate text-sm text-slate-300">
                        {item.fileName}
                      </span>

                      <span className="shrink-0 text-xs font-medium text-slate-500">
                        {item.progress}%
                      </span>
                    </div>
                  ))}
                </div>
              )}

              {/* Attached documents */}
              {attachedDocuments.length > 0 && (
                <ul className="mt-4 space-y-2">
                  {attachedDocuments.map((document) => (
                    <li
                      key={document.documentId}
                      className="
                        flex
                        items-center
                        gap-3
                        rounded-xl
                        border
                        border-white/10
                        bg-white/5
                        px-4
                        py-3
                      "
                    >
                      <FileText
                        size={16}
                        className="shrink-0 text-[#C6A15B]"
                        aria-hidden="true"
                      />

                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-white">
                          {document.fileName}
                        </p>

                        <p className="text-xs text-slate-400">
                          {document.documentTypeLabel}
                        </p>
                      </div>

                      <button
                        type="button"
                        onClick={() =>
                          handleRemoveDocument(document.documentId)
                        }
                        aria-label={`Remove ${document.fileName}`}
                        className="
                          shrink-0
                          rounded-lg
                          p-1.5
                          text-slate-400
                          transition
                          hover:bg-red-400/10
                          hover:text-red-300
                        "
                      >
                        <Trash2 size={15} aria-hidden="true" />
                      </button>
                    </li>
                  ))}
                </ul>
              )}

              <div className="mt-5 flex items-start gap-2 rounded-xl border border-white/10 bg-white/5 p-3 text-xs leading-5 text-slate-400">
                <ShieldCheck
                  size={15}
                  className="mt-0.5 shrink-0 text-emerald-400"
                  aria-hidden="true"
                />
                Your documents are stored securely and only visible to you
                and our review team.
              </div>
            </motion.section>
          </div>

          {/* ================================================================
              ACTIONS
          ================================================================ */}

          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.3, delay: 0.15 }}
            className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end"
          >
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate("/dashboard/applications")}
              disabled={submitting}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              loading={submitting}
              disabled={submitting || uploadingFiles.length > 0}
            >
              <Send size={16} aria-hidden="true" />
              Submit Application
            </Button>
          </motion.div>
        </form>

        {/* ==================================================================
            SUCCESS CONFIRMATION
        ================================================================== */}

        <Modal
          open={successModalOpen}
          onClose={handleCloseSuccessModal}
          title=""
        >
          {submittedApplication && (
            <div>
              <div className="flex flex-col items-center text-center">
                <motion.div
                  initial={{ scale: 0.5, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  transition={{
                    type: "spring",
                    stiffness: 260,
                    damping: 18,
                  }}
                  className="
                    flex
                    h-16
                    w-16
                    items-center
                    justify-center
                    rounded-full
                    bg-emerald-400/10
                    text-emerald-400
                  "
                >
                  <CheckCircle2 size={34} aria-hidden="true" />
                </motion.div>

                <h3 className="mt-5 text-xl font-bold text-white">
                  Application Submitted Successfully
                </h3>

                <p className="mt-2 max-w-sm text-sm leading-6 text-slate-400">
                  Thank you, {submittedApplication.fullName.split(" ")[0]}.
                  Your immigration application has been received and is now
                  in our review queue.
                </p>
              </div>

              <div className="mt-6 grid grid-cols-2 gap-x-4 gap-y-4 rounded-xl border border-white/10 bg-white/5 p-4 text-left text-sm">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Reference ID
                  </p>
                  <p className="mt-1 font-semibold text-white">
                    {buildReferenceId(submittedApplication.id)}
                  </p>
                </div>

                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Status
                  </p>
                  <p className="mt-1 font-semibold text-[#C6A15B]">
                    Pending Review
                  </p>
                </div>

                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Visa Type
                  </p>
                  <p className="mt-1 font-semibold text-white">
                    {submittedApplication.visaType}
                  </p>
                </div>

                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Destination
                  </p>
                  <p className="mt-1 font-semibold text-white">
                    {submittedApplication.country}
                  </p>
                </div>

                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Documents
                  </p>
                  <p className="mt-1 font-semibold text-white">
                    {submittedApplication.documentCount} attached
                  </p>
                </div>

                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Submitted
                  </p>
                  <p className="mt-1 font-semibold text-white">
                    {formatSubmittedDate(submittedApplication.submittedAt)}
                  </p>
                </div>
              </div>

              <div className="mt-5 space-y-3 rounded-xl border border-white/10 bg-white/5 p-4 text-left">
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  What happens next
                </p>

                <ul className="space-y-2.5 text-sm text-slate-300">
                  <li className="flex items-start gap-2.5">
                    <ShieldCheck
                      size={16}
                      className="mt-0.5 shrink-0 text-emerald-400"
                      aria-hidden="true"
                    />
                    Our team reviews your details and documents, typically
                    within 3-5 business days.
                  </li>

                  <li className="flex items-start gap-2.5">
                    <Mail
                      size={16}
                      className="mt-0.5 shrink-0 text-[#C6A15B]"
                      aria-hidden="true"
                    />
                    You will receive an email update as soon as your
                    application status changes.
                  </li>

                  <li className="flex items-start gap-2.5">
                    <FileSearch
                      size={16}
                      className="mt-0.5 shrink-0 text-blue-300"
                      aria-hidden="true"
                    />
                    Track progress anytime from My Applications.
                  </li>
                </ul>
              </div>

              <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                <Button variant="secondary" onClick={handleCloseSuccessModal}>
                  Submit Another
                </Button>

                <Button onClick={handleViewApplications}>
                  View My Applications
                  <ArrowRight size={16} aria-hidden="true" />
                </Button>
              </div>
            </div>
          )}
        </Modal>
      </main>
    </div>
  );
}
