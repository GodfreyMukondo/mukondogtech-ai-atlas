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
bg-white
p-4
shadow-sm
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
text-[#F4B81A]
"
/>

}



{

status==="success" &&

<CheckCircle
className="
text-green-600
"
/>

}



{

status==="error" &&

<XCircle
className="
text-red-600
"
/>

}



<div>


<p className="
text-sm
font-medium
text-[#0B1736]
truncate
max-w-xs
">

{fileName}

</p>


<p className="
text-xs
text-gray-500
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
text-red-600
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
bg-gray-200
rounded-full
overflow-hidden
">


<div

className="
h-full
bg-[#F4B81A]
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