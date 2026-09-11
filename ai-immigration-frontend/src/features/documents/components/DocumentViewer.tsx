import {
  useState,
} from "react";

import {
  FileText,
  Download,
  ExternalLink,
  AlertCircle,
  Loader2,
} from "lucide-react";



interface Props {

  url:string;

  fileName:string;

  fileType?:string;

}




export default function DocumentViewer({

  url,

  fileName,

  fileType,

}:Props){



const [loading,setLoading] =
useState(true);


const [error,setError] =
useState(false);




const extension =
fileName
.split(".")
.pop()
?.toLowerCase();




const isImage =

fileType?.startsWith("image/")
||
[
"png",
"jpg",
"jpeg",
"webp"
]
.includes(extension ?? "");



const isPdf =

fileType==="application/pdf"
||
extension==="pdf";





const handleLoad = ()=>{

setLoading(false);

};




const handleError = ()=>{

setLoading(false);

setError(true);

};





return (

<div

className="
border
border-white/10
bg-white/5
backdrop-blur-xl
rounded-3xl
overflow-hidden
shadow-2xl
shadow-black/20
"

>


{/* Header */}

<div

className="
flex
items-center
justify-between
px-6
py-4
border-b
border-white/10
"

>


<div

className="
flex
items-center
gap-3
min-w-0
"

>


<div

className="
p-2
rounded-lg
bg-[#C6A15B]/15
"

>

<FileText

size={20}

className="
text-[#C6A15B]
"

/>


</div>



<div className="
truncate
"

>


<h3

className="
font-semibold
text-white
truncate
"

>

{fileName}

</h3>



<p

className="
text-xs
text-slate-400
"

>

Document Preview

</p>



</div>



</div>






<div

className="
flex
gap-3
"

>


<a

href={url}

target="_blank"

rel="noopener noreferrer"

className="
p-2
rounded-lg
hover:bg-white/10
text-slate-300
hover:text-white
"

title="Open document"

>

<ExternalLink size={18}/>

</a>




<a

href={url}

download={fileName}

className="
p-2
rounded-lg
hover:bg-white/10
text-slate-300
hover:text-white
"

title="Download document"

>

<Download size={18}/>

</a>



</div>



</div>







{/* Viewer */}


<div

className="
relative
bg-black/20
min-h-[500px]
"

>




{

loading && (

<div

className="
absolute
inset-0
flex
items-center
justify-center
z-10
bg-[#0B1F3A]/80
"

>

<Loader2

className="
animate-spin
text-[#C6A15B]
"

size={32}

/>


</div>

)

}






{

error ?


(

<div

className="
h-[600px]
flex
flex-col
items-center
justify-center
text-center
p-6
"

>


<AlertCircle

size={40}

className="
text-red-400
mb-4
"

/>



<h3

className="
font-semibold
text-white
"

>

Unable to preview document

</h3>



<p

className="
text-slate-400
mt-2
text-sm
"

>

Please download the file to view it.

</p>



</div>

)



:



isImage ?


(

<img

src={url}

alt={fileName}

onLoad={handleLoad}

onError={handleError}

className="
w-full
max-h-[700px]
object-contain
p-6
"

/>

)



:


isPdf ?


(

<iframe

src={url}

title={fileName}

loading="lazy"

onLoad={handleLoad}

onError={handleError}

sandbox="
allow-same-origin
allow-scripts
allow-downloads
"

className="
w-full
h-[700px]
"

>

</iframe>

)



:


(

<div

className="
h-[600px]
flex
flex-col
items-center
justify-center
"

>


<FileText

size={45}

className="
text-slate-500
"

/>



<p

className="
mt-4
text-slate-400
"

>

Preview not available for this file type.

</p>


</div>


)


}




</div>



</div>


);


}