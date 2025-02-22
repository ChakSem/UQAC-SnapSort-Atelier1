# V0.5 - SnapSort


 <!-- // TODO : Racine a adapter en fonction de chacun pour la demo / test des affichages 
        //NB: Le probleme sera régle automatiquement par le fait que dans la VF ça sera un dossier qui est dans l'arbo de l'application 
        private string rootImageFolder = @"C:\Users\alaac\Pictures"; -->
## Adaptations necessaires pour le bon fonctionnement de l'application
-V_02/21 : Pour pouvoir ouvrir correctement l'onglet Albums , il faut mettre a jour la variable rootImageFolder dans le fichier Form1.cs
```csharp
private string rootImageFolder = @"C:\Users\alaac\Pictures";
```
Il faut remplacer le chemin par le chemin de votre dossier d'images.

## Notes: 
- 22/02/2025 : Ajustement a faire pour le pannel de previsualisation des images (mettre une taille par defaut pour ne pas a voir a redimensionner a chaque fois), (j'ai essaye en faisant en augementant la taille de parent.width et parent.height mais ça n'a pas marché), donc faudra exploerer d'autres pistes.