import API from "./axios";


export const fetchAnalyticsOverview = async () => {

  const response = await API.get(
    "/analytics/overview"
  );

  return response.data;

};


export const fetchDocumentAnalytics = async () => {

  const response = await API.get(
    "/analytics/documents"
  );

  return response.data;

};


export const fetchChatAnalytics = async () => {

  const response = await API.get(
    "/analytics/chat"
  );

  return response.data;

};


export const fetchProcessingAnalytics = async () => {

  const response = await API.get(
    "/analytics/processing"
  );

  return response.data;

};