# Organisation du dossier scripts/python

Ce document décrit l'organisation et le rôle des principaux fichiers Python présents dans le dossier `scripts/python` du projet SnapSort, pour le backend.

Le dossier peut être scindé en 3 grandes parties : 
- Les fichiers pour le tri des images
- Les fichiers pour la recherche par mots-clés
- Les fichiers partagés entre les 2

## Les fichiers partagés entre les 2

- **image_details.py**
  - Classe pour encapsuler toutes les informations extraites d'une image (nom, chemin, exif, objets détectés, description, etc.).
  - Fournit des méthodes pour sérialiser ces informations (to_dict, get_page_content).

  La classe sert pour stocker toutes les informations qu'on récupère d'une image : le nom, le chemin, les coordonnées GPS, ainsi que les objets détectés et la description pour la recherche par mot clés

- **functions.py**
  - Contient des fonctions utilitaires pour la gestion des images, la manipulation de fichiers, la création d'arborescences à partir de CSV, le reverse geocoding, etc.
  - Sert de boîte à outils pour les autres scripts.

## Tri des images

Cette partie a pour objectif de trier automatiquement un ensemble d’images en catégories pertinentes grâce à un pipeline d’analyse combinant nettoyage, clustering, et embeddings visuels (CLIP).

### Étapes du Pipeline

Appel du pipeline dans `main.py`

#### 1. Création du DataFrame avec les métadonnées
Un DataFrame est généré pour contenir les métadonnées extraites de chaque image :
- Nom du fichier
- Chemin
- Date et heure
- Latitude
- Longitude

Ces données sont essentielles pour l’analyse temporelle et spatiale ultérieure.

Fichier : **dataframe_completion.py**
  - Permet de créer un DataFrame pandas à partir d'une liste de chemins d'images et d'extraire les informations pertinentes via `ImageDetails`.
  - Peut sauvegarder le DataFrame en CSV.

---

#### 2. Clustering des images par date et similarité
Les images sont :
- **D’abord regroupées par jour** (selon la date EXIF),
- Puis **clustérisées par similarité visuelle** à l’aide des embeddings CLIP.
L’algorithme de clustering repose sur la similarité des vecteurs d’embeddings entre voisins proches.


Fichiers: 
- **clusterin_manager.py** : 
    - La classe constitue le cœur du système d’analyse visuelle du projet. Elle encapsule toute la logique nécessaire à la transformation d’images en vecteurs d’embedding via le modèle CLIP.
-  **clustering_manager.py**: 
    - La classe hérite de EmbeddingsManager, ce qui lui permet d'accéder directement aux fonctionnalités de génération d'embeddings visuels à l’aide du modèle CLIP
    - Elle fournit une structure de regroupement automatique qui permet :
        - Une analyse visuelle par lot (ex. : scènes similaires dans une même journée),
        - Une optimisation de la catégorisation en traitant des groupes d’images plutôt que des images individuelles.

---

### 3. Nettoyage des images (doublons & flou)
Pour chaque cluster :
- Les **doublons** sont détectés avec la distance de pHash.
- Les **images floues** sont filtrées via la variance du Laplacien.
Les images de meilleure qualité sont conservées.

Fichier : **image_manager.py**
- Classe dédiée au nettoyage automatique des images. Son rôle est d’éliminer les doublons visuels et les images floues afin d’assurer une base de données propre et pertinente.
- Elle est appelée après la création de clusters pour limiter le temps d'execution, qui devient rapidement exponentiel si le nombre d'images à vérifier est trop important. On estime que s'il doit y avoir des doublons, ils seront dans le même cluster.

---

### 4. Attribution des catégories aux clusters
- Un **embedding moyen** est calculé pour chaque cluster.
- Il est comparé à un ensemble d’**embeddings de catégories prédéfinies** (ex : Plage, Ville, Nature…).
- La catégorie avec la meilleure similarité est attribuée à toutes les images du cluster.
- En cas de doute (un résultat est supposé être "Autres" mais avec une autre classe très proche), une logique d’ajustement permet d’assigner une catégorie plus précise que “Autres”.

Fichier : **catégorie_manager.py**
- Classe trop grosse, elle est le départ d'un peu tout (ouais c'est pas la meilleure archi, on travaille dessus)
- Hérite de EmbeddingsManager : accède aux embeddings CLIP d’images. Contient également une instance de :
    - DataframeCompletion → pour gérer le DataFrame des images (Etape 1)
    - ClusteringManager → pour regrouper les images similaires par jour (Etape 2)        
    - ImageCleaner → pour nettoyer les images floues et les doublons (Etape 3)
---

### 5. Sauvegarde des résultats & organisation des dossiers
- Le DataFrame enrichi est sauvegardé dans un fichier `.csv`.
- Les images sont triées et copiées dans des sous-dossiers nommés par date et catégorie. L'arborescence est : Année > Saison > Localisation (si l'image possède les données EXIF) > Date+Catégorie

Fichier : **function.py**

---
---

## Recherche par mots-clés

Cette partie du projet permet d’analyser automatiquement des images via un LLM, de générer des descriptions textuelles enrichies et des listes d’objets détectés, puis de les indexer dans une base vectorielle sous forme d'embeddings. L’utilisateur peut ensuite retrouver les images les plus pertinentes à partir d’une requête en langage naturel.


### 1. Analyse d’images via LLM (description + objets)

Chaque image est analysée via deux prompts :
- Un premier pour **identifier les objets présents**.
- Un second pour **décrire l’image** le plus précisément possible.

Le LLM renvoie :
- une **liste d’objets** (`name`, `description`)
- une **description complète**  
Ces éléments sont fusionnés dans un objet `ImageDetails` puis stockés en base.

Fichier : 
- **llm_call.py**
  - Gère l'appel au modèle de langage (LLM) pour l'analyse d'images.
  - Encode les images, prépare les prompts, interroge le LLM pour obtenir descriptions et objets détectés.
  - Permet de traiter un lot d'images et d'enregistrer les résultats dans une base de données vectorielle. Seules les nouvelles images sont traitées


- **image_details.py**
    - Gère les métadonnées EXIF de l’image (date, GPS…)
    - Regroupe les objets détectés, les descriptions générées et prépare un format textuel utilisable par la base vectorielle (`get_page_content()`)

---

### 2. Indexation dans une base vectorielle (ChromaDB)

Les contenus générés (objets + description) sont stockés sous forme de `Document` dans une base **Chroma**, avec :
- Un `id` unique (UUID)
- Un champ `page_content` textuel
- Des `metadata` (nom du fichier, coordonnées, etc.)

Fichier : **chroma_db.py**
- Classe `ChromaDatabase` :
  - Gère la base de données vectorielle (Chroma) pour l'indexation et la recherche d'images par similarité sémantique.
  - Permet d'ajouter, de rechercher et de nettoyer la base.

---

### 3. Recherche d’images à partir d’un prompt

L'utilisateur saisit une requête en langague naturel

Le prompt est encodé dans le même espace vectoriel que les textes générés, et comparé à tous les `page_content` en base.

Fichier : **image_retrieval.py**
  - Script pour rechercher des images similaires à un prompt textuel dans la base Chroma.
  - Sauvegarde les résultats dans `similar_images.json`. Ces derniers sont les noms de images, triés par ordre décroissant de correspondance avec le texte (les images les plus pertinentes en haut). Nous n'utilisons pas le chemin d'accès car les images seront appelées depuis un autre dossier.


---
---


## Utilisation

Voir le document `lancer_code`


