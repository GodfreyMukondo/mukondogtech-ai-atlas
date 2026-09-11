import {
  motion,
} from "framer-motion";

import {
  useState,
} from "react";

import {
  User,
  Save,
  Camera,
} from "lucide-react";


export default function ProfileSettingsPage(){

const [loading,setLoading]=useState(false);


const handleSave=()=>{

setLoading(true);

setTimeout(()=>{
setLoading(false);
},1200);

};



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
Profile Settings
</h1>


<p
className="
text-slate-300
mt-2
mb-10
"
>
Manage your personal information.
</p>



<motion.div

initial={{
opacity:0,
y:20
}}

animate={{
opacity:1,
y:0
}}

className="
bg-white/5
backdrop-blur-xl
rounded-3xl
border
border-white/10
p-8
"
>


<div
className="
flex
items-center
gap-6
mb-8
"
>

<div
className="
w-24
h-24
rounded-full
bg-[#0B1F3A]
text-white
flex
items-center
justify-center
"
>

<User size={40}/>

</div>


<button
className="
flex
items-center
gap-2
border
border-white/15
text-white
hover:bg-white/10
transition
px-4
py-2
rounded-xl
"
>

<Camera size={18}/>

Change Photo

</button>


</div>



<div
className="
grid
md:grid-cols-2
gap-6
"
>


<input
defaultValue="Godfrey"
placeholder="First Name"
className="
border
border-white/15
bg-white/5
text-white
placeholder:text-slate-500
rounded-xl
px-4
py-3
"
/>


<input
defaultValue="Mukondo"
placeholder="Last Name"
className="
border
border-white/15
bg-white/5
text-white
placeholder:text-slate-500
rounded-xl
px-4
py-3
"
/>


<input
defaultValue="user@example.com"
placeholder="Email"
className="
border
border-white/15
bg-white/5
text-white
placeholder:text-slate-500
rounded-xl
px-4
py-3
md:col-span-2
"
/>


</div>



<button

onClick={handleSave}

className="
mt-8
bg-[#C6A15B]
text-[#071426]
px-6
py-3
rounded-xl
font-semibold
flex
items-center
gap-2
"

>

<Save size={18}/>

{
loading
?
"Saving..."
:
"Save Changes"
}

</button>



</motion.div>


</div>


</div>

);

}