import {
  useEffect,
  useState,
} from "react";

import {
  Cookie,
} from "lucide-react";



const COOKIE_KEY =
"mukondogtech_cookie_consent";



type Consent =
"accepted"
|
"rejected"
|
null;



export default function CookieConsentBanner(){


const [
consent,
setConsent
]=useState<Consent>(null);



const [
visible,
setVisible
]=useState(false);




useEffect(()=>{


const stored =
localStorage.getItem(
COOKIE_KEY
) as Consent;



if(!stored){

setVisible(true);

}
else{

setConsent(stored);

}



},[]);





const saveConsent = (
value:"accepted"|"rejected"
)=>{


localStorage.setItem(
COOKIE_KEY,
value
);



setConsent(value);

setVisible(false);


};





if(
!visible ||
consent
){

return null;

}




return (

<div

className="
fixed
bottom-5
left-5
right-5
z-50
mx-auto
max-w-5xl
rounded-2xl
bg-[#1F314A]
border
border-white/10
shadow-2xl
shadow-black/40
backdrop-blur-xl
p-6
"

>


<div

className="
flex
flex-col
md:flex-row
gap-5
items-start
md:items-center
justify-between
"

>



<div

className="
flex
gap-4
"

>


<div

className="
p-3
rounded-xl
bg-white/5
"

>

<Cookie

className="
text-[#C6A15B]
"

/>

</div>




<div>


<h3

className="
font-semibold
text-white
"

>

We use cookies

</h3>



<p

className="
text-sm
text-slate-300
mt-1
max-w-xl
"

>

We use cookies to improve your
experience, remember preferences,
and analyze platform usage.

</p>


</div>


</div>





<div

className="
flex
gap-3
"

>


<button

onClick={()=>saveConsent(
"rejected"
)}

className="
px-4
py-2
rounded-lg
border
border-white/15
text-slate-200
hover:bg-white/10
"

>

Reject

</button>




<button

onClick={()=>saveConsent(
"accepted"
)}

className="
px-5
py-2
rounded-lg
bg-gradient-to-r
from-[#C6A15B]
to-[#A8894D]
text-[#071426]
font-semibold
hover:opacity-90
"

>

Accept

</button>



</div>



</div>



</div>

);


}