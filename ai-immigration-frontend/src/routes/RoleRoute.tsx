import {
  Navigate,
  Outlet,
  useLocation,
} from "react-router-dom";

import {
  Loader2,
} from "lucide-react";


import {
  useAuth,
} from "@/hooks/useAuth";



interface RoleRouteProps {

  allowedRoles?: string[];

}



export default function RoleRoute({
  allowedRoles = [],
}:RoleRouteProps){


const {
  user,
  loading,
}=useAuth();


const location = useLocation();



if(loading){

return (

<div
className="
min-h-screen
flex
items-center
justify-center
bg-[#F8F6F1]
"
>

<Loader2
className="
animate-spin
text-[#F4B81A]
"
size={40}
/>

</div>

);

}




// Not authenticated

if(!user){

return (

<Navigate

to="/login"

state={{
from:location.pathname
}}

replace

/>

);

}




// Role validation

if(

allowedRoles.length > 0 &&

!allowedRoles.includes(
user.role
)

){

return (

<Navigate

to="/403"

replace

/>

);

}



return <Outlet/>;


}