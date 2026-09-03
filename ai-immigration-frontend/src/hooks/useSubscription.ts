import {
  useCallback,
  useEffect,
  useState,
} from "react";


import api from "../api/axios";


export interface Subscription {

  id:number;

  plan:
  "FREE"
  |
  "BASIC"
  |
  "PREMIUM"
  |
  "ENTERPRISE";


  status:
  "ACTIVE"
  |
  "CANCELLED"
  |
  "EXPIRED";


  startDate:string;

  endDate:string;

}



interface UseSubscriptionReturn {


subscription:
Subscription | null;


loading:boolean;


error:string;


refresh:()=>Promise<void>;


changePlan:
(
plan:string
)=>Promise<void>;


cancelSubscription:
()=>Promise<void>;


}




export default function useSubscription()
:UseSubscriptionReturn {



const [
subscription,
setSubscription
]=useState<Subscription|null>(null);



const [
loading,
setLoading
]=useState(false);



const [
error,
setError
]=useState("");






const refresh = useCallback(
async()=>{


try{


setLoading(true);

setError("");



const response =
await api.get(
"/subscriptions/current"
);



setSubscription(
response.data
);



}

catch(error:any){


setError(

error?.response?.data?.message
||
"Failed to load subscription."

);


}

finally{


setLoading(false);


}


},[]);







useEffect(()=>{


refresh();


},[refresh]);









const changePlan = async(
plan:string
)=>{


try{


setLoading(true);


await api.post(
"/subscriptions/change",
{
plan
}
);



await refresh();



}

catch(error:any){


setError(

error?.response?.data?.message
||
"Unable to change plan."

);


throw error;


}

finally{


setLoading(false);


}


};








const cancelSubscription =
async()=>{


try{


setLoading(true);



await api.post(
"/subscriptions/cancel"
);



await refresh();



}

catch(error:any){


setError(

error?.response?.data?.message
||
"Unable to cancel subscription."

);


throw error;


}

finally{


setLoading(false);


}


};








return {


subscription,

loading,

error,

refresh,

changePlan,

cancelSubscription


};


}