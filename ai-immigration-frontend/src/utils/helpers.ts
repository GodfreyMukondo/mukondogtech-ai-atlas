export function isEmpty(
value: unknown
):boolean {


return (
value === null ||
value === undefined ||
value === ""
);

}




export function delay(
milliseconds:number
):Promise<void>{


return new Promise(
(resolve)=>
setTimeout(
resolve,
milliseconds
)
);

}





export function generateId():string {


return crypto.randomUUID();

}





export async function copyToClipboard(
text:string
):Promise<void>{


await navigator.clipboard.writeText(
text
);


}