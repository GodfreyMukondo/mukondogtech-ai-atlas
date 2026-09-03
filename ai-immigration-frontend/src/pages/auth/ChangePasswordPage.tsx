"use client";


import {
    useState,
} from "react";


import {
    useNavigate,
} from "react-router-dom";


import {
    Lock,
    Eye,
    EyeOff,
    CheckCircle2,
    ArrowRight,
} from "lucide-react";


import {
    changePassword,
} from "../../api/authApi";


import {
    useAuth,
} from "../../features/auth/hooks/useAuth";







interface PasswordForm {


    currentPassword:string;


    newPassword:string;


    confirmPassword:string;


}









export default function ChangePasswordPage(){



const navigate = useNavigate();



const {
    logout,
    user,
}=useAuth();






const [form,setForm] = useState<PasswordForm>({

    currentPassword:"",

    newPassword:"",

    confirmPassword:"",

});





const [loading,setLoading] =
useState(false);



const [error,setError] =
useState("");



const [success,setSuccess] =
useState("");





const [showPassword,setShowPassword] =
useState({

    current:false,

    new:false,

    confirm:false,

});









const updateField = (

    field:keyof PasswordForm,

    value:string

)=>{


setForm({

    ...form,

    [field]:value,

});


};









const handleSubmit = async(

    event:React.FormEvent<HTMLFormElement>

)=>{


event.preventDefault();



setError("");

setSuccess("");






if(form.newPassword.length < 12){


    setError(
        "Password must contain at least 12 characters."
    );


    return;

}





if(form.newPassword !== form.confirmPassword){


    setError(
        "Passwords do not match."
    );


    return;

}





try{


    setLoading(true);





    await changePassword({

        currentPassword:
            form.currentPassword,


        newPassword:
            form.newPassword,

    });







    setSuccess(
        "Password changed successfully. Redirecting..."
    );







    /*
    |--------------------------------------------------------------------------
    | Remove old temporary session
    |--------------------------------------------------------------------------
    */


    setTimeout(()=>{


        logout();


        navigate(

            "/login",

            {
                replace:true,
            }

        );


    },1500);







}
catch(error:any){



    console.error(
        "Password change failed:",
        error
    );




    setError(

        error?.response?.data?.message ||

        "Unable to change password."

    );



}
finally{


    setLoading(false);


}



};









return (


<div

className="
min-h-screen
flex
items-center
justify-center
bg-[#F8F6F1]
px-4
"

>


<div

className="
w-full
max-w-md
rounded-3xl
bg-white
border
border-[#E5DED1]
shadow-xl
p-8
"

>





<div

className="
flex
items-center
gap-3
"

>

<div

className="
rounded-2xl
bg-[#F4B81A]/20
p-3
"

>

<Lock

className="text-[#F4B81A]"

/>

</div>



<h1

className="
text-3xl
font-black
text-[#0B1736]
"

>

Change Password

</h1>


</div>









<p

className="
mt-4
text-[#7D8CA3]
"

>

For security reasons, you must update your password before continuing.

</p>







{
user?.email &&

<p

className="
mt-3
text-sm
font-semibold
text-[#0B1736]
"

>

Account:

{" "}

{user.email}

</p>

}









{
error &&

<div

className="
mt-6
rounded-xl
border
border-red-200
bg-red-50
px-4
py-3
text-sm
text-red-600
"

>

{error}

</div>

}







{
success &&

<div

className="
mt-6
flex
items-center
gap-2
rounded-xl
border
border-green-200
bg-green-50
px-4
py-3
text-sm
text-green-700
"

>

<CheckCircle2 size={18}/>

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










{
[
{
key:"currentPassword",
label:"Current Password",
placeholder:"Enter temporary password"
},
{
key:"newPassword",
label:"New Password",
placeholder:"Minimum 12 characters"
},
{
key:"confirmPassword",
label:"Confirm New Password",
placeholder:"Repeat new password"
}

].map((field)=>{


const key =
field.key as keyof PasswordForm;



return (

<div

key={field.key}

>


<label

className="
mb-2
block
font-semibold
text-[#0B1736]
"

>

{field.label}

</label>




<div

className="
relative
"

>

<input


type={

showPassword[

field.key as keyof typeof showPassword

]

?

"text"

:

"password"

}



required



value={

form[key]

}



onChange={(e)=>

updateField(

key,

e.target.value

)

}



placeholder={
field.placeholder
}




className="
w-full
rounded-xl
border
border-gray-300
px-4
py-3
pr-12
outline-none
focus:ring-2
focus:ring-[#F4B81A]
"





/>






<button


type="button"



onClick={()=>


setShowPassword({

    ...showPassword,


    [field.key]:

    !showPassword[

    field.key as keyof typeof showPassword

    ],


})


}


className="
absolute
right-3
top-1/2
-translate-y-1/2
text-gray-500
"

>


{
showPassword[

field.key as keyof typeof showPassword

]

?

<EyeOff size={20}/>

:

<Eye size={20}/>

}



</button>




</div>


</div>

);


})}









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
bg-[#F4B81A]
py-4
font-black
text-black
transition
hover:bg-[#e6ab12]
disabled:opacity-50
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






</div>


</div>


);


}