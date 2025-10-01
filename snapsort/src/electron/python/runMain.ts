import fs from 'fs';
import { spawn } from 'child_process';
import { pythonScript, pythonPath, pythonImageRetrievalScript, pythonFillDatabaseScript } from './paths.js';
import { RunImageRetrievalOptions, RunPythonFillDatabaseOptions, RunPythonOptions } from '../types/interfaces.js';

// Helper function to run a Python script with arguments and logging
function runPythonScriptWithArgs({
  scriptPath,
  args,
  onLog,
  scriptLabel = "Python script"
}: {
  scriptPath: string;
  args: string[];
  onLog: (msg: string) => void;
  scriptLabel?: string;
}) {
  return new Promise((resolve, reject) => {
    onLog(`[COMMENT]: Starting ${scriptLabel}...`);
    if (!fs.existsSync(scriptPath)) {
      return reject(`The script does not exist at path: ${scriptPath}`);
    }

    const fullArgs = ['-u', scriptPath, ...args];
    onLog(`[COMMENT]: Running: ${pythonPath} ${fullArgs.join(' ')}`);
    const pythonProcess = spawn(pythonPath, fullArgs, { stdio: ['ignore', 'pipe', 'pipe'] });

    pythonProcess.stdout.setEncoding('utf8');
    pythonProcess.stderr.setEncoding('utf8');
    pythonProcess.stdout.removeAllListeners('data');
    pythonProcess.stderr.removeAllListeners('data');

    pythonProcess.stdout.on('data', (data) => {
      onLog(`[PYTHON]: ${data}`);
    });

    pythonProcess.stderr.on('data', (data) => {
      onLog(`[PYTHON ERROR]: ${data}`);
    });

    pythonProcess.on('close', (code) => {
      if (code === 0) {
        resolve(`${scriptLabel} executed successfully`);
      } else {
        reject(`Python error with code: ${code}`);
      }
    });
  });
}

// Now use the helper in your exported functions

export const runPythonFile = ({ directory, destination_directory, copy_directory, onLog }: RunPythonOptions) => {
  return runPythonScriptWithArgs({
    scriptPath: pythonScript,
    args: [
      '--directory', directory,
      '--destination_directory', destination_directory,
      '--copy_directory', copy_directory
    ],
    onLog,
    scriptLabel: "Python script"
  });
};

export const runImageRetrieval = ({ prompt, onLog }: RunImageRetrievalOptions) => {
  return runPythonScriptWithArgs({
    scriptPath: pythonImageRetrievalScript,
    args: [
      '--prompt', prompt
    ],
    onLog,
    scriptLabel: "Image retrieval script"
  });
};

export const runPythonFillDatabase = ({ copy_directory, onLog }: RunPythonFillDatabaseOptions) => {
  return runPythonScriptWithArgs({
    scriptPath: pythonFillDatabaseScript,
    args: [
      '--copy_directory', copy_directory
    ],
    onLog,
    scriptLabel: "Python fill database script"
  });
}
