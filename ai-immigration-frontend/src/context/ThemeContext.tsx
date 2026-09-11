import {

createContext,

useContext,

useEffect,

useState,

} from "react";

import type {

ReactNode

} from "react";



type Theme =

"light"

|

"dark"

|

"system";





interface ThemeContextType {


theme:Theme;


setTheme:
(theme:Theme)=>void;


resolvedTheme:
"light"
|
"dark";

}



const ThemeContext =
createContext<
ThemeContextType | undefined
>(undefined);





export function ThemeProvider({

children

}:{

children:ReactNode;

}){


const [

theme,

setTheme

]=useState<Theme>(

()=>(
localStorage.getItem(
"theme"
) as Theme
)
||
"system"

);





const [

resolvedTheme,

setResolvedTheme

]=useState<
"light"|"dark"
>("light");





useEffect(()=>{


const root =
document.documentElement;



const updateTheme = ()=>{


const systemDark =
window.matchMedia(
"(prefers-color-scheme: dark)"
)
.matches;



const dark =

theme==="dark"

||

(
theme==="system"
&&
systemDark
);



if(dark){

root.classList.add(
"dark"
);

setResolvedTheme(
"dark"
);

}

else{

root.classList.remove(
"dark"
);

setResolvedTheme(
"light"
);

}


};




updateTheme();



localStorage.setItem(
"theme",
theme
);




const listener =
window.matchMedia(
"(prefers-color-scheme: dark)"
);



listener.addEventListener(
"change",
updateTheme
);



return()=>{

listener.removeEventListener(
"change",
updateTheme
);

};



},[theme]);






return (

<ThemeContext.Provider

value={{

theme,

setTheme,

resolvedTheme

}}

>


{children}


</ThemeContext.Provider>


);


}




export function useTheme(){


const context =
useContext(
ThemeContext
);



if(!context){

throw new Error(
"useTheme must be used inside ThemeProvider"
);

}



return context;


}