import fs from "fs";
import path from "path";
import ffmpeg from "fluent-ffmpeg";
import { exec } from "child_process";

export function isDev() {
  return process.env.NODE_ENV === 'development';
}

// Fonction pour nettoyer le dossier temp
export function cleanTempFolder(directoryPath: string, tempDirectoryPath: string): void {

  if (fs.existsSync(tempDirectoryPath)) {
    // fs.readdirSync(tempDirectoryPath).forEach(file => {
    //   fs.unlinkSync(path.join(tempDirectoryPath, file));
    // });
  } else {
    fs.mkdirSync(tempDirectoryPath);
  }
}

/**
 * Generates a thumbnail image from a video file using ffmpeg.
 *
 * @param videoPath - The full path to the video file.
 * @param tempDirectoryPath - The directory where the thumbnail image will be saved.
 * @returns Promise<string> - Resolves with the path to the generated thumbnail image if successful, rejects with an error otherwise.
 *
 * This function ensures the temporary directory exists, then uses ffmpeg to capture a frame
 * (at 1% of the video duration) and saves it as a JPEG image in the specified directory.
 * The thumbnail will have the same base name as the video file, with a .jpg extension.
 */
export function generateThumbnail(videoPath: string, tempDirectoryPath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    // Ensure the temp directory exists
    if (!fs.existsSync(tempDirectoryPath)) {
      fs.mkdirSync(tempDirectoryPath, { recursive: true });
    }
    
    // Extract the base name of the video file without extension
    // and create the thumbnail path
    const baseName = path.basename(videoPath, path.extname(videoPath));
    const thumbnailPath = path.join(tempDirectoryPath, `${baseName}.jpg`);

    ffmpeg(videoPath)
      .screenshots({
        timestamps: ["1%"], // Capture 1% of the video
        filename: `${baseName}.jpg`, // Use the base name of the video
        folder: tempDirectoryPath,
        size: "320x240",
      })
      .on("end", () => resolve(thumbnailPath))
      .on("error", err => reject(err));
  });
}

/**
 * Execute a PowerShell command and return its output as a Promise.
 *
 * @param command - The PowerShell command to execute.
 * @param errorContext - A string describing the context, used in error logs.
 * @returns Promise<string> - Resolves with the trimmed stdout output if successful, rejects with an error message otherwise.
 *
 * This function runs the given PowerShell command using Node's child_process.exec.
 * If the command fails or outputs to stderr, it logs the error with the provided context and rejects the Promise.
 * Otherwise, it resolves the Promise with the command's stdout output.
 */
export function execPowerShell(command: string, errorContext: string): Promise<string> {
    return new Promise((resolve, reject) => {
        exec(command, (error, stdout, stderr) => {
            if (error) {
                console.error(`${errorContext}: ${error.message}`);
                reject(`Erreur: ${error.message}`);
                return;
            }
            if (stderr) {
                console.error(`${errorContext}: ${stderr}`);
                reject(`Erreur PowerShell: ${stderr}`);
                return;
            }
            resolve(stdout.trim());
        });
    });
}