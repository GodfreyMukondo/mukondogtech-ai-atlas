import {
  useMemo,
  useState,
} from "react";



interface PaginationOptions {


initialPage?:number;


initialSize?:number;


}



export default function usePagination({

initialPage=1,

initialSize=10

}:PaginationOptions={}){



const [

page,

setPage

]=useState(initialPage);



const [

size,

setSize

]=useState(initialSize);







const nextPage = ()=>{


setPage(
prev=>prev+1
);


};






const previousPage = ()=>{


setPage(
prev=>Math.max(
1,
prev-1
)
);


};






const goToPage = (
value:number
)=>{


if(value < 1)
return;



setPage(value);


};








const changeSize = (
value:number
)=>{


setSize(value);


setPage(1);


};







const query = useMemo(()=>({

page,

size,

offset:
(page-1)*size


}),[
page,
size
]);







const reset = ()=>{


setPage(initialPage);

setSize(initialSize);


};







return {


page,

size,

query,


nextPage,

previousPage,

goToPage,

changeSize,

reset


};


}