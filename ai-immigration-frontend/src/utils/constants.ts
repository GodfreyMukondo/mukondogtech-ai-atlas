export const USER_ROLES = {

  ADMIN: "ADMIN",

  USER: "USER",

  CONSULTANT: "CONSULTANT",

} as const;



export const DOCUMENT_TYPES = [

  "PASSPORT",

  "IDENTITY_DOCUMENT",

  "BANK_STATEMENT",

  "CERTIFICATE",

  "OTHER",

] as const;



export const APPLICATION_STATUS = {

  DRAFT: "DRAFT",

  SUBMITTED: "SUBMITTED",

  REVIEWING: "REVIEWING",

  APPROVED: "APPROVED",

  REJECTED: "REJECTED",

} as const;



export const PAGINATION = {

  DEFAULT_PAGE: 0,

  DEFAULT_SIZE: 10,

  MAX_SIZE: 100,

} as const;



export const API_ENDPOINTS = {

  AUTH: "/auth",

  USERS: "/users",

  DOCUMENTS: "/documents",

  APPLICATIONS: "/applications",

} as const;