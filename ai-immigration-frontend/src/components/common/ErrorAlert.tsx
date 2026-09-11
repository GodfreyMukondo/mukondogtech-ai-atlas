import {
AlertCircle
} from "lucide-react";


interface ErrorAlertProps {

message:string;

}


export default function ErrorAlert({

message

}:ErrorAlertProps){


return (

<div

role="alert"

className="
flex
gap-3
items-start
rounded-xl
bg-red-400/10
border
border-red-400/20
px-4
py-3
text-red-300
"

>


<AlertCircle
size={20}
/>


<p>
{message}
</p>


</div>

);

}