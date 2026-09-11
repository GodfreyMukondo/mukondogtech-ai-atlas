import {
  useEffect,
  useState,
} from "react";


import {
  Link,
  useNavigate,
  useSearchParams,
} from "react-router-dom";


import {
  CheckCircle,
} from "lucide-react";


import {
  verifyEmail,
} from "../../api/authApi";







export default function VerifyEmailPage(){



const navigate = useNavigate();



const [searchParams] =
useSearchParams();



const token =
searchParams.get("token");





const [loading,setLoading] =
useState(true);



const [message,setMessage] =
useState("");



const [error,setError] =
useState("");







useEffect(()=>{


const verify = async()=>{



if(!token){


setError(
"Invalid verification link."
);


setLoading(false);


return;


}



try{


await verifyEmail(token);



setMessage(
"Your email has been verified successfully."
);



setTimeout(()=>{


navigate("/login");


},3000);



}catch(err:any){



setError(

err?.response?.data?.message ||

"Unable to verify email."

);



}finally{


setLoading(false);


}


};



verify();



},[
token,
navigate
]);








return (


<div

className="
bg-white/5
backdrop-blur-xl
border
border-white/10
rounded-3xl
p-8
shadow-2xl
shadow-black/40
text-center
"

>





{
loading &&

<p

className="
text-slate-300
"

>

Verifying your email...

</p>

}







{
message &&

<>


<CheckCircle

size={60}

className="
mx-auto
text-emerald-400
"

/>





<h2

className="
mt-5
text-3xl
font-bold
text-white
"

>

Email Verified

</h2>





<p

className="
mt-3
text-slate-300
"

>

{message}

</p>




</>

}







{
error &&

<div

className="
mt-5
rounded-xl
bg-red-500/10
border
border-red-500/30
text-red-300
px-4
py-3
"

>

{error}

</div>

}








<div

className="
mt-6
space-y-3
"

>



<Link

to="/login"

className="
block
font-semibold
text-white
hover:text-[#C6A15B]
transition
"

>

Continue to Sign In

</Link>




<Link

to="/register"

className="
block
text-slate-300
hover:text-white
transition
"

>

Create another account

</Link>




</div>






</div>


);


}