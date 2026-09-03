import api from "@/lib/api";


export const subscriptionService={


getSubscription(){

return api.get(
"/subscriptions/current"
);

},


subscribe(plan:string){

return api.post(
"/subscriptions",
{
plan
}
);

},


cancel(){

return api.post(
"/subscriptions/cancel"
);

}


};