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
text-[#F4B81A]
"

/>


{
text && (

<p
className="
text-gray-500
"
>

{text}

</p>

)
}


</div>

);

}