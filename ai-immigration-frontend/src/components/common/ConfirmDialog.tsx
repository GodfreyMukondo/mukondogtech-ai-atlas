import {
  Fragment,
  useEffect,
} from "react";

import {
  Dialog,
  Transition,
} from "@headlessui/react";

import {
  AlertTriangle,
  CheckCircle,
  XCircle,
} from "lucide-react";

import {
  motion,
} from "framer-motion";


interface ConfirmDialogProps {

  open:boolean;

  title:string;

  description:string;

  confirmText?:string;

  cancelText?:string;

  variant?:
  | "danger"
  | "success"
  | "warning";

  loading?:boolean;

  onConfirm:()=>void;

  onCancel:()=>void;
}



export default function ConfirmDialog({

  open,

  title,

  description,

  confirmText="Confirm",

  cancelText="Cancel",

  variant="danger",

  loading=false,

  onConfirm,

  onCancel,

}:ConfirmDialogProps){


useEffect(()=>{

const handler=(e:KeyboardEvent)=>{

if(e.key==="Escape" && open){

onCancel();

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


},[open,onCancel]);



const icons={

danger:
<XCircle className="text-red-500"/>,

success:
<CheckCircle className="text-green-500"/>,

warning:
<AlertTriangle className="text-yellow-500"/>

};



return (

<Transition
show={open}
as={Fragment}
>

<Dialog
open={open}
onClose={onCancel}
className="relative z-50"
>


<Transition.Child

enter="ease-out duration-200"

enterFrom="opacity-0"

enterTo="opacity-100"

leave="ease-in duration-150"

leaveFrom="opacity-100"

leaveTo="opacity-0"

>

<div className="
fixed inset-0
bg-black/40
backdrop-blur-sm
"/>

</Transition.Child>



<div className="
fixed inset-0
flex
items-center
justify-center
p-4
">


<Transition.Child

enter="ease-out duration-200"

enterFrom="opacity-0 scale-95"

enterTo="opacity-100 scale-100"

leave="ease-in duration-150"

leaveFrom="opacity-100 scale-100"

leaveTo="opacity-0 scale-95"

>

<Dialog.Panel
as={motion.div}
className="
w-full
max-w-md
rounded-2xl
bg-white
p-6
shadow-xl
"
>


<div className="
flex
gap-3
items-center
">

{icons[variant]}

<Dialog.Title
className="
text-lg
font-semibold
text-[#0B1736]
"
>

{title}

</Dialog.Title>

</div>


<p className="
mt-4
text-gray-600
text-sm
">

{description}

</p>



<div className="
mt-6
flex
justify-end
gap-3
">


<button

onClick={onCancel}

disabled={loading}

className="
px-4
py-2
rounded-lg
border
text-gray-700
hover:bg-gray-100
"

>

{cancelText}

</button>



<button

onClick={onConfirm}

disabled={loading}

className={`
px-4
py-2
rounded-lg
text-white

${
variant==="danger"
?
"bg-red-600 hover:bg-red-700"
:
variant==="success"
?
"bg-green-600 hover:bg-green-700"
:
"bg-yellow-500 hover:bg-yellow-600"
}

`}

>


{
loading
?
"Processing..."
:
confirmText
}


</button>


</div>


</Dialog.Panel>


</Transition.Child>


</div>


</Dialog>


</Transition>


);


}