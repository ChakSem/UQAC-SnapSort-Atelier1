import { spawn, spawnSync } from 'child_process';

import { requirementsPath, pythonPath } from './paths.js';
import { SetupPythonSchema } from '../types/interfaces.js';

/**
 * Check if a Nvidia GPU is detected on the system.
 * @description This function attempts to run the `nvidia-smi` command to check for the presence of an Nvidia GPU. If the command executes successfully, it indicates that an Nvidia GPU is present.
 * @returns {boolean} true if an Nvidia GPU is detected, false otherwise.
 */
function hasNvidiaGpu(): boolean {
    try {
        const result = spawnSync('nvidia-smi', [], { stdio: 'ignore' });
        return result.status === 0;
    } catch {
        return false;
    }
}

export async function installRequirements({ onLog }: SetupPythonSchema) {
    try {
        onLog('[COMMENT]: Installation des dépendances...');

        // Helper to run a pip command and stream logs
        const runPip = (args: string[]) => {
            return new Promise<void>((resolve, reject) => {
                const proc = spawn(pythonPath, ['-m', 'pip', ...args]);
                proc.stdout.on('data', data => onLog(`[PROMPT]: ${data.toString()}`));
                proc.stderr.on('data', data => onLog(`[PROMPT ERROR]: ${data.toString()}`));
                proc.on('close', code => {
                    if (code === 0) resolve();
                    else reject(new Error(`pip exited with code ${code}`));
                });
            });
        };

        await runPip(['install', '-r', requirementsPath]);

        if (hasNvidiaGpu()) {
            onLog('[COMMENT]: Installation de torch avec CUDA 12.6 (GPU Nvidia détecté)...');
            await runPip(['install', 'torch', 'torchvision', 'torchaudio', '--index-url', 'https://download.pytorch.org/whl/cu126']);
        } else {
            onLog('[COMMENT]: No Nvidia GPU detected');
        }
    } catch (err) {
        onLog('[COMMENT ERROR]: Erreur durant l\'installation des dépendances : ' + err);
        return;
    }
}