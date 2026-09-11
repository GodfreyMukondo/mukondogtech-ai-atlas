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
    loginUser,
} from "../../api/authApi";


import {
    useAuth,
} from "../../features/auth/hooks/useAuth";






interface LoginForm {

    email:string;

    password:string;

}





interface LoginUserResponse {

    id?:number;

    email?:string;

    fullName?:string;

    role?:string;

    enabled?:boolean;

    mustChangePassword?:boolean;

}





interface LoginResponse {

    token:string;

    user:LoginUserResponse;

}









export default function LoginPage(){


const navigate = useNavigate();



const {
    login,
} = useAuth();





const [form,setForm] =
useState<LoginForm>({

    email:"",

    password:"",

});





const [loading,setLoading] =
useState(false);



const [error,setError] =
useState("");



const [showPassword,setShowPassword] =
useState(false);







/*
|--------------------------------------------------------------------------
| NORMALIZE ROLE
|--------------------------------------------------------------------------
*/


const normalizeRole = (

    role?:string

)=>{


    if(!role){

        return "";

    }



    return role

        .trim()

        .replace(

            "ROLE_",

            ""

        )

        .toUpperCase();


};









/*
|--------------------------------------------------------------------------
| LOGIN SUBMIT
|--------------------------------------------------------------------------
*/


const handleSubmit = async(

    event:React.FormEvent<HTMLFormElement>

)=>{


    event.preventDefault();



    if(loading){

        return;

    }



    setError("");




    try{


        setLoading(true);




        const response =

            await loginUser({

                email:

                    form.email

                    .trim()

                    .toLowerCase(),


                password:

                    form.password,


            }) as LoginResponse;







        console.log(

            "LOGIN RESPONSE:",

            response

        );








        if(

            !response ||

            !response.token ||

            !response.user

        ){


            throw new Error(

                "Invalid server authentication response."

            );


        }







        const authenticatedUser =

            response.user;







        const role =

            normalizeRole(

                authenticatedUser.role

            );







        console.log(

            "USER ROLE:",

            role

        );








        const finalUser = {


            ...authenticatedUser,


            role,


        };







        console.log(

            "FINAL AUTH USER:",

            finalUser

        );








        /*
        |--------------------------------------------------------------------------
        | SAVE AUTH STATE
        |--------------------------------------------------------------------------
        */


        login(

            response.token,

            finalUser

        );







        /*
        |--------------------------------------------------------------------------
        | FORCE PASSWORD CHANGE
        |--------------------------------------------------------------------------
        */


        if(

            authenticatedUser.mustChangePassword

        ){


            navigate(

                "/change-password",

                {

                    replace:true,

                }

            );


            return;


        }









        /*
        |--------------------------------------------------------------------------
        | REDIRECT AFTER AUTH STATE UPDATE
        |--------------------------------------------------------------------------
        */


        setTimeout(()=>{


            if(

                role === "ADMIN"

            ){


                console.log(

                    "ADMIN REDIRECT"

                );



                navigate(

                    "/admin",

                    {

                        replace:true,

                    }

                );



            }

            else{


                console.log(

                    "USER REDIRECT"

                );



                navigate(

                    "/dashboard",

                    {

                        replace:true,

                    }

                );


            }


        },100);









    }

    catch(error:any){


        console.error(

            "LOGIN FAILED:",

            error

        );





        setError(

            error?.response?.data?.message ||

            error?.message ||

            "Invalid email or password."

        );


    }

    finally{


        setLoading(false);


    }


};









return (

<div

className="
rounded-3xl
border
border-white/10
bg-white/5
backdrop-blur-xl
p-8
shadow-2xl
shadow-black/40
"

>


<h1

className="
text-3xl
font-black
text-white
"

>

Welcome back

</h1>




<p

className="
mt-3
text-slate-300
"

>

Sign in to access your AI immigration platform.

</p>







{

error &&

<div

className="
mt-5
rounded-xl
border
border-red-500/30
bg-red-500/10
px-4
py-3
text-sm
text-red-300
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
mb-2
block
font-semibold
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



placeholder="admin@example.com"



className="
w-full
rounded-xl
border
border-white/15
bg-white/5
text-white
placeholder:text-slate-500
px-4
py-3
outline-none
focus:ring-2
focus:ring-[#C6A15B]
"

/>


</div>







<div>

<label

className="
mb-2
block
font-semibold
text-slate-200
"

>

Password

</label>





<div

className="
relative
"

>



<input


type={

showPassword

?

"text"

:

"password"

}



required



value={form.password}



onChange={(e)=>

setForm({

    ...form,

    password:e.target.value,

})

}



placeholder="Password"



className="
w-full
rounded-xl
border
border-white/15
bg-white/5
text-white
placeholder:text-slate-500
px-4
py-3
pr-12
outline-none
focus:ring-2
focus:ring-[#C6A15B]
"

/>







<button


type="button"



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

aria-label="Toggle password visibility"

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









<div

className="
flex
justify-end
"

>


<Link

to="/forgot-password"

className="
text-sm
font-semibold
text-slate-300
hover:text-[#C6A15B]
"

>

Forgot password?

</Link>


</div>







<button


type="submit"



disabled={loading}



className="
flex
w-full
items-center
justify-center
gap-2
rounded-xl
bg-[#C6A15B]
py-4
font-bold
text-black
transition
hover:bg-[#A8894D]
disabled:opacity-50
"

>


{

loading

?

"Signing in..."

:

<>

Sign In

<ArrowRight size={18}/>

</>

}


</button>






</form>









<p

className="
mt-6
text-center
text-sm
text-slate-400
"

>

Don't have an account?{" "}



<Link

to="/register"

className="
font-bold
text-white
hover:text-[#C6A15B]
"

>

Create account

</Link>


</p>






</div>

);


}