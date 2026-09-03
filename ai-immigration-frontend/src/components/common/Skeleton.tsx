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
bg-gray-200
${width}
${height}
${rounded}
${className}
`}

/>

);

}