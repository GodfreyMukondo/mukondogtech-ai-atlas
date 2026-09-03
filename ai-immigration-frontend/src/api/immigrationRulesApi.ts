import API from "./axios";

/**
 * ============================================================================
 * IMMIGRATION RULES API
 * ============================================================================
 *
 * Backend base path:
 *
 *   /api/admin/immigration-rules
 *
 * Axios already provides:
 *
 *   /api
 *
 * through the configured baseURL.
 *
 * Therefore the endpoints below become:
 *
 *   GET    /api/admin/immigration-rules
 *   GET    /api/admin/immigration-rules/stats
 *   GET    /api/admin/immigration-rules/{id}
 *   POST   /api/admin/immigration-rules
 *   PUT    /api/admin/immigration-rules/{id}
 *   DELETE /api/admin/immigration-rules/{id}
 *   POST   /api/admin/immigration-rules/{id}/publish
 *   POST   /api/admin/immigration-rules/{id}/review
 *   POST   /api/admin/immigration-rules/{id}/draft
 * ============================================================================
 */

export type ImmigrationRuleStatus =
    | "ACTIVE"
    | "DRAFT"
    | "REVIEW";

export interface ImmigrationRuleSummary {
    id: number;
    country: string;
    visaType: string;
    category: string;
    status: ImmigrationRuleStatus;
    version: string;
    source: string;
    updatedAt: string;
    updatedBy: string;
}

export interface ImmigrationRule {
    id: number;
    country: string;
    visaType: string;
    category: string;
    status: ImmigrationRuleStatus;
    version: string;
    source: string;
    sourceUrl?: string | null;
    description?: string | null;
    requirements?: string | null;
    eligibility?: string | null;
    restrictions?: string | null;
    processingTime?: string | null;
    fees?: string | null;
    effectiveDate?: string | null;
    expiryDate?: string | null;
    aiIndexed: boolean;
    aiIndexedAt?: string | null;
    createdBy: string;
    updatedBy: string;
    createdAt: string;
    updatedAt: string;
}

export interface ImmigrationRuleStatistics {
    totalRules: number;
    activeRules: number;
    draftRules: number;
    reviewRules: number;
    countriesCovered: number;
    categoriesCovered: number;
    visaTypesCovered: number;
    aiIndexedRules: number;
    pendingAiIndexRules: number;
}

export interface CreateImmigrationRuleRequest {
    country: string;
    visaType: string;
    category: string;
    status?: ImmigrationRuleStatus;
    version: string;
    source: string;
    sourceUrl?: string;
    description?: string;
    requirements?: string;
    eligibility?: string;
    restrictions?: string;
    processingTime?: string;
    fees?: string;
    effectiveDate?: string;
    expiryDate?: string;
}

export interface UpdateImmigrationRuleRequest {
    country: string;
    visaType: string;
    category: string;
    status?: ImmigrationRuleStatus;
    version: string;
    source: string;
    sourceUrl?: string;
    description?: string;
    requirements?: string;
    eligibility?: string;
    restrictions?: string;
    processingTime?: string;
    fees?: string;
    effectiveDate?: string;
    expiryDate?: string;
}

export interface ImmigrationRulePage {
    content: ImmigrationRuleSummary[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}

export interface ImmigrationRuleQuery {
    search?: string;
    country?: string;
    category?: string;
    status?: ImmigrationRuleStatus;
    page?: number;
    size?: number;
}

const IMMIGRATION_RULES_API =
    "/admin/immigration-rules";

/**
 * ============================================================================
 * GET RULES
 * ============================================================================
 */

export async function getImmigrationRules(
    params: ImmigrationRuleQuery = {}
): Promise<ImmigrationRulePage> {

    const response =
        await API.get<ImmigrationRulePage>(
            IMMIGRATION_RULES_API,
            {
                params: {
                    search:
                        params.search?.trim() ||
                        undefined,

                    country:
                        params.country?.trim() ||
                        undefined,

                    category:
                        params.category?.trim() ||
                        undefined,

                    status:
                        params.status ||
                        undefined,

                    page:
                        params.page ?? 0,

                    size:
                        params.size ?? 20,
                },
            }
        );

    return response.data;
}

/**
 * ============================================================================
 * GET STATISTICS
 * ============================================================================
 */

export async function getImmigrationRuleStatistics():
    Promise<ImmigrationRuleStatistics> {

    const response =
        await API.get<ImmigrationRuleStatistics>(
            `${IMMIGRATION_RULES_API}/stats`
        );

    return response.data;
}

/**
 * ============================================================================
 * GET RULE
 * ============================================================================
 */

export async function getImmigrationRule(
    id: number
): Promise<ImmigrationRule> {

    const response =
        await API.get<ImmigrationRule>(
            `${IMMIGRATION_RULES_API}/${id}`
        );

    return response.data;
}

/**
 * ============================================================================
 * CREATE RULE
 * ============================================================================
 */

export async function createImmigrationRule(
    request: CreateImmigrationRuleRequest
): Promise<ImmigrationRule> {

    const response =
        await API.post<ImmigrationRule>(
            IMMIGRATION_RULES_API,
            request
        );

    return response.data;
}

/**
 * ============================================================================
 * UPDATE RULE
 * ============================================================================
 */

export async function updateImmigrationRule(
    id: number,
    request: UpdateImmigrationRuleRequest
): Promise<ImmigrationRule> {

    const response =
        await API.put<ImmigrationRule>(
            `${IMMIGRATION_RULES_API}/${id}`,
            request
        );

    return response.data;
}

/**
 * ============================================================================
 * DELETE RULE
 * ============================================================================
 */

export async function deleteImmigrationRule(
    id: number
): Promise<void> {

    await API.delete(
        `${IMMIGRATION_RULES_API}/${id}`
    );
}

/**
 * ============================================================================
 * STATUS OPERATIONS
 * ============================================================================
 */

export async function publishImmigrationRule(
    id: number
): Promise<ImmigrationRule> {

    const response =
        await API.post<ImmigrationRule>(
            `${IMMIGRATION_RULES_API}/${id}/publish`
        );

    return response.data;
}

export async function reviewImmigrationRule(
    id: number
): Promise<ImmigrationRule> {

    const response =
        await API.post<ImmigrationRule>(
            `${IMMIGRATION_RULES_API}/${id}/review`
        );

    return response.data;
}

export async function draftImmigrationRule(
    id: number
): Promise<ImmigrationRule> {

    const response =
        await API.post<ImmigrationRule>(
            `${IMMIGRATION_RULES_API}/${id}/draft`
        );

    return response.data;
}

