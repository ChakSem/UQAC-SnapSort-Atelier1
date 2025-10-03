import Store from "electron-store";
import { SettingsSchema, GlobalVarsSchema } from "./types/interfaces.js";

// Store the user settings
const store = new Store<SettingsSchema>({
  name: "settings",
  defaults: {
    directoryPath: "",
    nbrOfFilesLoaded: 10,
  },
});

// Store global variables
const globalStore = new Store<GlobalVarsSchema>({
  name: "global",
  defaults: {
    AIFillingDatabase: false,
    AIFillingDatabaseProgress: 0,

    AISorting: false,
    AISortingProgress: 0,
    
    AISearching: false,
    AISearchingProgress: 0,
  },
});

export { store, globalStore };