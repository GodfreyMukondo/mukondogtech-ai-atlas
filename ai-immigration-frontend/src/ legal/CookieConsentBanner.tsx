import {
  useEffect,
  useState,
} from "react";

import {
  Cookie,
  X,
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
value:Consent
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
bg-white
border
border-[#E5DED1]
shadow-xl
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
bg-[#F8F6F1]
"

>

<Cookie

className="
text-[#F4B81A]
"

/>

</div>




<div>


<h3

className="
font-semibold
text-[#0B1736]
"

>

We use cookies

</h3>



<p

className="
text-sm
text-gray-600
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
text-gray-700
hover:bg-gray-100
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
bg-[#0B1736]
text-white
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