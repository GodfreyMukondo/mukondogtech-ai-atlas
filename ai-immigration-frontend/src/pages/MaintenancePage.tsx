import {
Settings,
} from "lucide-react";


export default function MaintenancePage(){


return (

<div
className="
min-h-screen
flex
items-center
justify-center
text-center
p-6
"
>


<div>


<Settings
size={70}
className="
mx-auto
text-[#C6A15B]
"
/>


<h1
className="
text-5xl
font-bold
mt-6
"
>
Under Maintenance
</h1>


<p
className="
text-slate-300
mt-4
max-w-md
"
>
MukondoGTech AI Platform is temporarily unavailable while we perform
important improvements.
</p>


</div>


</div>

);

}