import type {
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
text-[#C6A15B]
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
text-white
"

>

{title}

</h3>



{
description && (

<p
className="
mt-2
text-slate-400
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