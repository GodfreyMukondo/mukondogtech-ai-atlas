import {
  useRef,
  useState,
} from "react";


import axios from "../api/axios";



interface UploadOptions {


maxSizeMB?:number;


allowedTypes?:string[];


}



export default function useFileUpload(

options:UploadOptions={}

){



const {

maxSizeMB=10,

allowedTypes=[

"application/pdf",

"image/png",

"image/jpeg"

]


}=options;






const [

progress,

setProgress

]=useState(0);



const [

loading,

setLoading

]=useState(false);



const [

error,

setError

]=useState("");



const [

uploadedUrl,

setUploadedUrl

]=useState("");





const cancelToken =
useRef(
new AbortController()
);









const validateFile = (
file:File
)=>{



if(
!allowedTypes.includes(
file.type
)
){

return "File type not supported.";

}





if(
file.size >
maxSizeMB *
1024 *
1024
){

return (

`Maximum file size is ${maxSizeMB}MB`

);

}





return "";



};









const upload = async(

file:File

)=>{



const validation =
validateFile(file);



if(validation){


setError(validation);

throw new Error(validation);


}






try{


setLoading(true);

setError("");

setProgress(0);






const formData =
new FormData();



formData.append(
"file",
file
);







const response =
await axios.post(

"/documents/upload",

formData,


{

signal:
cancelToken.current.signal,


headers:{

"Content-Type":
"multipart/form-data"

},


onUploadProgress(event){


const percent =
Math.round(

(event.loaded*100)
/
(event.total || 1)

);



setProgress(percent);


}



}


);





setUploadedUrl(
response.data.url
);



return response.data;



}

catch(error:any){



if(
error.name==="CanceledError"
){

setError(
"Upload cancelled."
);


}

else{


setError(

error?.response?.data?.message
||
"Upload failed."

);


}



throw error;


}

finally{


setLoading(false);


}



};









const cancelUpload = ()=>{


cancelToken.current.abort();



};








const reset = ()=>{


setProgress(0);

setError("");

setUploadedUrl("");

setLoading(false);


cancelToken.current =
new AbortController();


};






return {


upload,


cancelUpload,


reset,


progress,


loading,


error,


uploadedUrl


};


}