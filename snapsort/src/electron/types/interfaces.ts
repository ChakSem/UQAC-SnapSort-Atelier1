export interface RunPythonOptions {
  directory: string;
  destination_directory: string;
  copy_directory: string;
  onLog: (data: string) => void;
}

export interface RunImageRetrievalOptions {
  prompt: string;
  onLog: (data: string) => void;
}

export interface RunPythonFillDatabaseOptions {
  copy_directory: string;
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
  AIFillingDatabase: boolean;
  AIFillingDatabaseProgress: number;
  AISorting: boolean;
  AISortingProgress: number;
  AISearching: boolean;
  AISearchingProgress: number;
}