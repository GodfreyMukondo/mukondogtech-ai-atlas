export function formatDate(
  date:string | Date
):string {


  return new Intl.DateTimeFormat(
    "en-US",
    {
      year:"numeric",
      month:"short",
      day:"numeric",
    }
  ).format(
    new Date(date)
  );

}




export function formatFileSize(
bytes:number
):string {


if(bytes === 0)
return "0 Bytes";


const units = [
"Bytes",
"KB",
"MB",
"GB",
];


const index =
Math.floor(
Math.log(bytes) /
Math.log(1024)
);



return `${(
bytes /
Math.pow(1024,index)
).toFixed(2)} ${units[index]}`;

}





export function capitalize(
value:string
):string {


return value
.charAt(0)
.toUpperCase()
+
value.slice(1).toLowerCase();


}