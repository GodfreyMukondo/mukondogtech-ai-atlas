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
bg-white
border
border-[#E5DED1]
rounded-3xl
p-8
shadow-sm
text-center
"

>





{
loading &&

<p

className="
text-[#7D8CA3]
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
text-green-600
"

/>





<h2

className="
mt-5
text-3xl
font-bold
text-[#0B1736]
"

>

Email Verified

</h2>





<p

className="
mt-3
text-[#7D8CA3]
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
bg-red-50
border
border-red-200
text-red-600
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
text-[#0B1736]
hover:text-[#F4B81A]
transition
"

>

Continue to Sign In

</Link>




<Link

to="/register"

className="
block
text-[#7D8CA3]
hover:text-[#0B1736]
transition
"

>

Create another account

</Link>




</div>






</div>


);


}