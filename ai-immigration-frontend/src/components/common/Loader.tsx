import {
LoaderCircle
} from "lucide-react";


interface LoaderProps {

size?:number;

text?:string;

}


export default function Loader({

size=30,
text

}:LoaderProps){


return (

<div

className="
flex
flex-col
items-center
justify-center
gap-3
"

>


<LoaderCircle

size={size}

className="
animate-spin
text-[#C6A15B]
"

/>


{
text && (

<p
className="
text-slate-400
"
>

{text}

</p>

)
}


</div>

);

}