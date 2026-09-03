export default function ServerErrorPage(){


return (

<div
className="
min-h-screen
bg-[#F8F6F1]
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
text-[#0B1736]
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
text-gray-600
mt-3
"
>
Something went wrong on our servers.
</p>


<button
className="
mt-6
bg-[#F4B81A]
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