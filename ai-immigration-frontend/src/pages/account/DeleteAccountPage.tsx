import {
AlertTriangle,
} from "lucide-react";


export default function DeleteAccountPage(){


return (

<div
className="
min-h-screen
bg-[#F8F6F1]
p-6
"
>


<div
className="
max-w-xl
mx-auto
bg-white
rounded-3xl
border
p-8
"
>


<AlertTriangle
size={50}
className="
text-red-600
"
/>


<h1
className="
text-3xl
font-bold
mt-5
"
>
Delete Account
</h1>


<p
className="
text-gray-600
mt-4
"
>
Deleting your account permanently removes your profile,
documents, and subscription information.
</p>



<button
className="
mt-8
bg-red-600
text-white
px-6
py-3
rounded-xl
"
>
Permanently Delete Account
</button>


</div>


</div>

);

}