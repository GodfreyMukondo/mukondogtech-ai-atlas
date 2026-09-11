import {
  UploadCloud,
  CheckCircle,
  XCircle,
} from "lucide-react";


type UploadStatus =
"uploading"
|
"success"
|
"error";


interface UploadProgressBarProps {


fileName:string;

progress:number;

status?:UploadStatus;

errorMessage?:string;

onCancel?:()=>void;


}



export default function UploadProgressBar({

fileName,

progress,

status="uploading",

errorMessage,

onCancel,

}:UploadProgressBarProps){



return (

<div className="
w-full
rounded-xl
border
border-white/10
bg-white/5
backdrop-blur-xl
p-4
shadow-lg
shadow-black/20
">


<div className="
flex
justify-between
items-center
">


<div className="
flex
items-center
gap-3
">


{

status==="uploading" &&

<UploadCloud
className="
text-[#C6A15B]
"
/>

}



{

status==="success" &&

<CheckCircle
className="
text-emerald-400
"
/>

}



{

status==="error" &&

<XCircle
className="
text-red-400
"
/>

}



<div>


<p className="
text-sm
font-medium
text-white
truncate
max-w-xs
">

{fileName}

</p>


<p className="
text-xs
text-slate-400
">

{

status==="uploading"
?
`${progress}% uploaded`

:

status==="success"
?
"Upload completed"

:
errorMessage || "Upload failed"

}

</p>


</div>


</div>




{

status==="uploading" && onCancel && (

<button

onClick={onCancel}

className="
text-sm
text-red-400
hover:underline
"

>

Cancel

</button>


)


}


</div>




<div className="
mt-4
h-2
bg-white/10
rounded-full
overflow-hidden
">


<div

className="
h-full
bg-[#C6A15B]
transition-all
duration-300
"

style={{

width:`${progress}%`

}}


/>


</div>


</div>


);


}