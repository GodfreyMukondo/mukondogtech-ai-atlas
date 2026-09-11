import API from "../api/axios";


export interface ChatRequest {

  question: string;

  conversationId?: string;

}


export interface ChatResponse {

  answer: string;

  conversationId?: string;

  timestamp?: string;

}



export const sendChatMessage = async (
  data: ChatRequest
): Promise<ChatResponse> => {


  const response = await API.post<ChatResponse>(
    "/chat",
    data
  );


  return response.data;

};



export const getChatHistory = async (
  conversationId?: string
) => {


  const response = await API.get(
    "/chat/history",
    {
      params:{
        conversationId
      }
    }
  );


  return response.data;

};



export const clearChatHistory = async (
  conversationId:string
)=>{


await API.delete(
  `/chat/history/${conversationId}`
);


};