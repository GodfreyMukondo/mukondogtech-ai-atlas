import {
  useState,
} from "react";

import {
  Link,
  useNavigate,
} from "react-router-dom";

import {
  ArrowRight,
} from "lucide-react";

import {
  forgotPassword,
} from "../../api/authApi";



interface ForgotPasswordForm {

  email:string;

}



export default function ForgotPasswordPage(){


const navigate = useNavigate();



const [form,setForm] =
useState<ForgotPasswordForm>({
 email:"",
});



const [loading,setLoading] =
useState(false);



const [error,setError] =
useState("");



const [success,setSuccess] =
useState("");






const handleSubmit = async(
e:React.FormEvent<HTMLFormElement>
)=>{


e.preventDefault();


setError("");
setSuccess("");



try{


setLoading(true);



await forgotPassword(
form.email
);



setSuccess(
"Password reset instructions have been sent to your email."
);



setTimeout(()=>{

navigate("/login");

},3000);



}catch(err:any){


setError(

err?.response?.data?.message ||

"Unable to process password reset request."

);



}finally{


setLoading(false);


}


};







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
"

>



<h2

className="
text-3xl
font-bold
text-white
"

>

Forgot password?

</h2>




<p

className="
mt-2
text-slate-300
"

>

Enter your email address and we will send
instructions to reset your password.

</p>







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





{
success &&

<div

className="
mt-5
rounded-xl
bg-emerald-500/10
border
border-emerald-500/30
text-emerald-300
px-4
py-3
"

>

{success}

</div>

}







<form

onSubmit={handleSubmit}

className="
mt-8
space-y-5
"

>



<div>


<label

className="
block
mb-2
font-medium
text-slate-200
"

>

Email Address

</label>



<input


type="email"


required


value={form.email}



onChange={(e)=>

setForm({

email:e.target.value

})

}



placeholder="you@example.com"



className="
w-full
px-4
py-3
rounded-xl
border
border-white/15
bg-white/5
text-white
placeholder:text-slate-500
focus:outline-none
focus:ring-2
focus:ring-[#C6A15B]
"



/>



</div>







<button


type="submit"


disabled={loading}



className="
w-full
py-4
rounded-xl
bg-[#C6A15B]
hover:bg-[#A8894D]
font-semibold
text-black
flex
items-center
justify-center
gap-2
disabled:opacity-60
"



>

{

loading

?

"Sending..."

:

<>

Send Reset Link

<ArrowRight size={18}/>

</>

}


</button>





</form>







<p

className="
mt-6
text-center
text-slate-300
"

>

Remember your password?{" "}



<Link

to="/login"

className="
font-semibold
text-white
hover:text-[#C6A15B]
transition
"

>

Sign in

</Link>


</p>





<p

className="
mt-3
text-center
text-slate-300
"

>

Need an account?{" "}



<Link

to="/register"

className="
font-semibold
text-white
hover:text-[#C6A15B]
transition
"

>

Create account

</Link>


</p>




</div>

);


}