import {
  useEffect,
} from "react";

import {
  motion,
  AnimatePresence,
} from "framer-motion";

import {
  AlertTriangle,
  Trash2,
  X,
} from "lucide-react";


interface DocumentDeleteConfirmModalProps {

  open:boolean;

  documentName:string;

  loading?:boolean;

  onConfirm:()=>void;

  onCancel:()=>void;

}



export default function DocumentDeleteConfirmModal({

  open,

  documentName,

  loading=false,

  onConfirm,

  onCancel,

}:DocumentDeleteConfirmModalProps){



useEffect(()=>{


const handleEscape = (
event:KeyboardEvent
)=>{


if(
event.key==="Escape" &&
open
){

onCancel();

}


};



window.addEventListener(
"keydown",
handleEscape
);



return()=>{

window.removeEventListener(
"keydown",
handleEscape
);

};


},[
open,
onCancel
]);



return (

<AnimatePresence>


{

open && (

<motion.div

initial={{
opacity:0
}}

animate={{
opacity:1
}}

exit={{
opacity:0
}}

className="
fixed
inset-0
z-50
flex
items-center
justify-center
bg-black/40
backdrop-blur-sm
p-4
"

>


<motion.div

initial={{
scale:0.95,
opacity:0
}}

animate={{
scale:1,
opacity:1
}}

exit={{
scale:0.95,
opacity:0
}}

className="
bg-white
w-full
max-w-md
rounded-2xl
shadow-xl
p-6
"


>


<div className="
flex
justify-between
items-start
">


<div className="
flex
gap-3
items-center
">


<div className="
p-3
rounded-full
bg-red-100
">

<Trash2
className="
text-red-600
"
/>

</div>


<div>

<h2 className="
text-lg
font-semibold
text-[#0B1736]
">

Delete Document

</h2>


<p className="
text-sm
text-gray-500
mt-1
">

This action cannot be undone.

</p>


</div>


</div>



<button

onClick={onCancel}

disabled={loading}

className="
text-gray-400
hover:text-gray-700
"

>

<X size={20}/>

</button>


</div>




<div className="
mt-5
flex
gap-3
bg-red-50
rounded-xl
p-4
">


<AlertTriangle
className="
text-red-500
shrink-0
"
/>


<p className="
text-sm
text-red-700
">

You are about to permanently delete:

<br/>


<strong>
{documentName}
</strong>


</p>


</div>




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
hover:bg-gray-100
"

>

Cancel

</button>



<button

onClick={onConfirm}

disabled={loading}

className="
px-4
py-2
rounded-lg
bg-red-600
text-white
hover:bg-red-700
disabled:opacity-50
"

>


{

loading
?
"Deleting..."
:
"Delete"

}


</button>


</div>



</motion.div>


</motion.div>


)

}


</AnimatePresence>


);


}