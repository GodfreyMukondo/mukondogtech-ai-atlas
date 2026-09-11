import {
User,
CreditCard,
Trash2,
} from "lucide-react";

import {
Link
} from "react-router-dom";


export default function AccountPage(){


return (

<div
className="
min-h-screen
p-6
text-slate-100
"
>


<div
className="
max-w-4xl
mx-auto
"
>


<h1
className="
text-4xl
font-bold
mb-10
"
>
Account
</h1>



<div
className="
grid
md:grid-cols-3
gap-6
"
>


<div className="
bg-white
border
rounded-2xl
p-6
">

<User/>

<h3 className="
font-bold
mt-4
">
Profile
</h3>

</div>



<div className="
bg-white
border
rounded-2xl
p-6
">

<CreditCard/>

<h3 className="
font-bold
mt-4
">
Subscription
</h3>

</div>




<Link
to="/account/delete"
className="
bg-white
border
rounded-2xl
p-6
"
>

<Trash2
className="
text-red-600
"/>

<h3
className="
font-bold
mt-4
"
>
Delete Account
</h3>


</Link>


</div>


</div>


</div>

);

}