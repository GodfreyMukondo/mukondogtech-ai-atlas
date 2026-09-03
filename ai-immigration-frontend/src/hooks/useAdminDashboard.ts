import {
useEffect,
useState,
} from "react";


import {
getAdminDashboard,
} from "../features/documents/services/adminService";


import type {
  AdminDashboardResponse
} from "../types/admin-dashboard";


export function useAdminDashboard(){


const [data,setData]=
useState<AdminDashboardResponse|null>(null);



const [loading,setLoading]=
useState(true);



const [error,setError]=
useState<string|null>(null);





const loadDashboard =
async()=>{


try{


setLoading(true);


setError(null);



const result =
await getAdminDashboard();



setData(result);



}
catch(error:any){


setError(
error?.response?.data?.message
||
"Failed loading dashboard"
);


}

finally{


setLoading(false);


}



};






useEffect(()=>{


loadDashboard();


},[]);






return {


data,

loading,

error,

refresh:loadDashboard,


};


}