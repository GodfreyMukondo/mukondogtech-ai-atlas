import {
Settings,
} from "lucide-react";


export default function MaintenancePage(){


return (

<div
className="
min-h-screen
bg-[#F8F6F1]
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
text-[#F4B81A]
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
text-gray-600
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