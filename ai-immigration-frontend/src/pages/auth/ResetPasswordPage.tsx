import {
  useState,
} from "react";

import {
  Link,
  useNavigate,
  useSearchParams,
} from "react-router-dom";


import {
  ArrowRight,
} from "lucide-react";


import {
  resetPassword,
} from "../../api/authApi";



interface ResetPasswordForm {

  password:string;

  confirmPassword:string;

}





export default function ResetPasswordPage(){



const navigate = useNavigate();



const [searchParams] =
useSearchParams();



const token =
searchParams.get("token");





const [form,setForm] =
useState<ResetPasswordForm>({
 password:"",
 confirmPassword:"",
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



if(!token){


setError(
"Invalid or expired password reset link."
);


return;

}




if(
form.password !==
form.confirmPassword
){


setError(
"Passwords do not match."
);


return;

}




if(form.password.length < 8){


setError(
"Password must contain at least 8 characters."
);


return;

}




try{


setLoading(true);



await resetPassword({

token,

password:
form.password,

});



setSuccess(
"Password updated successfully. Redirecting to login..."
);



setTimeout(()=>{


navigate("/login");


},3000);



}catch(err:any){



setError(

err?.response?.data?.message ||

"Unable to reset password."

);



}finally{


setLoading(false);


}


};







return (


<div

className="
bg-white
border
border-[#E5DED1]
rounded-3xl
p-8
shadow-sm
"

>



<h2

className="
text-3xl
font-bold
text-[#0B1736]
"

>

Reset password

</h2>





<p

className="
mt-2
text-[#7D8CA3]
"

>

Create a new secure password for your account.

</p>







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







{
success &&

<div

className="
mt-5
rounded-xl
bg-green-50
border
border-green-200
text-green-700
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



<input


type="password"


required


value={form.password}



onChange={(e)=>

setForm({

...form,

password:e.target.value

})

}



placeholder="New password"



className="
w-full
px-4
py-3
rounded-xl
border
border-[#D9DDE5]
focus:outline-none
focus:ring-2
focus:ring-[#F4B81A]
"



/>







<input


type="password"


required



value={form.confirmPassword}



onChange={(e)=>

setForm({

...form,

confirmPassword:e.target.value

})

}



placeholder="Confirm password"



className="
w-full
px-4
py-3
rounded-xl
border
border-[#D9DDE5]
focus:outline-none
focus:ring-2
focus:ring-[#F4B81A]
"



/>








<button


disabled={loading}


className="
w-full
py-4
rounded-xl
bg-[#F4B81A]
hover:bg-[#e6ab12]
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

"Updating..."

:

<>

Update Password

<ArrowRight size={18}/>

</>

}


</button>




</form>







<p

className="
mt-6
text-center
text-[#7D8CA3]
"

>

Remember your password?{" "}



<Link

to="/login"

className="
font-semibold
text-[#0B1736]
hover:text-[#F4B81A]
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
text-[#7D8CA3]
"

>

Need another reset link?{" "}



<Link

to="/forgot-password"

className="
font-semibold
text-[#0B1736]
hover:text-[#F4B81A]
transition
"

>

Request again

</Link>


</p>





</div>


);


}