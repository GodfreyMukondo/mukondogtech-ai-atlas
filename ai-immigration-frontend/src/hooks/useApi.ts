import {
  useState,
} from "react";

import api from "../api/axios";

import type {
  AxiosRequestConfig,
} from "axios";



interface ApiState<T>{

 data:T|null;

 loading:boolean;

 error:string;

}



export default function useApi<T = any>(){


const [
state,
setState
]=useState<ApiState<T>>({

data:null,

loading:false,

error:""

});





const request = async(

method:
"get"
|
"post"
|
"put"
|
"delete"
|
"patch",

url:string,

body?:unknown,

config?:AxiosRequestConfig

):Promise<T | null>=>{


try{


setState({

data:null,

loading:true,

error:""

});



let response;



switch(method){


case "get":

response =
await api.get(
url,
config
);

break;



case "post":

response =
await api.post(
url,
body,
config
);

break;



case "put":

response =
await api.put(
url,
body,
config
);

break;



case "patch":

response =
await api.patch(
url,
body,
config
);

break;



case "delete":

response =
await api.delete(
url,
config
);

break;


}



setState({

data:response.data,

loading:false,

error:""

});



return response.data;


}

catch(error:any){


const message =

error?.response?.data?.message
||
"Something went wrong";



setState({

data:null,

loading:false,

error:message

});



return null;


}


};






return {


...state,


get:
(url:string,config?:AxiosRequestConfig)=>
request(
"get",
url,
undefined,
config
),


post:
(url:string,data?:unknown)=>
request(
"post",
url,
data
),


put:
(url:string,data?:unknown)=>
request(
"put",
url,
data
),


patch:
(url:string,data?:unknown)=>
request(
"patch",
url,
data
),


remove:
(url:string)=>
request(
"delete",
url
)


};


}