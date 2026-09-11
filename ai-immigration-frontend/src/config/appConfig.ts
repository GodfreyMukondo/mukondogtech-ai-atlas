/**
 * Application Configuration
 *
 * Global frontend settings.
 *
 * Import this instead of
 * hardcoding values.
 */


import {
  env,
} from "./env";




export const appConfig = {


  app: {


    name:
      env.APP_NAME,


    version:
      env.APP_VERSION,


    environment:
      env.NODE_ENV,


  },





  api:{


    baseURL:
      env.API_URL,


    timeout:
      30000,


  },





  authentication:{


    tokenKey:
      "access_token",


    refreshTokenKey:
      "refresh_token",


    userKey:
      "current_user",



    sessionTimeout:
      60 * 60 * 1000,


  },







  upload:{


    maxFileSize:
      10 * 1024 * 1024,


    allowedTypes:[


      "application/pdf",


      "image/jpeg",


      "image/png",


      "application/msword",


      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",


    ],



    maxFiles:
      10,


  },







  pagination:{


    defaultPage:
      0,


    defaultSize:
      10,


    maxSize:
      100,


  },







  features:{


    analytics:
      env.ENABLE_ANALYTICS,


    debug:
      env.ENABLE_DEBUG,



    aiAssistant:
      true,


    documentAnalysis:
      true,


    payments:
      true,


  },







  routes:{


    home:
      "/",


    login:
      "/login",


    register:
      "/register",


    dashboard:
      "/dashboard",


    documents:
      "/documents",


    profile:
      "/profile",


    settings:
      "/settings",


  },







  storage:{


    prefix:
      "mukondogtech_",


  },




  branding:{


    platformName:
      env.APP_NAME,


    dashboardHeadline:
      "Immigration Command Center",


    dashboardDescription:
      "Enterprise administration dashboard for immigration workflows, artificial intelligence operations, security monitoring and global platform management.",


  },



} as const;




export type AppConfig =
typeof appConfig;



export default appConfig;