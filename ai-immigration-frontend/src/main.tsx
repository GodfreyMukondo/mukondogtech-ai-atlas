import React from "react";
import ReactDOM from "react-dom/client";

import {
  BrowserRouter,
} from "react-router-dom";


import App from "./App";

import "./index.css";


import AuthProvider from "./context/AuthContext";


import {
  ThemeProvider,
} from "./context/ThemeContext";


import {
  NotificationProvider,
} from "./context/NotificationContext";



const rootElement =
  document.getElementById("root");



if (!rootElement) {

  throw new Error(
    "Root element not found"
  );

}



ReactDOM.createRoot(rootElement).render(

  <React.StrictMode>


    <ThemeProvider>


      <NotificationProvider>


        <AuthProvider>


          <BrowserRouter>


            <App />


          </BrowserRouter>


        </AuthProvider>


      </NotificationProvider>


    </ThemeProvider>


  </React.StrictMode>

);