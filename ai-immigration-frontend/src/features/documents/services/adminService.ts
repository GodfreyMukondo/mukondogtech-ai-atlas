import API from "../../../api/axios";

import type {
  AdminDashboardResponse
} from "../../../types/admin-dashboard";



export const getAdminDashboard =
async()=>{


const response =
await API.get<AdminDashboardResponse>(
"/admin/dashboard"
);



return response.data;


};