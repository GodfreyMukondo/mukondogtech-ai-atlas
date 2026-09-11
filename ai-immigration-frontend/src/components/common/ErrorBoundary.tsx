import {
Component,
} from "react";

import type {
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
p-6
">


<div className="
max-w-md
text-center
border
border-white/10
bg-[#1F314A]
rounded-2xl
shadow-2xl
shadow-black/40
backdrop-blur-xl
p-8
">


<h1 className="
text-2xl
font-bold
text-white
">

Something went wrong

</h1>


<p className="
mt-3
text-slate-300
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
bg-[#C6A15B]
text-black
font-semibold
hover:bg-[#A8894D]
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