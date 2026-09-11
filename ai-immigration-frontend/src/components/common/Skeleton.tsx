interface SkeletonProps {

width?:string;

height?:string;

rounded?:string;

className?:string;

}



export default function Skeleton({

width="w-full",

height="h-5",

rounded="rounded-md",

className=""

}:SkeletonProps){


return (

<div

className={`
animate-pulse
bg-white/10
${width}
${height}
${rounded}
${className}
`}

/>

);

}