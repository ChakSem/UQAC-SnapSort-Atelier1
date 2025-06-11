import { contextBridge, ipcRenderer } from 'electron';

let logHandler: ((event: any, msg: string) => void) | null = null;

contextBridge.exposeInMainWorld('electron', {
    // Python script
    runPython: () => ipcRenderer.invoke('run-python'),
    runImageRetrival: (prompt: string) => ipcRenderer.invoke('run-image-retrieval', prompt),
    onPythonLog: (callback: (msg: string) => void) => {
        logHandler = (_, msg) => callback(msg);
        ipcRenderer.on("log", logHandler);
    },
    removePythonLogListener: () => {
        if (logHandler) {
            ipcRenderer.removeListener("log", logHandler);
            logHandler = null;
        }
    },
    onPythonEnd: (callback: () => void) => {
        ipcRenderer.on("python-end", callback);
    },
    removePythonEndListener: (callback: () => void) => {
        ipcRenderer.removeListener("python-end", callback);
    },

    // Settings
    selectDirectory: () => ipcRenderer.invoke('select-directory'),
    getSetting: (key: string) => ipcRenderer.invoke("get-setting", key),
    setSetting: (key: string, value: any) => ipcRenderer.invoke("set-setting", key, value),

    // Albums and Unsorted Images
    getMediaFiles: (key: string) => ipcRenderer.invoke("get-media-files", key),
    getGlobalVariables: (key: string) => ipcRenderer.invoke("get-global-variables", key),
    setGlobalVariables: (key: string, value: any) => ipcRenderer.invoke("set-global-variables", key, value),

    // Connectivity
    startHotspot: () => ipcRenderer.invoke("start-hotspot"),
    getIpAdress: () => ipcRenderer.invoke("get-ip"),
    getFolders: (key: string) => ipcRenderer.invoke("get-folders", key),
    startImageTransferService: () => ipcRenderer.invoke('start-image-transfer-service'),
    stopImageTransferService: () => ipcRenderer.invoke('stop-image-transfer-service'),
    generateTransferQRCode: (wifiString: string, serverIp: string) => 
        ipcRenderer.invoke('generate-transfer-qrcode', wifiString, serverIp),
    getTransferServiceStatus: () => ipcRenderer.invoke('get-transfer-service-status'),
    getWifiInfo: () => ipcRenderer.invoke('get-wifi-info'),

    // API pour les événements de transfert
    on: (channel: string, callback: (...args: any[]) => void) => {
        const validChannels: readonly string[] = [
            'transfer:start',
            'transfer:progress',
            'transfer:complete',
            'transfer:error',
            'transfer:service-started',
            'transfer:service-stopped'
        ];

        if (validChannels.includes(channel)) {
            const subscription = (_event: any, ...args: any[]) => callback(...args);
            ipcRenderer.on(channel, subscription);

            return () => {
                ipcRenderer.removeListener(channel, subscription);
            };
        }

        return () => {}; // Retourne une fonction vide si le canal n'est pas valide
    },

    off: (channel: string, callback: (...args: any[]) => void) => {
        const validChannels: readonly string[] = [
            'transfer:start',
            'transfer:progress',
            'transfer:complete',
            'transfer:error',
            'transfer:service-started',
            'transfer:service-stopped'
        ];

        if (validChannels.includes(channel)) {
            ipcRenderer.removeListener(channel, callback);
        }
    }
});