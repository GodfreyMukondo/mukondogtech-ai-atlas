import axios, { AxiosError } from "axios";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:5000/api";

const supportClient = axios.create({
  baseURL: `${API_BASE_URL}/support`,
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

supportClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export interface FAQ {
  id: string;
  question: string;
  answer: string;
  category: string;
}

export interface SupportTicket {
  id: string;
  subject: string;
  message: string;
  status:
    | "open"
    | "pending"
    | "resolved"
    | "closed";
  createdAt: string;
  updatedAt: string;
}

export interface CreateTicketPayload {
  subject: string;
  message: string;
  category?: string;
}

export interface ContactRequest {
  name: string;
  email: string;
  subject: string;
  message: string;
}

class SupportApi {
  async getFaqs(): Promise<FAQ[]> {
    const response = await supportClient.get("/faqs");
    return response.data.data;
  }

  async createTicket(
    payload: CreateTicketPayload
  ): Promise<SupportTicket> {
    const response = await supportClient.post(
      "/tickets",
      payload
    );

    return response.data.data;
  }

  async getMyTickets(): Promise<SupportTicket[]> {
    const response = await supportClient.get("/tickets");

    return response.data.data;
  }

  async getTicketById(
    ticketId: string
  ): Promise<SupportTicket> {
    const response = await supportClient.get(
      `/tickets/${ticketId}`
    );

    return response.data.data;
  }

  async closeTicket(ticketId: string): Promise<void> {
    await supportClient.patch(
      `/tickets/${ticketId}/close`
    );
  }

  async submitContactForm(
    payload: ContactRequest
  ): Promise<void> {
    await supportClient.post("/contact", payload);
  }

  async uploadTicketAttachment(
    ticketId: string,
    file: File
  ): Promise<void> {
    const formData = new FormData();

    formData.append("file", file);

    await supportClient.post(
      `/tickets/${ticketId}/attachments`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    );
  }
}

export const supportApi = new SupportApi();

export const getSupportErrorMessage = (
  error: unknown
): string => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<any>;

    return (
      axiosError.response?.data?.message ||
      axiosError.message ||
      "Unexpected support error"
    );
  }

  return "Unexpected support error";
};