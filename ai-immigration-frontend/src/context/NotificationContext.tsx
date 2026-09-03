import {

createContext,

useContext,

useState,

ReactNode

} from "react";



import {

CheckCircle,

AlertCircle,

Info,

XCircle

} from "lucide-react";





type NotificationType =

"success"

|

"error"

|

"warning"

|

"info";





interface Notification {


id:number;


message:string;


type:NotificationType;


}





interface Context {


notify:

(

message:string,

type?:NotificationType,

duration?:number

)=>void;


}





const NotificationContext =
createContext<
Context | undefined
>(undefined);





export function NotificationProvider({

children

}:{

children:ReactNode;

}){


const [

notifications,

setNotifications

]=useState<
Notification[]
>([]);







const notify = (

message:string,

type:NotificationType="info",

duration=4000

)=>{



const id =
Date.now();




setNotifications(prev=>[

...prev,

{
id,
message,
type
}

]);




setTimeout(()=>{


setNotifications(prev=>

prev.filter(
item=>item.id!==id
)

);



},duration);



};





const remove = (
id:number
)=>{


setNotifications(prev=>

prev.filter(
item=>item.id!==id
)

);


};





const icons={


success:
<CheckCircle
size={20}
/>,


error:
<XCircle
size={20}
/>,


warning:
<AlertCircle
size={20}
/>,


info:
<Info
size={20}
/>


};






return (

<NotificationContext.Provider

value={{
notify
}}

>


{children}




<div

className="
fixed
top-5
right-5
z-50
space-y-3
"

>


{

notifications.map(item=>(


<div

key={item.id}

className="
flex
items-center
gap-3
bg-white
border
rounded-xl
shadow-lg
px-5
py-4
min-w-[300px]
"

>


<div>

{icons[item.type]}

</div>




<p

className="
text-sm
font-medium
text-[#0B1736]
flex-1
"

>

{item.message}

</p>




<button

onClick={()=>remove(
item.id
)}

className="
text-gray-400
"

>

×

</button>



</div>


))


}



</div>



</NotificationContext.Provider>


);


}






export function useNotification(){


const context =
useContext(
NotificationContext
);



if(!context){

throw new Error(
"useNotification must be used inside NotificationProvider"
);

}



return context;


}