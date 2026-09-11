import API from "./axios";


/**
 * ======================================================
 * ADMIN API
 * ======================================================
 *
 * Handles administrator operations:
 *
 * - Dashboard
 * - Reports
 * - AI audits
 * - System management
 *
 * ======================================================
 */


export const generateAdminReport = async () => {

    const response = await API.post(
        "/admin/reports/generate"
    );

    return response.data;

};



export const runAIAudit = async () => {

    const response = await API.post(
        "/admin/audit/run"
    );

    return response.data;

};



export const getAdminDashboard = async () => {

    const response = await API.get(
        "/admin/dashboard"
    );

    return response.data;

};



/**
 * ======================================================
 * CASE CREATION
 * ======================================================
 *
 * Backs the admin dashboard's "Create Case" action - an administrator
 * creating a new immigration application on behalf of an existing user.
 *
 * Mirrors the applicant-facing submission flow: supporting documents must
 * already have been uploaded by that user (see getAvailableCaseDocuments),
 * rather than accepting file uploads directly here.
 */

export interface AvailableCaseDocument {
    id: number;
    fileName: string;
    documentType: string;
    uploadedAt: string;
}

export interface CreateCasePayload {
    userId: string;
    fullName: string;
    email: string;
    phone?: string;
    dateOfBirth?: string;
    country: string;
    visaType: string;
    notes?: string;
    documentIds: number[];
}

export const getAvailableCaseDocuments = async (
    userId: string,
): Promise<AvailableCaseDocument[]> => {

    const response = await API.get<AvailableCaseDocument[]>(
        "/admin/applications/documents/available",
        {
            params: { userId },
        },
    );

    return Array.isArray(response.data) ? response.data : [];

};



export const createCase = async (payload: CreateCasePayload) => {

    const response = await API.post(
        "/admin/applications",
        {
            ...payload,
            userId: Number(payload.userId),
        },
    );

    return response.data;

};