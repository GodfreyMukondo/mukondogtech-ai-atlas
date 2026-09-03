import React,{
Component,
ErrorInfo,
ReactNode
} from "react";


interface Props{

children:ReactNode;

}



interface State{

hasError:boolean;

error?:Error;

}



export default class ErrorBoundary
extends Component<Props,State>{


state:State={

hasError:false

};



static getDerivedStateFromError(
error:Error
){

return{

hasError:true,

error

};

}



componentDidCatch(
error:Error,
info:ErrorInfo
){

console.error(
"Application Error:",
error,
info
);


// Send to monitoring
// Sentry / Datadog / LogRocket


}



render(){


if(this.state.hasError){


return (

<div className="
min-h-screen
flex
items-center
justify-center
bg-gray-50
p-6
">


<div className="
max-w-md
text-center
bg-white
rounded-xl
shadow
p-8
">


<h1 className="
text-2xl
font-bold
text-[#0B1736]
">

Something went wrong

</h1>


<p className="
mt-3
text-gray-600
">

An unexpected error occurred.
Please refresh the page.

</p>


<button

onClick={()=>window.location.reload()}

className="
mt-6
px-5
py-2
rounded-lg
bg-[#F4B81A]
text-[#0B1736]
font-semibold
"

>

Reload Application

</button>


</div>


</div>

);


}



return this.props.children;


}


}