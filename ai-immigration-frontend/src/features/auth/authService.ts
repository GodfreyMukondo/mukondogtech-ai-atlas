import axiosClient from "../../api/axios";


/*
|--------------------------------------------------------------------------
| TYPES
|--------------------------------------------------------------------------
*/


export interface LoginRequest {

    email:string;

    password:string;

}



export interface UserResponse {

    id:number;

    fullName:string;

    email:string;

    role:string;

    enabled:boolean;

    mustChangePassword:boolean;

}



export interface AuthResponse {

    token:string;

    tokenType:string;

    user:UserResponse;

}



/*
|--------------------------------------------------------------------------
| LOGIN
|--------------------------------------------------------------------------
*/


export async function loginService(

    request:LoginRequest

):Promise<AuthResponse>{


    const response = await axiosClient.post<AuthResponse>(

        "/auth/login",

        request

    );


    return response.data;

}





/*
|--------------------------------------------------------------------------
| REGISTER
|--------------------------------------------------------------------------
*/


export async function registerService(

    data:any

){


    const response = await axiosClient.post(

        "/auth/register",

        data

    );


    return response.data;

}





/*
|--------------------------------------------------------------------------
| CURRENT USER
|--------------------------------------------------------------------------
*/


export async function getCurrentUserService(){


    const response = await axiosClient.get(

        "/auth/me"

    );


    return response.data;

}





/*
|--------------------------------------------------------------------------
| LOGOUT
|--------------------------------------------------------------------------
*/


export function logoutService(){


    localStorage.removeItem(
        "token"
    );


    localStorage.removeItem(
        "user"
    );


    localStorage.removeItem(
        "role"
    );


}