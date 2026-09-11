import {
  useAuth,
} from "../features/auth/hooks/useAuth";



export default function usePermission(){


const {
 user
}=useAuth();





const hasRole = (
role:string
)=>{


return (
user?.roles
?.includes(role)
||
false
);


};






const hasPermission = (

permission:string

)=>{


return (

user?.authorities
?.includes(permission)

||
false

);


};







const can = (

permission:string

)=>{


return hasPermission(
permission
);


};






return {


hasRole,

hasPermission,

can


};


}