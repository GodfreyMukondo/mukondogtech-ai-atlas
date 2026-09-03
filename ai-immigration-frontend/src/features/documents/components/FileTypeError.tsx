import {
  FileWarning,
  Upload,
} from "lucide-react";


interface FileTypeErrorProps {


fileName?:string;

allowedTypes?:string[];

message?:string;

onRetry?:()=>void;


}



export default function FileTypeError({

fileName,

allowedTypes=[
"PDF",
"DOCX",
"JPG",
"PNG"
],

message,

onRetry,

}:FileTypeErrorProps){



return (

<div className="
rounded-xl
border
border-red-200
bg-red-50
p-5
">


<div className="
flex
gap-3
items-start
">


<div className="
p-2
rounded-lg
bg-red-100
">


<FileWarning

className="
text-red-600
"

/>


</div>



<div>


<h3 className="
font-semibold
text-red-700
">

Unsupported File Type

</h3>



<p className="
text-sm
text-red-600
mt-1
">

{

message ||

`${fileName || "This file"} cannot be uploaded.`

}

</p>



<p className="
text-xs
text-gray-600
mt-3
">

Supported formats:

<strong>
{" "}
{allowedTypes.join(", ")}
</strong>


</p>


</div>


</div>




{

onRetry && (

<button

onClick={onRetry}

className="
mt-4
inline-flex
items-center
gap-2
px-4
py-2
rounded-lg
bg-[#0B1736]
text-white
text-sm
hover:opacity-90
"

>


<Upload size={16}/>

Choose Another File


</button>


)


}


</div>


);


}