import fs from 'fs';

import { spawn } from 'child_process';
import { pythonScript, pythonPath } from './paths.js';
import { RunPythonOptions } from '../types/interfaces.js';

export const runPythonFile = ({ directory, destination_directory, onLog }: RunPythonOptions) => {
  return new Promise((resolve, reject) => {

    onLog('[COMMENT]: Lancement du script Python...');
    // Check if the Python script exists
    if (!fs.existsSync(pythonScript)) {
      return reject(`Le script Python n'existe pas à ce chemin : ${pythonScript}`);
    }

    // Create the command to run the Python script
    const args = [
      '-u', // unbuffered
      pythonScript,
      '--directory', directory,
      '--destination_directory', destination_directory,
    ];

    // Run the Python script
    onLog(`[COMMENT]: Lancement du script Python : ${pythonPath} ${args.join(' ')}`);
    const pythonProcess = spawn(pythonPath, args, { stdio: ['ignore', 'pipe', 'pipe'] });

    // ---- Handle the output and error streams ----
    pythonProcess.stdout.setEncoding('utf8');
    pythonProcess.stderr.setEncoding('utf8');

    // Remove all listeners to avoid duplicate logs
    pythonProcess.stdout.removeAllListeners('data');
    pythonProcess.stderr.removeAllListeners('data');

    // Stdout
    pythonProcess.stdout.on('data', (data) => {
      onLog(`[PYTHON]: ${data}`);
    });

    // Stderr
    pythonProcess.stderr.on('data', (data) => {
      onLog(`[PYTHON ERROR]: ${data}`);
    });

    // Handle process exit
    pythonProcess.on('close', (code) => {
      if (code === 0) {
        resolve('Python script executed successfully');
      } else {
        reject(`Python error with code: ${code}`);
      }
    });
  });
};
