import {
  fetchAnalyticsOverview,
  fetchDocumentAnalytics,
  fetchChatAnalytics,
  fetchProcessingAnalytics,
} from "../../api/analyticsApi";


import type {
  AnalyticsOverview,
  DocumentAnalytics,
  ChatAnalytics,
  ProcessingAnalytics,
} from "../../types/analytics";





export const getAnalyticsOverview =
async (): Promise<AnalyticsOverview> => {

  return await fetchAnalyticsOverview();

};






export const getDocumentAnalytics =
async (): Promise<DocumentAnalytics> => {

  return await fetchDocumentAnalytics();

};






export const getChatAnalytics =
async (): Promise<ChatAnalytics> => {

  return await fetchChatAnalytics();

};






export const getProcessingAnalytics =
async (): Promise<ProcessingAnalytics> => {

  return await fetchProcessingAnalytics();

};