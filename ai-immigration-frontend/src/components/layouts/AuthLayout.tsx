import {
ReactNode
} from "react";

import {
Link,
Outlet
} from "react-router-dom";


import {
ScanSearch
} from "lucide-react";



interface Props {

children?:ReactNode;

}



export default function AuthLayout({

children

}:Props){


return (

<div

className="
min-h-screen
bg-[#F8F6F1]
flex
items-center
justify-center
px-6
"

>


<div

className="
w-full
max-w-md
"

>


<Link

to="/"

className="
flex
justify-center
items-center
gap-3
mb-8
"

>


<div

className="
w-12
h-12
rounded-xl
border
border-[#F4B81A]
flex
items-center
justify-center
"

>

<ScanSearch

className="
text-[#F4B81A]
"

/>

</div>



<h1

className="
text-2xl
font-bold
text-[#0B1736]
"

>

MukondoGTech

<span
className="
text-[#F4B81A]
"
>
AI
</span>


</h1>


</Link>



{
children ?? <Outlet/>
}


</div>


</div>

);

}