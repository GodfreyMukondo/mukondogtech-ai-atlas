export interface DocumentAnalytics {
  totalDocuments: number;
  uploadedToday: number;
  uploadedThisMonth: number;
  uploadTrend: AnalyticsPoint[];
}

export interface ChatAnalytics {
  totalQueries: number;
  queriesToday: number;
  averageResponseTime: number;
  queryTrend: AnalyticsPoint[];
}

export interface ProcessingAnalytics {
  averageProcessingTime: number;
  successfulAnalyses: number;
  failedAnalyses: number;
  accuracyRate: number;
}

export interface AnalyticsPoint {
  label: string;
  value: number;
}

export interface AnalyticsOverview {
  documents: DocumentAnalytics;
  chat: ChatAnalytics;
  processing: ProcessingAnalytics;
}