import {
ReactNode
} from "react";


interface EmptyStateProps {

title:string;

description?:string;

icon?:ReactNode;

action?:ReactNode;

}


export default function EmptyState({

title,
description,
icon,
action

}:EmptyStateProps){


return (

<div

className="
flex
flex-col
items-center
justify-center
text-center
py-16
px-6
"

>


{
icon && (

<div
className="
mb-5
text-[#F4B81A]
"
>

{icon}

</div>

)
}



<h3

className="
text-xl
font-bold
text-[#0B1736]
"

>

{title}

</h3>



{
description && (

<p
className="
mt-2
text-gray-500
max-w-md
"
>

{description}

</p>

)

}



{
action && (

<div className="mt-6">

{action}

</div>

)

}


</div>

);

}