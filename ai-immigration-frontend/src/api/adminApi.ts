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