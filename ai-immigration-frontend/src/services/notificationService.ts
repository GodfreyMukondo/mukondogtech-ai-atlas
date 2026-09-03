/**
 * Notification Service
 *
 * Central place for all user notifications.
 */


export type NotificationType =
  | "success"
  | "error"
  | "warning"
  | "info";



export interface NotificationPayload {

  type: NotificationType;

  message: string;

  duration?: number;

}



class NotificationService {


  private listeners:
    Array<
      (
        notification:
        NotificationPayload,
      ) => void
    > = [];



  /**
   * Subscribe UI components
   */
  subscribe(
    callback:
      (
        notification:
        NotificationPayload,
      ) => void,
  ) {


    this.listeners.push(
      callback,
    );


    return () => {

      this.listeners =
        this.listeners.filter(
          listener =>
            listener !== callback,
        );

    };

  }



  /**
   * Send notification
   */
  notify(
    payload:
      NotificationPayload,
  ) {


    this.listeners.forEach(
      listener =>
        listener(payload),
    );



    if (
      import.meta.env.DEV
    ) {

      console.log(
        `[${payload.type}]`,
        payload.message,
      );

    }

  }




  success(
    message:string,
  ) {

    this.notify({

      type:"success",

      message,

      duration:3000,

    });

  }




  error(
    message:string,
  ) {

    this.notify({

      type:"error",

      message,

      duration:5000,

    });

  }





  warning(
    message:string,
  ) {

    this.notify({

      type:"warning",

      message,

      duration:4000,

    });

  }





  info(
    message:string,
  ) {

    this.notify({

      type:"info",

      message,

      duration:3000,

    });

  }



}



export const notificationService =
  new NotificationService();


export default notificationService;