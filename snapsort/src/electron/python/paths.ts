import path from 'path';

import { getScriptsPath } from '../pathResolver.js';

// Directories
export const pythonRootDir = getScriptsPath('python');
export const pythonDir = path.join(pythonRootDir, 'python-3.12.10-amd64');

// Files
export const pythonScript = path.join(pythonRootDir, 'main.py');
export const requirementsPath = path.join(pythonRootDir, 'requirements.txt');
export const pythonPath = path.join(pythonDir, 'python.exe');