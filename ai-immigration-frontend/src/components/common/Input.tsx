import type {
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
text-slate-200
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
bg-white/5
text-white
outline-none
transition
placeholder:text-slate-500

${
error
?
"border-red-400 focus:ring-red-300"
:
"border-white/15 focus:ring-[#C6A15B]"
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