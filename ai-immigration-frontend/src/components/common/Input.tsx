import {
  InputHTMLAttributes,
} from "react";


interface InputProps
 extends InputHTMLAttributes<HTMLInputElement> {

 label?: string;

 error?: string;

}


export default function Input({

 label,
 error,
 className="",
 ...props

}: InputProps) {


return (

<div className="space-y-2">


{label && (

<label
className="
block
text-sm
font-medium
text-[#0B1736]
"
>

{label}

</label>

)}



<input

className={`
w-full
px-4
py-3
rounded-xl
border
outline-none
transition

${
error
?
"border-red-400 focus:ring-red-300"
:
"border-[#D9DDE5] focus:ring-[#F4B81A]"
}

focus:ring-2

${className}

`}

{...props}

/>



{
error && (

<p
className="
text-sm
text-red-500
"
>

{error}

</p>

)
}


</div>

);

}