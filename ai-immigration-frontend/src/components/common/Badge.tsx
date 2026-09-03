interface BadgeProps{

children:React.ReactNode;

variant?:
"primary"
|
"success"
|
"warning"
|
"danger"
|
"neutral";

}



export default function Badge({

children,

variant="primary"

}:BadgeProps){


const styles={


primary:
"bg-blue-100 text-blue-700",


success:
"bg-green-100 text-green-700",


warning:
"bg-yellow-100 text-yellow-700",


danger:
"bg-red-100 text-red-700",


neutral:
"bg-gray-100 text-gray-700"


};



return (

<span

className={`
inline-flex
items-center
px-3
py-1
rounded-full
text-xs
font-semibold
${styles[variant]}
`}

>

{children}

</span>


);

}