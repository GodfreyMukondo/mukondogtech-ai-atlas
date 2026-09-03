import axios, { AxiosError } from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:5000/api";

const billingClient = axios.create({
  baseURL: `${API_BASE_URL}/billing`,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

billingClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export interface SubscriptionPlan {
  id: string;
  name: string;
  description: string;
  price: number;
  currency: string;
  interval: "monthly" | "yearly";
  features: string[];
}

export interface Subscription {
  id: string;
  status:
    | "active"
    | "trialing"
    | "past_due"
    | "cancelled"
    | "expired";
  currentPeriodStart: string;
  currentPeriodEnd: string;
  plan: SubscriptionPlan;
}

export interface Invoice {
  id: string;
  amount: number;
  currency: string;
  status: string;
  invoiceUrl?: string;
  createdAt: string;
}

export interface CheckoutSession {
  checkoutUrl: string;
}

class BillingApi {
  async getPlans(): Promise<SubscriptionPlan[]> {
    const response = await billingClient.get("/plans");
    return response.data.data;
  }

  async getCurrentSubscription(): Promise<Subscription> {
    const response = await billingClient.get("/subscription");
    return response.data.data;
  }

  async createCheckoutSession(
    planId: string
  ): Promise<CheckoutSession> {
    const response = await billingClient.post("/checkout", {
      planId,
    });

    return response.data.data;
  }

  async cancelSubscription(): Promise<void> {
    await billingClient.post("/subscription/cancel");
  }

  async reactivateSubscription(): Promise<void> {
    await billingClient.post("/subscription/reactivate");
  }

  async getBillingHistory(): Promise<Invoice[]> {
    const response = await billingClient.get("/invoices");
    return response.data.data;
  }

  async downloadInvoice(invoiceId: string): Promise<Blob> {
    const response = await billingClient.get(
      `/invoices/${invoiceId}/download`,
      {
        responseType: "blob",
      }
    );

    return response.data;
  }

  async openCustomerPortal(): Promise<string> {
    const response = await billingClient.post(
      "/customer-portal"
    );

    return response.data.data.portalUrl;
  }
}

export const billingApi = new BillingApi();

export const getApiErrorMessage = (
  error: unknown
): string => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<any>;

    return (
      axiosError.response?.data?.message ||
      axiosError.message ||
      "Unexpected billing error"
    );
  }

  return "Unexpected billing error";
};