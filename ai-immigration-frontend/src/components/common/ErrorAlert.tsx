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
bg-red-50
border
border-red-200
px-4
py-3
text-red-600
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