import path from 'path';
import fs from 'fs';
import https from 'https';
import unzipper from 'unzipper';
import { pythonRootDir, pythonDir } from './paths.js';
import { SetupPythonSchema } from '../types/interfaces.js';

export async function installPython({ onLog }: SetupPythonSchema) {
    const url = 'https://www.python.org/ftp/python/3.12.10/python-3.12.10-amd64.zip';
    const zipPath = path.join(pythonRootDir, 'python-3.12.10.zip');

    if (onLog) onLog('[COMMENT]: Downloading Python (python-3.12.10-amd64.zip)...');

    const file = fs.createWriteStream(zipPath);
    await new Promise((resolve, reject) => {
        https.get(url, (response) => {
            response.pipe(file);
            file.on('finish', () => {
                file.close(resolve);
            });
        }).on('error', (err) => {
            onLog('[HTTP ERROR]: ' + err.message);
            reject(err);
        });
    });

    onLog('[COMMENT]: Python downloaded.');
    onLog('[COMMENT]: Extracting Python...');

    // Extract to the correct folder
    await fs.createReadStream(zipPath)
        .pipe(unzipper.Extract({ path: pythonDir }))
        .promise();
    // Delete the zip after extraction
    fs.unlinkSync(zipPath);

    onLog('[COMMENT]: Python extracted.');
}