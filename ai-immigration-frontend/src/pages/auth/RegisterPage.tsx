// src/pages/auth/RegisterPage.tsx


"use client";


import {
  useState,
} from "react";


import {
  Link,
  useNavigate,
} from "react-router-dom";


import {
  ArrowRight,
  Eye,
  EyeOff,
} from "lucide-react";


import {
  registerUser,
} from "../../api/authApi";





interface RegisterForm {

  fullName:string;

  email:string;

  password:string;

  confirmPassword:string;

}








export default function RegisterPage(){



const navigate =
useNavigate();







const [form,setForm] =
useState<RegisterForm>({

  fullName:"",

  email:"",

  password:"",

  confirmPassword:"",

});








const [loading,setLoading] =
useState(false);






const [error,setError] =
useState("");







const [showPassword,setShowPassword] =
useState(false);






const [showConfirmPassword,setShowConfirmPassword] =
useState(false);










const validatePassword = (
password:string
)=>{


if(password.length < 12){

return "Password must contain at least 12 characters.";

}


if(!/[A-Z]/.test(password)){

return "Password must contain at least one uppercase letter.";

}


if(!/[0-9]/.test(password)){

return "Password must contain at least one number.";

}


return null;


};









const handleSubmit = async(
e:React.FormEvent<HTMLFormElement>
)=>{


e.preventDefault();





if(loading){

return;

}





setError("");









if(!form.fullName.trim()){


setError(
"Full name is required."
);


return;


}








if(
form.password !== form.confirmPassword
){


setError(
"Passwords do not match."
);


return;


}








const passwordError =
validatePassword(
form.password
);



if(passwordError){


setError(passwordError);


return;


}









try{



setLoading(true);






await registerUser({



fullName:

form.fullName
.trim(),





email:

form.email
.trim()
.toLowerCase(),






password:

form.password,



});








navigate(

"/login",

{

replace:true,

}

);







}catch(error:any){





console.error(

"Registration error:",

error

);







setError(


error?.response?.data?.message ||


error?.message ||


"Unable to create account. Please try again."

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

Create account


</h2>








<p


className="

mt-2

text-slate-300

"


>

Start verifying documents with AI-powered
immigration intelligence.


</p>












{


error &&


<div


role="alert"


className="

mt-5

rounded-xl

bg-red-500/10

border

border-red-500/30

text-red-300

px-4

py-3

text-sm

"


>

{error}


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

Full Name


</label>




<input


type="text"


required




value={form.fullName}




onChange={(e)=>


setForm({

...form,

fullName:e.target.value,

})


}




placeholder="John Doe"




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

...form,

email:e.target.value,

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









<div>


<label


className="

block

mb-2

font-medium

text-slate-200

"


>

Password


</label>






<div className="relative">





<input


type={showPassword ? "text":"password"}


required




value={form.password}




onChange={(e)=>


setForm({

...form,

password:e.target.value,

})


}




placeholder="Create a secure password"




className="

w-full

px-4

py-3

pr-12

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








<button


type="button"


aria-label="Toggle password visibility"



onClick={()=>


setShowPassword(
previous=>!previous
)


}



className="

absolute

right-3

top-1/2

-translate-y-1/2

text-slate-400

"


>



{

showPassword

?

<EyeOff size={20}/>

:

<Eye size={20}/>

}



</button>





</div>


</div>









<div>


<label


className="

block

mb-2

font-medium

text-slate-200

"


>

Confirm Password


</label>






<div className="relative">





<input


type={showConfirmPassword ? "text":"password"}


required




value={form.confirmPassword}




onChange={(e)=>


setForm({

...form,

confirmPassword:e.target.value,

})


}




placeholder="Confirm password"




className="

w-full

px-4

py-3

pr-12

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







<button


type="button"


aria-label="Toggle confirm password visibility"



onClick={()=>


setShowConfirmPassword(

previous=>!previous

)


}




className="

absolute

right-3

top-1/2

-translate-y-1/2

text-slate-400

"


>


{


showConfirmPassword

?

<EyeOff size={20}/>

:

<Eye size={20}/>

}



</button>






</div>


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

text-black

font-semibold

transition

flex

items-center

justify-center

gap-2

disabled:opacity-60

disabled:cursor-not-allowed

"


>



{


loading

?

"Creating account..."

:

<>

Create Account

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

Already have an account?{" "}



<Link


to="/login"



className="

text-white

font-semibold

hover:text-[#C6A15B]

transition

"


>

Sign in


</Link>



</p>








</div>



);



}