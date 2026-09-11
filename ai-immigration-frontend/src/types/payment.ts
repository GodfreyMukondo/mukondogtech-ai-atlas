import {
createPaymentApi,
verifyPaymentApi,
getPaymentsApi,
} from "../api/paymentApi";

import type {
PaymentRequest
} from "../api/paymentApi";



export const createPayment = async (
data:PaymentRequest
)=>{


return await createPaymentApi(data);


};



export const verifyPayment = async (
paymentId:string
)=>{


return await verifyPaymentApi(
paymentId
);


};



export const getPaymentHistory = async()=>{


return await getPaymentsApi();

};