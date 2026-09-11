import {
useState
} from "react";


const options=[

"Email notifications",

"Document analysis updates",

"Security alerts",

"Marketing updates"

];


export default function NotificationSettingsPage(){


const [settings,setSettings]=useState(
options.map(()=>true)
);



return (

<div
className="
min-h-screen
p-6
text-slate-100
"
>


<div
className="
max-w-4xl
mx-auto
"
>


<h1
className="
text-4xl
font-bold
"
>
Notification Settings
</h1>


<p
className="
text-slate-300
mt-2
mb-10
"
>
Control how MukondoGTech communicates with you.
</p>




<div
className="
border
border-white/10
bg-white/5
backdrop-blur-xl
rounded-3xl
p-8
space-y-6
"
>


{
options.map(
(item,index)=>(


<div
key={item}
className="
flex
justify-between
items-center
"
>


<span className="text-slate-200">
{item}
</span>


<button

onClick={()=>{

const copy=[...settings];

copy[index]=!copy[index];

setSettings(copy);

}}

className={`
w-14
h-7
rounded-full
transition
${
settings[index]
?
"bg-[#C6A15B]"
:
"bg-white/15"
}
`}

>


<div
className={`
w-6
h-6
bg-white
rounded-full
transition
${
settings[index]
?
"translate-x-7"
:
""
}
`}
/>


</button>


</div>


)
)

}


</div>


</div>


</div>

);

}