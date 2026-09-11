import {
  UploadCloud,
  BrainCircuit,
  FileCheck,
  ShieldAlert,
  UserRound,
} from "lucide-react";
import { Link } from "react-router-dom";


const steps = [
  {
    icon: UploadCloud,
    title:"Upload Documents",
    text:"Upload passports, visas, certificates and supporting files securely.",
  },
  {
    icon: BrainCircuit,
    title:"AI Analysis",
    text:"Our AI extracts information and analyses document accuracy.",
  },
  {
    icon: ShieldAlert,
    title:"Risk Detection",
    text:"Identify missing information and possible inconsistencies.",
  },
  {
    icon: FileCheck,
    title:"Generate Report",
    text:"Receive clear verification insights and recommendations.",
  },
];


export default function DemoSection(){

return (

<section
className="
py-24
bg-[#071426]
text-white
"
>

<div
className="
max-w-7xl
mx-auto
px-6
"
>

<div className="text-center">

<h2
className="
text-4xl
md:text-5xl
font-bold
"
>
How MukondoGTech AI Works
</h2>


<p
className="
mt-5
text-slate-300
max-w-2xl
mx-auto
"
>
From document upload to intelligent verification,
our AI simplifies the entire process.
</p>

</div>



<div
className="
mt-14
grid
grid-cols-1
md:grid-cols-4
gap-6
"
>

{steps.map((step)=>(

<div
key={step.title}
className="
bg-white/10
border
border-white/10
rounded-3xl
p-6
"
>

<step.icon
size={30}
className="text-[#C6A15B]"
/>


<h3
className="
mt-5
font-semibold
text-xl
"
>
{step.title}
</h3>


<p
className="
mt-3
text-slate-300
"
>
{step.text}
</p>


</div>

))}

</div>

<div
className="
mt-10
flex
flex-col
items-center
justify-center
gap-4
rounded-2xl
border
border-white/10
bg-white/5
px-6
py-5
text-center
sm:flex-row
sm:text-left
"
>

<div className="flex -space-x-3">
{[0, 1].map((i) => (
<span
key={i}
className="
flex
h-10
w-10
items-center
justify-center
rounded-full
border-2
border-[#071426]
bg-gradient-to-br
from-[#D4B984]
to-[#C6A15B]
text-[#071426]
"
>
<UserRound size={18} />
</span>
))}
</div>

<p className="text-sm text-slate-300">
Prefer a human check? A document specialist can review any
flagged result with you —{" "}
<Link
to="/contact"
className="font-semibold text-[#C6A15B] underline-offset-4 hover:underline"
>
talk to our team
</Link>
</p>

</div>

</div>

</section>

);

}