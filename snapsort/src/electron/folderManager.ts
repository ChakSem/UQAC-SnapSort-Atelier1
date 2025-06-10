import fs from 'fs';
import path from 'path';

// Fonction récursive pour obtenir les dossiers
export const getFolders = (folderPath: string): any[] => {
    try {
        return fs.readdirSync(folderPath).map((file) => {
            const fullPath = path.join(folderPath, file);

            // Ignorer les dossiers "unsorted_images" et "temp" à la racine
            if (folderPath === path.resolve(folderPath) && (file === 'unsorted_images' || file === 'temp' || file === 'all_images')) {
                return null; // Ignorer ce dossier
            }

            if (fs.statSync(fullPath).isDirectory()) {
                return {
                    name: file,
                    path: fullPath,
                    children: getFolders(fullPath) // Appel récursif pour les sous-dossiers
                };
            }
            return null;
        }).filter(Boolean); // Enlève les valeurs null (fichiers ou dossiers ignorés)
    } catch (error) {
        console.error("Erreur lecture dossiers:", error);
        return [];
    }
};