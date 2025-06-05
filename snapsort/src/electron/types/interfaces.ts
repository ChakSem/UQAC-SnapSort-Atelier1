export interface RunPythonOptions {
  directory: string;
  destination_directory: string;
  onLog: (data: string) => void;
}

export interface SetupPythonSchema {
  onLog: (data: string) => void;
}

export interface SettingsSchema {
  directoryPath: string;
  nbrOfFilesLoaded: number;
}

export interface GlobalVarsSchema {
  AIProcessing: boolean;
  // Ajoute ici d'autres variables globales si besoin
}