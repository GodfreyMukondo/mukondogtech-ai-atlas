import {
  useEffect,
} from "react";

import {
  AnimatePresence,
  motion,
} from "framer-motion";

import {
  AlertTriangle,
  CheckCircle,
  XCircle,
} from "lucide-react";


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

<AnimatePresence>

{open && (

<div
role="dialog"
aria-modal="true"
className="
fixed
inset-0
z-50
flex
items-center
justify-center
p-4
"
>

<motion.div
initial={{opacity:0}}
animate={{opacity:1}}
exit={{opacity:0}}
transition={{duration:0.15}}
onClick={onCancel}
className="
fixed
inset-0
bg-black/40
backdrop-blur-sm
"
/>

<motion.div
initial={{opacity:0, scale:0.95}}
animate={{opacity:1, scale:1}}
exit={{opacity:0, scale:0.95}}
transition={{duration:0.2}}
className="
relative
w-full
max-w-md
rounded-2xl
border
border-white/10
bg-[#1F314A]
p-6
shadow-2xl
shadow-black/40
backdrop-blur-xl
"
>


<div className="
flex
gap-3
items-center
">

{icons[variant]}

<h2
className="
text-lg
font-semibold
text-white
"
>

{title}

</h2>

</div>


<p className="
mt-4
text-slate-300
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
border-white/15
text-slate-200
hover:bg-white/10
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


</motion.div>


</div>

)}

</AnimatePresence>


);


}
