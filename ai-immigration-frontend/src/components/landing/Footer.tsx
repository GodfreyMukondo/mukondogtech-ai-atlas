import {
  FaEnvelope,
  FaPhone,
  FaLinkedin,
  FaGithub,
  FaXTwitter,
  FaLocationDot,
  FaCopyright,
  FaFacebook,
  FaInstagram,
} from "react-icons/fa6";

import {
  BrainCircuit,
  ShieldCheck,
  Sparkles,
  ArrowRight,
  UserRound,
} from "lucide-react";


export default function Footer() {

  const currentYear = new Date().getFullYear();


  const platformLinks = [
    "AI Document Analysis",
    "Immigration Intelligence",
    "AI Assistant",
    "Document Verification",
    "Security & Privacy",
  ];


  const resourceLinks = [
    "Documentation",
    "Help Center",
    "Blog",
    "Pricing",
    "Contact",
  ];


  const socialLinks = [
    {
      icon:<FaFacebook/>,
      url:"https://facebook.com",
      label:"Facebook",
    },
    {
      icon:<FaInstagram/>,
      url:"https://instagram.com",
      label:"Instagram",
    },
    {
      icon:<FaLinkedin/>,
      url:"https://linkedin.com",
      label:"LinkedIn",
    },
    {
      icon:<FaGithub/>,
      url:"https://github.com",
      label:"Github",
    },
    {
      icon:<FaXTwitter/>,
      url:"https://x.com",
      label:"X",
    },
  ];



  return (

<footer
className="
relative
overflow-hidden
bg-gradient-to-br
from-[#111827]
via-[#1F2937]
to-[#064E3B]
text-white
"
>


{/* Decorative Glow */}

<div
className="
absolute
top-0
left-0
h-96
w-96
rounded-full
bg-emerald-400/20
blur-3xl
"
/>


<div
className="
absolute
right-0
bottom-0
h-80
w-80
rounded-full
bg-yellow-300/20
blur-3xl
"
/>



<div
className="
relative
w-full
px-6
py-16
"
>



{/* Newsletter */}

<div
className="
mb-16
rounded-3xl
border
border-white/10
bg-white/5
p-8
backdrop-blur-xl
"
>


<div
className="
flex
flex-col
gap-8
lg:flex-row
lg:items-center
lg:justify-between
"
>


<div>


<div
className="
flex
items-center
gap-3
mb-3
"
>

<Sparkles
size={22}
className="text-yellow-300"
/>


<h3
className="
text-2xl
font-bold
"
>
Stay Updated With AI Innovation
</h3>

</div>



<p
className="
max-w-xl
text-gray-300
"
>
Receive product updates, AI immigration
insights, and new platform features.
</p>


</div>



<div
className="
flex
w-full
max-w-md
rounded-xl
bg-white
p-1
"
>


<input
type="email"
placeholder="Enter your email"
className="
flex-1
rounded-xl
px-4
text-gray-800
outline-none
"
/>


<button
className="
flex
items-center
gap-2
rounded-xl
bg-emerald-600
px-5
py-3
font-bold
text-white
transition
hover:bg-emerald-700
"
>

Subscribe

<ArrowRight size={16}/>

</button>


</div>


</div>


</div>






<div
className="
grid
gap-12
md:grid-cols-2
lg:grid-cols-4
"
>



{/* Brand */}

<div>


<div
className="
mb-5
flex
items-center
gap-3
"
>


<div
className="
flex
h-12
w-12
items-center
justify-center
rounded-2xl
bg-gradient-to-br
from-yellow-300
to-yellow-500
shadow-lg
"
>

<BrainCircuit
size={25}
className="text-gray-900"
/>

</div>



<div>

<h3
className="
text-xl
font-extrabold
"
>

MukondoGTech

<span
className="
text-yellow-300
"
>
AI
</span>

</h3>


<p
className="
text-xs
uppercase
tracking-widest
text-gray-400
"
>
Intelligent Solutions
</p>


</div>


</div>




<p
className="
leading-relaxed
text-gray-300
"
>

Building secure AI-powered solutions
for immigration analysis,
automation, and digital transformation.

</p>



<div
className="
mt-6
flex
items-center
gap-2
rounded-xl
border
border-white/10
bg-white/5
p-3
"
>

<ShieldCheck
size={20}
className="text-emerald-400"
/>

<span
className="
text-sm
text-gray-300
"
>
Enterprise-grade security
</span>


</div>


</div>






{/* Platform */}

<div>

<h4
className="
mb-5
text-lg
font-bold
"
>
Platform
</h4>


<ul
className="
space-y-3
text-gray-300
"
>

{
platformLinks.map(item=>(

<li
key={item}
className="
cursor-pointer
transition
hover:text-yellow-300
"
>
{item}
</li>

))
}


</ul>


</div>






{/* Resources */}

<div>

<h4
className="
mb-5
text-lg
font-bold
"
>
Resources
</h4>


<ul
className="
space-y-3
text-gray-300
"
>

{
resourceLinks.map(item=>(

<li
key={item}
className="
cursor-pointer
transition
hover:text-yellow-300
"
>
{item}
</li>

))
}


</ul>


</div>







{/* Contact */}

<div>


<h4
className="
mb-5
text-lg
font-bold
"
>
Contact
</h4>



<div
className="
space-y-4
text-gray-300
"
>


<div className="flex gap-3">

<FaEnvelope
className="text-yellow-300"
/>

support@mukondogtech.ai

</div>



<div className="flex gap-3">

<FaPhone
className="text-yellow-300"
/>

+263 783 496 008

</div>



<div className="flex gap-3">

<FaLocationDot
className="text-yellow-300"
/>

Harare, Zimbabwe

</div>



</div>


<div
className="
mt-6
flex
items-center
gap-3
rounded-xl
border
border-white/10
bg-white/5
p-3
"
>

<div className="flex -space-x-3">
{[0, 1].map((i) => (
<span
key={i}
className="
flex
h-9
w-9
items-center
justify-center
rounded-full
border-2
border-[#111827]
bg-gradient-to-br
from-yellow-300
to-yellow-500
text-gray-900
"
>
<UserRound size={16} />
</span>
))}
</div>

<span
className="
text-sm
text-gray-300
"
>
Real people behind every reply — most messages
answered within a few hours.
</span>

</div>


</div>



</div>







{/* Social */}

<div
className="
mt-14
flex
flex-col
gap-6
border-t
border-white/10
pt-8
lg:flex-row
lg:items-center
lg:justify-between
"
>


<p
className="
flex
items-center
gap-2
text-sm
text-gray-400
"
>

<FaCopyright size={12}/>

{currentYear}
MukondoGTech AI.
All rights reserved.

</p>



<div
className="
flex
gap-3
"
>

{
socialLinks.map(item=>(

<a

key={item.label}

href={item.url}

target="_blank"

rel="noopener noreferrer"

aria-label={item.label}

className="
flex
h-11
w-11
items-center
justify-center
rounded-xl
border
border-white/10
bg-white/5
transition
hover:-translate-y-1
hover:bg-yellow-300
hover:text-gray-900
"

>

{item.icon}

</a>


))

}


</div>



<div
className="
flex
gap-5
text-sm
text-gray-400
"
>

<a
href="/privacy"
className="hover:text-white"
>
Privacy
</a>


<a
href="/terms"
className="hover:text-white"
>
Terms
</a>


<a
href="/cookies"
className="hover:text-white"
>
Cookies
</a>


</div>


</div>



</div>


</footer>


  );
}