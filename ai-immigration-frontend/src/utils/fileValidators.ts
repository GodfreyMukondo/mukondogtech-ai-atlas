import {
  appConfig,
} from "../config/appConfig";



export function validateFile(
  file: File
): boolean {


  if (
    file.size >
    appConfig.upload.maxFileSize
  ) {

    throw new Error(
      "File size exceeds allowed limit."
    );

  }



  if (
    !(appConfig.upload.allowedTypes as readonly string[]).includes(
      file.type
    )
  ) {

    throw new Error(
      "Unsupported file type."
    );

  }



  return true;

}