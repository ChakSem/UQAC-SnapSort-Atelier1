import fs from 'fs';

import { pythonPath } from './paths.js';
import { installPython } from './installPython.js';
import { installRequirements } from './installRequirements.js';
import { SetupPythonSchema } from '../types/interfaces.js';

export async function setupPythonEnv({ onLog }: SetupPythonSchema) {

    // 1. Download Python if not installed locally
    onLog('[COMMENT]: Checking if Python is installed...');
    if (!fs.existsSync(pythonPath)) {
        await installPython({ onLog });
    } else {
        onLog('[COMMENT]: Python already installed.');
    }

    // 2. Install dependencies from requirements.txt
    await installRequirements({ onLog });
    onLog('[COMMENT]: Python environment ready!');
}