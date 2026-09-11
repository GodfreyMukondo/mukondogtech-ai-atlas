import {
createContext,
useContext,
useState,
} from "react";

import type {
ReactNode
} from "react";

import {
CheckCircle,
AlertCircle,
Info
} from "lucide-react";



type ToastType=
"success"
|
"error"
|
"info";



interface Toast{

id:number;

message:string;

type:ToastType;

}



interface Context{

showToast:
(
message:string,
type?:ToastType
)=>void;

}



const ToastContext=
createContext<Context|null>(null);



export function ToastProvider({

children

}:{children:ReactNode}){


const [toasts,setToasts]=useState<Toast[]>([]);



function showToast(

message:string,

type:ToastType="info"

){


const id=Date.now();



setToasts(prev=>[
...prev,
{
id,
message,
type
}
]);



setTimeout(()=>{

setToasts(prev=>
prev.filter(
t=>t.id!==id
)
);

},4000);


}



return (

<ToastContext.Provider
value={{
showToast
}}
>


{children}



<div className="
fixed
top-5
right-5
space-y-3
z-50
">

{

toasts.map(toast=>(


<div

key={toast.id}

className="
flex
items-center
gap-3
rounded-xl
border
border-white/10
bg-[#1F314A]
shadow-2xl
shadow-black/40
backdrop-blur-xl
px-4
py-3
"

>


{

toast.type==="success"
?
<CheckCircle className="text-green-500"/>

:

toast.type==="error"
?

<AlertCircle className="text-red-500"/>

:

<Info className="text-blue-500"/>

}


<span className="
text-sm
font-medium
text-white
">

{toast.message}

</span>


</div>


))

}

</div>



</ToastContext.Provider>

);


}



export function useToast(){

const context=
useContext(ToastContext);


if(!context){

throw new Error(
"useToast must be inside ToastProvider"
);

}


return context;

}