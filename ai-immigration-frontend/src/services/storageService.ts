/**
 * Storage Service
 *
 * Secure wrapper around browser storage APIs.
 */


interface StorageItem<T> {

  value:T;

  expiresAt?:number;

}



class StorageService {


private storage:
Storage;



constructor(
storage:
Storage =
localStorage,
){

this.storage =
storage;

}





/**
 * Check browser support
 */
private available():boolean {


try {


const test =
"__storage_test__";


this.storage.setItem(
test,
test,
);


this.storage.removeItem(
test,
);


return true;


}
catch{

return false;

}


}






set<T>(
key:string,
value:T,
ttl?:number,
):void {


if(
!this.available()
)
return;



const item:
StorageItem<T> = {

value,

};



if(ttl){

item.expiresAt =
Date.now()+ttl;

}



this.storage.setItem(

key,

JSON.stringify(item),

);


}







get<T>(
key:string,
):T|null {


if(
!this.available()
)
return null;



const raw =
this.storage.getItem(key);



if(!raw)
return null;



try{


const item:
StorageItem<T> =
JSON.parse(raw);



if(
item.expiresAt &&
Date.now() >
item.expiresAt
){

this.remove(key);

return null;

}



return item.value;



}
catch{


return null;

}



}







remove(
key:string,
):void {


if(
this.available()
){

this.storage.removeItem(
key,
);

}


}







clear():void {


if(
this.available()
){

this.storage.clear();

}


}







has(
key:string,
):boolean {


return (
this.get(key)!==
null
);


}







/**
 * JWT helpers
 */


setToken(
token:string,
){

this.set(
"access_token",
token,
);

}




getToken():

string|null{

return this.get<string>(
"access_token",
);

}





removeToken(){

this.remove(
"access_token",
);

}





/**
 * User session helpers
 */


setUser<T>(
user:T,
){

this.set(
"user",
user,
);

}





getUser<T>():
T|null{

return this.get<T>(
"user",
);

}





removeUser(){

this.remove(
"user",
);

}


}





export const storageService =
new StorageService();



export default storageService;