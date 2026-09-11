import {
useEffect
} from "react";

import type {
ReactNode
} from "react";

import {
X
} from "lucide-react";


interface ModalProps {

open:boolean;

onClose:()=>void;

title?:string;

children:ReactNode;

}


export default function Modal({

open,
onClose,
title,
children

}:ModalProps){


useEffect(()=>{


const handler=(e:KeyboardEvent)=>{

if(e.key==="Escape"){
onClose();
}

};


window.addEventListener(
"keydown",
handler
);


return()=>{

window.removeEventListener(
"keydown",
handler
);

};


},[onClose]);



if(!open)
return null;



return (

<div

className="
fixed
inset-0
z-50
flex
items-center
justify-center
bg-black/40
px-6
"

onClick={onClose}

>


<div

className="
rounded-3xl
border
border-white/10
bg-[#1F314A]
max-w-lg
w-full
p-6
shadow-2xl
shadow-black/40
backdrop-blur-xl
"

onClick={
e=>e.stopPropagation()
}

>


<div
className="
flex
items-center
justify-between
mb-5
"
>


<h2
className="
text-xl
font-bold
text-white
"
>

{title}

</h2>


<button
onClick={onClose}
className="text-slate-400 hover:text-white"
>

<X/>

</button>


</div>


{children}


</div>


</div>

);

}