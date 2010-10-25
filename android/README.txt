MeMoPlayer sur Android r1.1

Pré-requis
----------

Ceci n'est pas un guide d'installation d'Eclipse, ou du SDK Android.
Installer Eclipse, Subclipse ou Subversion (SVN pour Eclipse), le SDK Android ainsi que le plugin Android pour Eclipse.

Les chemins suivant seront utilisés:
 Eclipse: ~/dev/eclipse
 Android: ~/dev/android-sdk
 Workspace: ~/dev/workspace

Configuration d'Eclipse
-----------------------

Créer sous Eclipse une librairie 'User Libraires' pointant sur le JAR Android du SDK.
   Menu Windows => Preferences => Java => Build Path => User Librairies => New
   Nommer la librairie "Android" et faire pointer sur le JAR ~/dev/android-sdk/android.jar

Un MeMoPlayer "compatible" Android
----------------------------------

Le portage Android n'étant pas complet, il est necessaire d'utiliser des sources préprocessées
afin de n'avoir que des appels systèmes gérés par le portage.

1. Créer avec un projet nommé "MeMoPlayer" à partir du depôt SVN:
    File => New => Project... => SVN => Project from SVN => Next
    URL: https://yourdev.rd.francetelecom.fr/svnroot/edlsoutdoor/tags/MeMoPlayer/java
    Nom du project: "MeMoPlayer"
    Le projet contenant les sources Java du MeMoPlayer sont maintenant dans ~/dev/workspace/MeMoPlayer.

2. Modifier le fichier "profile.properties" du projet "MeMoPlayer" pour compiler le projet avec un profil adapté à Android:
    current.profile=Android

3. Lancer le script And Build.xml avec la tâche "Build" pour préparer une version des sources preprocessées.
    Sélectionner Windows => Show View => Ant pour rajouter dans la vue Ant.
    Clisser déposer le fichier "build.xml" du projet "MeMoPlayer" dans la vue Ant.
    Lancer la tâche "Build" pour forcer le preprocesseur préparer le code source.
    Ne pas s'inquiter si le code ne réussi à être compiler. 
    Seule l'étape de preprocessing du code importe à ce stade.
    Un nouveau dossier psrc/ est créé à l'issue de cette étape avec du code compatible Android.

4. Déclarer le dossier psrc/ du projet comme ressource liée:
    Aller dans Preference => General => Workspace => Linked Resources
    Cliquer sur New
    Pour Name, entrer : MEMO_ANDROID_PSRC_PATH
    Pour Location, entrer le chemin ABSOLU du dossier psrc/ : /home/marc/Devel/Android/workspace/MeMoPlayer/psrc
    

Build du MeMoPlayer pour Android
--------------------------------

1. Créer un projet "MeMoAndro" utilisant le projet trunk/MeMoPlayer/android/j2ab_adapter à partir des sources SVN:
    Menu File => New => Project... => SVN => Project from SVN => Next
    URL: https://yourdev.rd.francetelecom.fr/svnroot/edlsoutdoor/trunk/MeMoPlayer/android/j2ab_adapter

2. Pour lancer l'émulateur Android sur le projet MeMoAndro:
    Cliquer avec le bouton droit sur le projet "MeMoAndo" => Run As => Android Application.


Configuration du MeMoPlayer pour Android
----------------------------------------

Les fichiers M4M sont à mettre dans le répertoire src/ du projet "MeMoAndro".
Les paramètres de JAD doivent être mis dans le fichier src/jad.properties.

Pour booter sur une scene "toto.m4m", il suffit de renseigner la ligne suivante:
boot=toto.m4m

L'icône et le nom du MeMoPlayer sous Android peuvent être changés dans le projet "MeMoAndro":
L'icône se trouve sous MeMoAndro/res/drawable/icon.png (dimension 64x64).
Le nom du projet se trouve sous MeMoAndro/res/values/strings.xml => app_name.

Changement de nom de l'application
----------------------------------

Chaque application MeMo sur Android doit avoir son propre nom, mais aussi son propre package
afin que les différentes applications utilisant le MeMo ne s'écrasent pas à l'installation.

Voici les instructions pour appeller une application "MyApp":

1. Changer dans Manifest.xml l'attribut package="com.orange.memoplayer" en package="com.orange.memoplayer.myApp".

2. Changer la string "app_name" dans res/values/strings.xml:

   <string name="app_name">MyApp</string>

Génération du player MeMoAndro.jar pour le MemoSDK
--------------------------------------------------

Le MemoSDK peut générer un APK à partir de n'importe quel projet de widget ou application MeMo
par l'utilisation de la tâche Package.Android.

Pour cela, le MemoSDK a besoin un player MeMo spécifique à Android.

Ce player est un JAR contenant toutes les classes des projets MeMoPlayer et MeMoAndo.
Il doit être placé dans le MemoSDK sous /tools/android/player/MeMoAndro.jar

Pour générer ce player, il suffit :
 1. D'avoir complété les étapes précédentes de ce guide, c'est à dire :
     - avoir un projet MeMoAndo sous Eclipse
     - avoir les sources "preprocessées" avec le profile Android du MeMoPlayer accessibles sous MeMoAndro/psrc
 2. Double cliquer sur le fichier MeMoAndro/ExportForMemoSDK.jardesc
 3. Cliquer sur le bouton "Finish" dans la fenêtre d'"Export de JAR" (ignorer les warnings)
 4. Copier le fichier MeMoAndro/MeMoAndro.jar nouvellement créé dans le MemoSDK sous /tools/android/player/MeMoAndro.jar

