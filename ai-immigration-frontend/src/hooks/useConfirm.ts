import {
  useState,
  useCallback,
} from "react";



export default function useConfirm(){



const [
open,
setOpen
]=useState(false);



const [
resolve,
setResolve
]=useState<
((value:boolean)=>void)
| null
>(null);







const confirm =
useCallback(()=>{


return new Promise<boolean>(

(resolve)=>{


setResolve(
()=>resolve
);


setOpen(true);


}

);


},[]);








const handleConfirm = ()=>{


setOpen(false);


resolve?.(true);


};






const handleCancel = ()=>{


setOpen(false);


resolve?.(false);


};






return {


open,


confirm,


handleConfirm,


handleCancel


};


}