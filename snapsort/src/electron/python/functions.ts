import { BrowserWindow, ipcMain } from 'electron';
import path from 'path';
import fs from 'fs';
import { lireListeImages } from '../util.js';
import { store, globalStore } from "../store.js";
import { runPythonRetreiveImages, runPythonFillDatabase, runPythonSortImages } from './runPythonFiles.js';
import { setupPythonEnv } from './setupPythonEnv.js';
import { getScriptsPath } from '../pathResolver.js';

// Run Python Sort Images
const sortImages = async (win: BrowserWindow) => {

  // Set global variable AIProcessing to true
  globalStore.set("AISorting", true);

  // Define the log/error forwarding functions ONCE
  const forwardLog = (msg: string) => win.webContents.send('log-python-sorting', msg);

  // Get the root path
  const rootPath = store.get("directoryPath") as string;
  if (!rootPath) {
    return { error: "No root directory path set" };
  }

  // Get the unsorted images path
  const unsortedImagesPath = path.join(rootPath, "unsorted_images");
  if (!fs.existsSync(unsortedImagesPath)) {
    console.log("No images to sort : unsorted_images folder not found");
    return unsortedImagesPath;
  }
  // Check if the folder is not empty
  const files = fs.readdirSync(unsortedImagesPath);
  if (files.length === 0) {
    console.log("No images to sort : unsorted_images folder is empty");
    return unsortedImagesPath;
  }

  // Get the albums path
  const albumsPath = path.join(rootPath, 'albums');
  if (!fs.existsSync(albumsPath)) {
    fs.mkdirSync(albumsPath, { recursive: true });
  }

  // Check if the "all_images" folder exists
  const allImagesPath = path.join(rootPath, 'all_images');
  if (!fs.existsSync(allImagesPath)) {
    // Si le dossier n'existe pas, le créer
    fs.mkdirSync(allImagesPath, { recursive: true });
  }

  // Check if the Python environment is ready
  await setupPythonEnv({ onLog: forwardLog });

  // Execute the Python sorting script
  forwardLog("[COMMENT]: unsortedImagesPath:" + unsortedImagesPath);
  forwardLog("[COMMENT]: albumsPath: " + albumsPath);
  forwardLog("[COMMENT]: allImagesPath: " + allImagesPath);
  forwardLog("[COMMENT]: Running Python sorting script...");

  await runPythonSortImages({
    directory: unsortedImagesPath,
    destination_directory: albumsPath,
    copy_directory: allImagesPath,
    onLog: forwardLog,
  });

  // Set global variable AISorting to false
  globalStore.set("AISorting", false);
  globalStore.set("AISortingProgress", 0);
  win.webContents.send('python-sorting-end');
};


  
// Run Python Image Retrieval
const retrieveImages = async (win: BrowserWindow, prompt: string) => {
  // Define the log/error forwarding functions ONCE
  const forwardLog = (msg: string) => win.webContents.send('log-python-retrieval', msg);

  // Check if the Python environment is ready
  await setupPythonEnv({ onLog: forwardLog });

  await runPythonRetreiveImages({
    prompt: prompt,
    onLog: forwardLog,
  });

  // Get temp files
  const tempFilesPath = getScriptsPath("temp_files");
  //check if directory exists
  if (!fs.existsSync(tempFilesPath)) {
    return { error: "The directory 'temp_files' has not been found" };
  }

  // Get json file
  const jsonPath = path.join(tempFilesPath, "similar_images.json");
  if (!fs.existsSync(jsonPath)) {
    return { error: "No json file found" };
  }

  // Read the json file
  const ImagesList = lireListeImages(jsonPath);

  win.webContents.send('python-retrieval-end');
  return ImagesList;
};
  
// Run Python Fill Database
const fillDatabase = async (win: BrowserWindow) => {
  // Define the log/error forwarding functions ONCE
  const forwardLog = (msg: string) => win.webContents.send('log-python-database', msg);

  // Get the root path
  const rootPath = store.get("directoryPath") as string;
  if (!rootPath) return { error: "No root directory path set" };

  // Check if the "all_images" folder exists
  const allImagesPath = path.join(rootPath, 'all_images');
  if (!fs.existsSync(allImagesPath)) {
    return { error: "No images to fill database: all_images folder not found" };
  }

  // Check if the Python environment is ready
  await setupPythonEnv({ onLog: forwardLog });

  // Execute the Python database filling script
  forwardLog("[COMMENT]: allImagesPath: " + allImagesPath);
  forwardLog("[COMMENT]: Running Python script to fill database...");

  await runPythonFillDatabase({
    copy_directory: allImagesPath,
    onLog: forwardLog,
  });

  win.webContents.send('python-database-end');
};

export { sortImages, retrieveImages, fillDatabase };