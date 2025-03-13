# UQAC-SnapSort-Atelier1

## Requirements :

enable long path (mandatory on uqac computers)

```bash
New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force
```

Ensure Microsoft Visual C++ Build Tools and CMake are correctly installed.
You can check using :
```bash
cl
```
```bash
cmake --version
```

I add to set this link in the env variables
```bash
C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Tools\MSVC\14.36.32532\bin\Hostx64\x64
```
if nor working, I added this in
```bash
{
    "terminal.integrated.env.windows": {
    "Path": "C:\\Program Files\\Microsoft Visual Studio\\2022\\Professional\\VC\\Tools\\MSVC\\14.36.32532\\bin\\Hostx64\\x64;${env:Path}"
}
```
Ouvrez VS Code.

Allez dans Fichier > Préférences > Paramètres.

Dans la barre de recherche, tapez terminal.integrated.env.windows.

## Setup

We used python 3.12.6 for this project.

### create a virtual environment
```bash
python -m venv venv
```

### activate the virtual environment
```bash
venv/Scripts/activate
```

### install dependencies
```bash
pip install -r requirements.txt
```

### Start the training
```bash
python run_pixtral.py
```