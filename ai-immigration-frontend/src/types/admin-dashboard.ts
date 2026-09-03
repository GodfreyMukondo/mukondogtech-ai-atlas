import type {
  LucideIcon,
} from "lucide-react";



/*
|--------------------------------------------------------------------------
| KPI
|--------------------------------------------------------------------------
*/

export interface DashboardKPI {

  title:string;

  value:string;

  change:string;

  icon:LucideIcon;

}




/*
|--------------------------------------------------------------------------
| GENERIC METRIC
|--------------------------------------------------------------------------
*/

export interface DashboardMetric {

  label:string;

  value:string | number;

}




/*
|--------------------------------------------------------------------------
| DOCUMENT METRICS
|--------------------------------------------------------------------------
*/

export interface DocumentMetric {

  title:string;

  value:string | number;

}




/*
|--------------------------------------------------------------------------
| OPERATIONS
|--------------------------------------------------------------------------
*/

export interface OperationsMetrics {


  pendingApplications:number;


  approvedCases:number;


  rejectedApplications:number;


  awaitingDocuments:number;


  slaBreaches:number;

}





/*
|--------------------------------------------------------------------------
| AI METRICS
|--------------------------------------------------------------------------
*/

export interface AIMetrics {


  requestsProcessed:number;


  accuracy:number;


  hallucinationRate:number;


  averageResponseTime:string;


  activeModels:number;

}





/*
|--------------------------------------------------------------------------
| SECURITY METRICS
|--------------------------------------------------------------------------
*/

export interface SecurityMetrics {


  alerts:number;


  blockedAttacks:number;


  failedLogins:number;


  status:string;

}





/*
|--------------------------------------------------------------------------
| HEALTH
|--------------------------------------------------------------------------
*/

export interface HealthMetric {


  title:string;


  value:string;


  status:string;


  icon?:LucideIcon;

}





/*
|--------------------------------------------------------------------------
| AUDIT LOGS
|--------------------------------------------------------------------------
*/

export interface AuditLog {


  id:number;


  message:string;


  time:string;

}





/*
|--------------------------------------------------------------------------
| ALERTS
|--------------------------------------------------------------------------
*/

export interface AlertItem {


  id:number;


  message:string;


  time?:string;

}





/*
|--------------------------------------------------------------------------
| EXECUTIVE SUMMARY
|--------------------------------------------------------------------------
*/

export interface ExecutiveMetrics {


  activeUsers:number;


  activeUsersChange:number;


  activeCases:number;


  activeCasesChange:number;


  monthlyRevenue:number;


  revenueChange:number;


  aiAccuracy:number;


  aiAccuracyChange:number;

}





/*
|--------------------------------------------------------------------------
| DOCUMENT INTELLIGENCE
|--------------------------------------------------------------------------
*/

export interface DocumentIntelligence {


  ocrAccuracy:number;


  fraudAlerts:number;


  averageProcessingTime:string;


  successRate:number;

}





/*
|--------------------------------------------------------------------------
| PLATFORM STATUS
|--------------------------------------------------------------------------
*/

export interface PlatformStatus {


  status:
  | "ONLINE"
  | "DEGRADED"
  | "OFFLINE";


  aiModels:number;


  uptime:number;

}





/*
|--------------------------------------------------------------------------
| COMPLETE ADMIN DASHBOARD RESPONSE
|--------------------------------------------------------------------------
*/

export interface AdminDashboardResponse {


  executive:ExecutiveMetrics;



  platform:PlatformStatus;



  kpis:DashboardKPI[];



  operations:OperationsMetrics;



  ai:AIMetrics;



  security:SecurityMetrics;



  documents:DocumentIntelligence;



  documentMetrics:DocumentMetric[];



  health:HealthMetric[];



  auditLogs:AuditLog[];



  alerts:AlertItem[];


}