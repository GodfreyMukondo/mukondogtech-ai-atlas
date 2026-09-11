import {
  motion,
} from "framer-motion";

import {
  useState,
} from "react";

import {
  useNavigate,
} from "react-router-dom";

import {
  CheckCircle,
  CreditCard,
  ShieldCheck,
  ArrowLeft,
  Loader2,
} from "lucide-react";


type Plan = {
  name:string;
  price:number;
  billing:string;
  features:string[];
};


const selectedPlan:Plan = {

name:"Professional",

price:29,

billing:"month",

features:[
"AI document analysis",
"Unlimited document uploads",
"Advanced immigration insights",
"Priority support",
]

};



export default function CheckoutPage(){

const navigate = useNavigate();


const [
loading,
setLoading
]=useState(false);



const handlePayment = async()=>{

setLoading(true);


try{

/*
Flutterwave integration goes here:

1. Create payment request
2. Redirect customer
3. Verify transaction
*/


await new Promise(
resolve=>setTimeout(resolve,1500)
);


navigate("/dashboard/subscription");


}

finally{

setLoading(false);

}

};



return (

<div
className="
min-h-screen
px-6
py-16
text-slate-100
"
>


<div
className="
max-w-4xl
mx-auto
"
>


<button

onClick={()=>navigate(-1)}

className="
flex
items-center
gap-2
text-sm
mb-8
hover:text-[#C6A15B]
"

>

<ArrowLeft size={18}/>

Back

</button>



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
grid
md:grid-cols-2
gap-8
"

>



<div
className="
bg-white/5
backdrop-blur-xl
rounded-3xl
p-8
shadow-sm
border
border-white/10
"
>


<h1
className="
text-3xl
font-bold
mb-4
text-white
"
>

Complete Checkout

</h1>


<p
className="
text-slate-300
mb-8
"
>

Secure your MukondoGTech AI subscription.

</p>



<div
className="
border
border-white/10
rounded-2xl
p-6
"
>


<h2
className="
font-semibold
text-xl
text-white
"
>

{selectedPlan.name}

</h2>


<div
className="
text-4xl
font-bold
mt-3
text-white
"
>

${selectedPlan.price}

<span
className="
text-base
font-normal
text-slate-400
"
>
/month
</span>

</div>


<ul
className="
mt-6
space-y-3
"
>


{
selectedPlan.features.map(
feature=>(

<li
key={feature}
className="
flex
gap-2
items-center
text-slate-200
"
>

<CheckCircle
size={18}
className="
text-emerald-400
"
/>

{feature}

</li>

)
)

}


</ul>


</div>



</div>





<div
className="
bg-white/5
backdrop-blur-xl
rounded-3xl
p-8
shadow-sm
border
border-white/10
"
>


<div
className="
flex
items-center
gap-3
mb-6
text-white
"
>

<CreditCard/>

<h2
className="
text-xl
font-bold
"
>

Payment

</h2>


</div>



<div
className="
bg-white/5
rounded-xl
p-5
mb-6
"
>

<p
className="
text-sm
text-slate-400
"
>
Payment provider
</p>


<p
className="
font-semibold
text-white
"
>
Flutterwave
</p>


</div>



<button

onClick={handlePayment}

disabled={loading}

className="
w-full
bg-[#C6A15B]
text-[#071426]
py-4
rounded-xl
font-semibold
flex
justify-center
items-center
gap-2
hover:opacity-90
disabled:opacity-60
"

>

{
loading
?
<>
<Loader2
className="
animate-spin
"
/>
Processing...
</>

:

<>
<ShieldCheck/>
Pay Securely
</>

}

</button>


<p
className="
text-xs
text-slate-500
mt-5
text-center
"
>

Your payment information is securely processed.

</p>


</div>



</motion.div>


</div>


</div>

);

}