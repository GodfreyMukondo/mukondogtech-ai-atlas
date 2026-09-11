export default function ServerErrorPage(){


return (

<div
className="
min-h-screen
flex
items-center
justify-center
text-center
p-6
"
>

<div>

<h1
className="
text-7xl
font-bold
text-slate-100
"
>
500
</h1>


<h2
className="
text-3xl
font-bold
mt-4
"
>
Server Error
</h2>


<p
className="
text-slate-300
mt-3
"
>
Something went wrong on our servers.
</p>


<button
className="
mt-6
bg-[#C6A15B]
text-[#071426]
font-semibold
px-6
py-3
rounded-xl
"
>
Try Again
</button>


</div>


</div>

);

}