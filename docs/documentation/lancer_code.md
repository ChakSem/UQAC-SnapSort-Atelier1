# Lancer les différents scripts

## Tri des photos

Suivre le README dans le root, pour lancer l'interface.


## Recherche par mots-clés

### Requirements

- Télécharger toutes les librairies de requierements.txt :

    ```pip install -r .\snapsort\scripts\python\requirements.txt```

- Installer Ollama : https://ollama.com/download 

- Télécharger les modèles `gemma3` et `mxbai-embed-large`: 

    ```ollama pull gemma3``` ou ```ollama run gemma3``` et ensuite quitter (Ctrl+d)
    ```ollama pull mxbai-embed-large``` ou ```ollama run mxbai-embed-large``` 

    Si vous voulez utiliser d'autres modèles, il faudra juste les changer dans le `__main__` du fichier `llm_call.py`
    
### Ajouter des images dans la base de données Chroma

- Commencer par s'assurer qu'ollama tourne ( ```ollama run gemma3``` et ensuite quitter)

- Modifier le chemin de départ (`directory`) du `__main__` du fichier `llm_call.py`

- Aller dans le dossier `snaport` : 

    ```cd \snapsort ``` 

    => Permet de s'assurer que le code continuera de fonctionner lorsqu'il sera raccroché dans l'interface

    => Crée/Récupère la base de donnée au bon endroit 

- Lancer le main

### Récupérer les images similaires 

- Commencer par s'assurer qu'ollama tourne ( ```ollama run gemma3``` et ensuite quitter)


- Aller dans le dossier `snaport` (voir explications en haut) : 

- Lancer :

     ```python .\scripts\python\image_retrieval.py --prompt "ton prompt"```

     Sinon on peut aussi changer le prompt dans le code, sans prendre d'arguments

Le programme retournera un .json au chemin `snapsort\scripts\temp_files\similar_images.json` avec le nom des images ainsi que leur score de similarité avec le prompt. NOTE : la métrique de similitude utilisée est L2 donc plus le score est petit, plus l'image est proche --> On veut un petit score 



