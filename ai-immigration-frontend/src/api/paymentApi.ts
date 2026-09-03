import API from "./axios";


export interface PaymentRequest {

  applicationId:number;

  amount:number;

  currency:string;

}



export interface PaymentResponse {

  paymentId:string;

  paymentUrl:string;

  status:string;

}



export const createPaymentApi = async (
data:PaymentRequest
):Promise<PaymentResponse>=>{


const response =
await API.post(
"/payments/create",
data
);


return response.data;

};



export const verifyPaymentApi = async (
paymentId:string
)=>{


const response =
await API.get(
`/payments/verify/${paymentId}`
);


return response.data;

};



export const getPaymentsApi = async()=>{


const response =
await API.get(
"/payments"
);


return response.data;

};