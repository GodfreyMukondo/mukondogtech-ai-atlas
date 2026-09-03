import API from "./axios";



/**
 * ======================================================
 * REGISTER REQUEST
 * ======================================================
 */

export interface RegisterRequest {

    fullName:string;

    email:string;

    password:string;

}



/**
 * ======================================================
 * LOGIN REQUEST
 * ======================================================
 */

export interface LoginRequest {

    email:string;

    password:string;

}



/**
 * ======================================================
 * CHANGE PASSWORD REQUEST
 * ======================================================
 */

export interface ChangePasswordRequest {

    newPassword:string;

}



/**
 * ======================================================
 * AUTH USER
 * ======================================================
 */

export interface AuthUser {

    id?:number;

    fullName?:string;

    email?:string;

    role?:string;

    enabled?:boolean;

    mustChangePassword?:boolean;

    createdAt?:string;

}



/**
 * ======================================================
 * AUTH RESPONSE
 * ======================================================
 */

export interface AuthResponse {

    token:string;

    tokenType?:string;

    user:AuthUser;

}



/**
 * ======================================================
 * GENERIC RESPONSE
 * ======================================================
 */

export interface ApiResponse {

    success?:boolean;

    message?:string;

}



/**
 * ======================================================
 * API ERROR FORMAT
 * ======================================================
 */

interface ApiErrorResponse {

    message?:string;

    error?:string;

    details?:string;

}



/**
 * ======================================================
 * ERROR PARSER
 * ======================================================
 */

const extractErrorMessage = (

    error:unknown

):string=>{


    if(

        typeof error === "object"

        &&

        error !== null

        &&

        "response" in error

    ){


        const axiosError = error as {

            response?:{

                data?:ApiErrorResponse;

                status?:number;

            };

            message?:string;

        };



        return (

            axiosError.response?.data?.message

            ||

            axiosError.response?.data?.error

            ||

            axiosError.response?.data?.details

            ||

            axiosError.message

            ||

            "An unexpected error occurred."

        );

    }




    if(error instanceof Error){

        return error.message;

    }



    return "An unexpected error occurred.";

};





/**
 * ======================================================
 * REGISTER USER
 * ======================================================
 */

export const registerUser = async(

    data:RegisterRequest

):Promise<AuthResponse>=>{


    try{


        const response =

            await API.post<AuthResponse>(

                "/auth/register",

                data

            );



        return response.data;



    }

    catch(error){


        throw new Error(

            extractErrorMessage(error)

        );

    }

};







/**
 * ======================================================
 * LOGIN USER
 * ======================================================
 */

export const loginUser = async(

    data:LoginRequest

):Promise<AuthResponse>=>{


    try{


        const response =

            await API.post<AuthResponse>(

                "/auth/login",

                data

            );



        return response.data;



    }

    catch(error){


        throw new Error(

            extractErrorMessage(error)

        );

    }

};








/**
 * ======================================================
 * CURRENT AUTHENTICATED USER
 * ======================================================
 */

export const getCurrentUser = async():

Promise<AuthUser>=>{


    try{


        const response =

            await API.get<AuthUser>(

                "/auth/me"

            );



        return response.data;



    }

    catch(error){


        throw new Error(

            extractErrorMessage(error)

        );

    }

};








/**
 * ======================================================
 * CHANGE PASSWORD
 * ======================================================
 *
 * POST /api/auth/change-password
 *
 * Request:
 *
 * {
 *    "newPassword":"StrongPassword123!"
 * }
 *
 * ======================================================
 */

export const changePassword = async(

    data:ChangePasswordRequest

):Promise<ApiResponse>=>{


    try{


        if(

            !data

            ||

            !data.newPassword

            ||

            data.newPassword.trim().length === 0

        ){

            throw new Error(

                "New password is required."

            );

        }



        console.log(

            "CHANGE PASSWORD REQUEST",

            {

                newPasswordLength:

                    data.newPassword.length

            }

        );






        const response =

            await API.post<ApiResponse>(

                "/auth/change-password",

                {

                    newPassword:

                        data.newPassword

                }

            );





        console.log(

            "CHANGE PASSWORD RESPONSE",

            response.data

        );






        return response.data ?? {


            success:true,


            message:

                "Password changed successfully."


        };



    }

    catch(error){



        console.error(

            "CHANGE PASSWORD ERROR",

            error

        );




        throw new Error(

            extractErrorMessage(error)

        );


    }

};








/**
 * ======================================================
 * VERIFY EMAIL
 * ======================================================
 */

export const verifyEmail = async(

    token:string

):Promise<ApiResponse>=>{


    try{


        const response =

            await API.get<ApiResponse>(

                "/auth/verify-email",

                {

                    params:{

                        token

                    }

                }

            );



        return response.data;



    }

    catch(error){


        throw new Error(

            extractErrorMessage(error)

        );

    }

};








/**
 * ======================================================
 * FORGOT PASSWORD
 * ======================================================
 */

export const forgotPassword = async(

    email:string

):Promise<ApiResponse>=>{


    try{


        const response =

            await API.post<ApiResponse>(

                "/auth/forgot-password",

                {

                    email

                }

            );



        return response.data;



    }

    catch(error){


        throw new Error(

            extractErrorMessage(error)

        );

    }

};








/**
 * ======================================================
 * RESET PASSWORD
 * ======================================================
 */

export const resetPassword = async(

    token:string,

    password:string

):Promise<ApiResponse>=>{


    try{


        const response =

            await API.post<ApiResponse>(

                "/auth/reset-password",

                {

                    token,

                    password

                }

            );



        return response.data;



    }

    catch(error){


        throw new Error(

            extractErrorMessage(error)

        );

    }

};








/**
 * ======================================================
 * LOGOUT USER
 * ======================================================
 */

export const logoutUser = ():void=>{


    localStorage.removeItem("token");


    localStorage.removeItem("user");


    localStorage.removeItem("role");


    localStorage.removeItem(
        "mustChangePassword"
    );


};