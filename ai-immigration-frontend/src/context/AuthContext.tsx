"use client";

import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
    type ReactNode,
} from "react";

import {
    jwtDecode,
} from "jwt-decode";


/**
 * ============================================================================
 * AUTHENTICATED USER
 * ============================================================================
 */

export interface AuthUser {

    /**
     * Database user ID.
     *
     * This is required by document APIs that currently use:
     *
     *     ?userId={id}
     */
    id?: number;

    email?: string;

    username?: string;

    fullName?: string;

    role?: string;

    roles?: string[];

    authorities?: string[];

    enabled?: boolean;

    mustChangePassword?: boolean;

    /**
     * JWT expiration timestamp.
     *
     * Stored as Unix seconds.
     */
    exp?: number;

}


/**
 * ============================================================================
 * AUTH CONTEXT TYPE
 * ============================================================================
 */

interface AuthContextType {

    /**
     * Currently authenticated user.
     */
    user: AuthUser | null;


    /**
     * Current JWT.
     */
    token: string | null;


    /**
     * True while authentication state is being restored.
     */
    loading: boolean;


    /**
     * Whether the user currently has an authenticated session.
     */
    isAuthenticated: boolean;


    /**
     * Establish an authenticated session.
     */
    login: (
        token: string,
        userData?: AuthUser
    ) => void;


    /**
     * Destroy the authenticated session.
     */
    logout: () => void;


    /**
     * Check whether the authenticated user has a role.
     */
    hasRole: (
        role: string
    ) => boolean;

}


/**
 * ============================================================================
 * STORAGE KEYS
 * ============================================================================
 */

const AUTH_TOKEN_KEY =
    "token";

const AUTH_USER_KEY =
    "user";

const AUTH_ROLE_KEY =
    "role";

const AUTH_PASSWORD_CHANGE_KEY =
    "mustChangePassword";


/**
 * ============================================================================
 * DEFAULT CONTEXT
 * ============================================================================
 */

const defaultAuthContext: AuthContextType = {

    user: null,

    token: null,

    loading: true,

    isAuthenticated: false,

    login: () => {
        throw new Error(
            "AuthContext login() is unavailable. " +
            "Make sure the component is wrapped in AuthProvider."
        );
    },

    logout: () => {
        throw new Error(
            "AuthContext logout() is unavailable. " +
            "Make sure the component is wrapped in AuthProvider."
        );
    },

    hasRole: () => false,

};


export const AuthContext =
    createContext<AuthContextType>(
        defaultAuthContext
    );


/**
 * ============================================================================
 * AUTH PROVIDER PROPS
 * ============================================================================
 */

interface AuthProviderProps {

    children: ReactNode;

}


/**
 * ============================================================================
 * HELPERS
 * ============================================================================
 */


/**
 * Normalize a database user ID.
 *
 * Supports values such as:
 *
 *     1
 *     "1"
 *
 * while rejecting:
 *
 *     undefined
 *     null
 *     0
 *     NaN
 *     negative values
 *     decimal values
 */
const normalizeUserId = (
    value: unknown
): number | undefined => {

    if (
        value === undefined ||
        value === null ||
        value === ""
    ) {
        return undefined;
    }


    const numericValue =
        Number(value);


    if (
        !Number.isInteger(numericValue) ||
        numericValue <= 0
    ) {
        return undefined;
    }


    return numericValue;

};


/**
 * Normalize a role.
 *
 * Examples:
 *
 *     USER
 *     ROLE_USER
 *     role_user
 *
 * all become:
 *
 *     USER
 */
const normalizeRole = (
    role?: string
): string => {

    if (
        !role ||
        typeof role !== "string"
    ) {
        return "";
    }


    return role
        .trim()
        .replace(
            /^ROLE_/i,
            ""
        )
        .toUpperCase();

};


/**
 * Extract the most appropriate role from a user object.
 */
const resolvePrimaryRole = (
    user?: AuthUser
): string | undefined => {

    if (!user) {
        return undefined;
    }


    return (
        user.role ||
        user.roles?.[0] ||
        user.authorities?.[0]
    );

};


/**
 * Build a normalized authenticated user.
 */
const normalizeUser = (
    user: AuthUser
): AuthUser => {

    const primaryRole =
        resolvePrimaryRole(user);


    const normalizedId =
        normalizeUserId(
            user.id
        );


    return {

        ...user,

        ...(normalizedId !== undefined
            ? {
                id: normalizedId,
            }
            : {}),

        ...(primaryRole
            ? {
                role: primaryRole,
            }
            : {}),

    };

};


/**
 * ============================================================================
 * AUTH PROVIDER
 * ============================================================================
 */

