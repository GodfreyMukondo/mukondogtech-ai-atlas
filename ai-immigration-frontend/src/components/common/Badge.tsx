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
"bg-blue-400/10 text-blue-300 border border-blue-400/20",


success:
"bg-emerald-400/10 text-emerald-300 border border-emerald-400/20",


warning:
"bg-[#C6A15B]/10 text-[#C6A15B] border border-[#C6A15B]/20",


danger:
"bg-red-400/10 text-red-300 border border-red-400/20",


neutral:
"bg-white/10 text-slate-300 border border-white/15"


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