export default function AuthProvider({
    children,
}: AuthProviderProps) {


    /**
     * ------------------------------------------------------------------------
     * STATE
     * ------------------------------------------------------------------------
     */

    const [
        token,
        setToken,
    ] =
        useState<string | null>(null);


    const [
        user,
        setUser,
    ] =
        useState<AuthUser | null>(null);


    const [
        loading,
        setLoading,
    ] =
        useState<boolean>(true);


    /**
     * ------------------------------------------------------------------------
     * CLEAR SESSION
     * ------------------------------------------------------------------------
     *
     * Declared with useCallback so it can safely be used by effects and
     * callbacks without causing unnecessary re-renders.
     */

    const clearSession =
        useCallback((): void => {

            try {

                localStorage.removeItem(
                    AUTH_TOKEN_KEY
                );

                localStorage.removeItem(
                    AUTH_USER_KEY
                );

                localStorage.removeItem(
                    AUTH_ROLE_KEY
                );

                localStorage.removeItem(
                    AUTH_PASSWORD_CHANGE_KEY
                );

            } catch (error) {

                console.error(
                    "Unable to clear authentication storage.",
                    error
                );

            }


            setToken(null);

            setUser(null);

        }, []);


    /**
     * ------------------------------------------------------------------------
     * RESTORE AUTHENTICATION SESSION
     * ------------------------------------------------------------------------
     */

    useEffect(() => {

        let active =
            true;


        const restoreSession =
            (): void => {

                try {

                    /**
                     * --------------------------------------------------------
                     * READ STORAGE
                     * --------------------------------------------------------
                     */

                    const storedToken =
                        localStorage.getItem(
                            AUTH_TOKEN_KEY
                        );


                    const storedUser =
                        localStorage.getItem(
                            AUTH_USER_KEY
                        );


                    /**
                     * --------------------------------------------------------
                     * NO SESSION
                     * --------------------------------------------------------
                     */

                    if (!storedToken) {

                        if (active) {

                            setToken(null);

                            setUser(null);

                            setLoading(false);

                        }

                        return;

                    }


                    /**
                     * --------------------------------------------------------
                     * DECODE JWT
                     * --------------------------------------------------------
                     */

                    const decoded =
                        jwtDecode<AuthUser>(
                            storedToken
                        );


                    /**
                     * --------------------------------------------------------
                     * TOKEN EXPIRATION
                     * --------------------------------------------------------
                     */

                    if (
                        decoded.exp !== undefined &&
                        decoded.exp !== null &&
                        decoded.exp * 1000 <= Date.now()
                    ) {

                        clearSession();

                        if (active) {
                            setLoading(false);
                        }

                        return;

                    }


                    /**
                     * --------------------------------------------------------
                     * BUILD USER FROM JWT
                     * --------------------------------------------------------
                     */

                    let restoredUser: AuthUser = {
                        ...decoded,
                    };


                    /**
                     * --------------------------------------------------------
                     * MERGE STORED USER
                     * --------------------------------------------------------
                     *
                     * Backend login responses can contain additional user
                     * information that is not present in the JWT.
                     */

                    if (storedUser) {

                        try {

                            const parsedUser =
                                JSON.parse(
                                    storedUser
                                ) as AuthUser;


                            if (
                                parsedUser &&
                                typeof parsedUser === "object"
                            ) {

                                restoredUser = {

                                    ...restoredUser,

                                    ...parsedUser,

                                };

                            }

                        } catch (error) {

                            console.warn(
                                "Unable to parse stored authentication user.",
                                error
                            );

                        }

                    }


                    /**
                     * --------------------------------------------------------
                     * NORMALIZE USER
                     * --------------------------------------------------------
                     */

                    restoredUser =
                        normalizeUser(
                            restoredUser
                        );


                    /**
                     * --------------------------------------------------------
                     * USER ID VALIDATION
                     * --------------------------------------------------------
                     *
                     * We do NOT reject the entire authentication session
                     * here when an old JWT/user record does not contain an ID.
                     *
                     * Instead, useDocuments will wait for a valid ID before
                     * calling the document endpoint.
                     */

                    if (!active) {
                        return;
                    }


                    setToken(
                        storedToken
                    );


                    setUser(
                        restoredUser
                    );


                } catch (error) {

                    console.error(
                        "Authentication restore failed.",
                        error
                    );


                    clearSession();

                } finally {

                    if (active) {
                        setLoading(false);
                    }

                }

            };


        restoreSession();


        return () => {

            active = false;

        };

    }, [
        clearSession,
    ]);


    /**
     * =========================================================================
     * LOGIN
     * =========================================================================
     */

    const login =
        useCallback(
            (
                jwt: string,
                userData?: AuthUser
            ): void => {

                /**
                 * ------------------------------------------------------------
                 * VALIDATE TOKEN
                 * ------------------------------------------------------------
                 */

                if (
                    !jwt ||
                    typeof jwt !== "string" ||
                    !jwt.trim()
                ) {

                    throw new Error(
                        "Authentication token is required."
                    );

                }


                try {

                    /**
                     * --------------------------------------------------------
                     * DECODE TOKEN
                     * --------------------------------------------------------
                     */

                    const decoded =
                        jwtDecode<AuthUser>(
                            jwt
                        );


                    /**
                     * --------------------------------------------------------
                     * CHECK EXPIRATION
                     * --------------------------------------------------------
                     */

                    if (
                        decoded.exp !== undefined &&
                        decoded.exp !== null &&
                        decoded.exp * 1000 <= Date.now()
                    ) {

                        throw new Error(
                            "Authentication token has expired."
                        );

                    }


                    /**
                     * --------------------------------------------------------
                     * MERGE USER INFORMATION
                     * --------------------------------------------------------
                     */

                    const mergedUser: AuthUser = {

                        ...decoded,

                        ...(userData || {}),

                    };


                    /**
                     * --------------------------------------------------------
                     * NORMALIZE USER
                     * --------------------------------------------------------
                     */

                    const finalUser =
                        normalizeUser(
                            mergedUser
                        );


                    /**
                     * --------------------------------------------------------
                     * RESOLVE ROLE
                     * --------------------------------------------------------
                     */

                    const primaryRole =
                        resolvePrimaryRole(
                            finalUser
                        );


                    const normalizedRole =
                        normalizeRole(
                            primaryRole
                        );


                    /**
                     * --------------------------------------------------------
                     * PERSIST SESSION
                     * --------------------------------------------------------
                     */

                    localStorage.setItem(
                        AUTH_TOKEN_KEY,
                        jwt
                    );


                    localStorage.setItem(
                        AUTH_USER_KEY,
                        JSON.stringify(
                            finalUser
                        )
                    );


                    localStorage.setItem(
                        AUTH_ROLE_KEY,
                        normalizedRole
                    );


                    localStorage.setItem(
                        AUTH_PASSWORD_CHANGE_KEY,
                        String(
                            finalUser.mustChangePassword ??
                            false
                        )
                    );


                    /**
                     * --------------------------------------------------------
                     * UPDATE REACT STATE
                     * --------------------------------------------------------
                     */

                    setToken(
                        jwt
                    );


                    setUser(
                        finalUser
                    );


                    /**
                     * Authentication restoration is complete after login.
                     */
                    setLoading(
                        false
                    );


                    if (
                        import.meta.env.DEV
                    ) {

                        console.info(
                            "[AuthContext] Authentication login successful."
                        );

                    }

                } catch (error) {

                    console.error(
                        "[AuthContext] Authentication login failed.",
                        error
                    );


                    clearSession();


                    throw error;

                }

            },
            [
                clearSession,
            ]
        );


    /**
     * =========================================================================
     * LOGOUT
     * =========================================================================
     */

    const logout =
        useCallback(
            (): void => {

                clearSession();

                setLoading(false);


                if (
                    import.meta.env.DEV
                ) {

                    console.info(
                        "[AuthContext] User logged out."
                    );

                }

            },
            [
                clearSession,
            ]
        );


    /**
     * =========================================================================
     * ROLE VALIDATION
     * =========================================================================
     */

    const hasRole =
        useCallback(
            (
                requiredRole: string
            ): boolean => {

                if (
                    !user ||
                    !requiredRole
                ) {

                    return false;

                }


                const wantedRole =
                    normalizeRole(
                        requiredRole
                    );


                if (!wantedRole) {
                    return false;
                }


                /**
                 * Primary role.
                 */
                if (
                    normalizeRole(
                        user.role
                    ) === wantedRole
                ) {

                    return true;

                }


                /**
                 * Multiple roles.
                 */
                if (
                    user.roles?.some(
                        role =>
                            normalizeRole(
                                role
                            ) === wantedRole
                    )
                ) {

                    return true;

                }


                /**
                 * Spring Security authorities.
                 */
                if (
                    user.authorities?.some(
                        authority =>
                            normalizeRole(
                                authority
                            ) === wantedRole
                    )
                ) {

                    return true;

                }


                return false;

            },
            [
                user,
            ]
        );


    /**
     * =========================================================================
     * CONTEXT VALUE
     * =========================================================================
     */

    const contextValue =
        useMemo<AuthContextType>(
            () => ({

                user,

                token,

                loading,

                isAuthenticated:
                    Boolean(
                        token &&
                        user
                    ),

                login,

                logout,

                hasRole,

            }),
            [
                user,
                token,
                loading,
                login,
                logout,
                hasRole,
            ]
        );


    /**
     * =========================================================================
     * PROVIDER
     * =========================================================================
     */

    return (

        <AuthContext.Provider
            value={contextValue}
        >

            {children}

        </AuthContext.Provider>

    );

}


/**
 * ============================================================================
 * useAuth HOOK
 * ============================================================================
 *
 * This is the hook required by:
 *
 *     useDocuments.ts
 *
 * Example:
 *
 *     const {
 *         user,
 *         token,
 *         loading,
 *         isAuthenticated,
 *     } = useAuth();
 *
 * ============================================================================
 */

export function useAuth(): AuthContextType {

    const context =
        useContext(
            AuthContext
        );


    if (!context) {

        throw new Error(
            "useAuth must be used within an AuthProvider."
        );

    }


    return context;

}