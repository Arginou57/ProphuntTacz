# Prop Hunt Mod pour Minecraft 1.21.x

Un mod de mini-jeu Prop Hunt pour Minecraft utilisant NeoForge.

## Prérequis

- **Java 21 ou supérieur** (OBLIGATOIRE pour NeoForge 1.21.x)
- **IntelliJ IDEA** (recommandé) ou Eclipse
- **Gradle** (inclus via wrapper)

## Installation de l'environnement de développement

### 1. Installer Java 21

Téléchargez Java 21 depuis :
- **Adoptium (recommandé)** : https://adoptium.net/
- **Oracle JDK 21** : https://www.oracle.com/java/technologies/downloads/#java21

Vérifiez l'installation :
```bash
java -version
```

### 2. Importer le projet dans IntelliJ IDEA

1. Ouvrez IntelliJ IDEA
2. File → Open → Sélectionnez le dossier `PropHuntMod`
3. Attendez que Gradle télécharge les dépendances (première fois = long)
4. Une fois terminé, exécutez : `./gradlew build` (Windows: `gradlew.bat build`)

### 3. Configurer les configurations de lancement

IntelliJ devrait créer automatiquement les configurations :
- **runClient** - Lance Minecraft avec le mod
- **runServer** - Lance un serveur avec le mod
- **runData** - Génère les data files

## Structure du Projet

```
PropHuntMod/
├── src/main/java/com/yourname/prophunt/
│   ├── PropHuntMod.java              # Classe principale
│   ├── game/
│   │   ├── GameManager.java          # Gestionnaire de parties
│   │   ├── PropHuntGame.java         # Logique du jeu
│   │   ├── GameState.java            # États du jeu
│   │   └── PropTransformation.java   # Système de transformation
│   ├── teams/
│   │   ├── TeamType.java             # Types d'équipes
│   │   └── PropHuntTeam.java         # Gestion des équipes
│   ├── commands/
│   │   └── PropHuntCommands.java     # Commandes du jeu
│   └── network/
│       └── NetworkHandler.java       # Communication réseau
└── src/main/resources/
    ├── META-INF/mods.toml
    └── pack.mcmeta
```

## Compilation et Test

### Lancer le client de développement
```bash
./gradlew runClient
```

### Lancer le serveur de développement
```bash
./gradlew runServer
```

### Compiler le mod
```bash
./gradlew build
```

Le fichier `.jar` sera généré dans `build/libs/`

## Utilisation du Mod

### Commandes disponibles

#### Pour les joueurs :
- `/prophunt join` - Rejoindre une partie
- `/prophunt leave` - Quitter la partie
- `/prophunt transform` - Se transformer en bloc proche (Props uniquement)
- `/prophunt transform <bloc>` - Se transformer en bloc spécifique (ex: `oak_planks`)
- `/prophunt revert` - Revenir à la forme normale
- `/prophunt status` - Voir le statut de la partie

#### Pour les admins (OP niveau 2+) :
- `/prophunt start` - Démarrer la partie
- `/prophunt stop` - Arrêter la partie

### Comment jouer

1. **Rejoindre** : Les joueurs utilisent `/prophunt join`
2. **Démarrer** : Un admin lance `/prophunt start`
3. **Phase de cachette (30 secondes)** :
   - Les **Props** doivent se cacher et se transformer en blocs
   - Les **Hunters** sont aveugles pendant cette phase
4. **Phase de chasse (5 minutes)** :
   - Les Hunters cherchent et éliminent les Props
   - Les Props doivent survivre
5. **Fin de partie** :
   - **Props gagnent** si au moins un survit jusqu'à la fin du temps
   - **Hunters gagnent** s'ils éliminent tous les Props

### Système d'équipes

- **Props (Vert)** : 2/3 des joueurs
  - Peuvent se transformer en blocs
  - Deviennent invisibles quand transformés
  - Reçoivent un boost de vitesse pendant la phase de cachette

- **Hunters (Rouge)** : 1/3 des joueurs
  - Doivent trouver et éliminer les Props
  - Sont aveugles pendant la phase de cachette

## Personnalisation

### Modifier les durées de jeu

Dans `PropHuntGame.java:20-22` :
```java
private int maxGameTime = 6000; // Durée totale (en ticks, 20 ticks = 1 sec)
private int hideTime = 600;     // Temps de cachette (en ticks)
```

### Modifier le ratio d'équipes

Dans `PropHuntGame.java:117` :
```java
int hunterCount = Math.max(1, playerList.size() / 3); // Actuellement 1/3
```

## Fonctionnalités implémentées

- ✅ Système de jeu complet (phases : attente, cachette, chasse)
- ✅ Transformation en blocs pour les Props
- ✅ Système d'équipes (Props vs Hunters)
- ✅ Timer et gestion des rounds
- ✅ Système de scores
- ✅ Commandes complètes
- ⚠️ Interface graphique (basique, nécessite amélioration client-side)

## À améliorer (TODO)

- [ ] GUI pour sélectionner les blocs de transformation
- [ ] Rendu 3D des props transformés (actuellement invisibilité simple)
- [ ] Effets sonores et particules
- [ ] Système de maps dédiées
- [ ] Statistiques persistantes
- [ ] Configuration via fichier config
- [ ] Support multijoueur amélioré avec sync réseau

## Problèmes connus

- Le rendu des props transformés utilise simplement l'invisibilité
  - Pour un rendu complet, il faut implémenter un EntityRenderer custom
- L'hitbox n'est pas ajustée à la taille du bloc transformé
- Pas de GUI pour la sélection de blocs (utiliser commandes)

## Dépannage

### Le mod ne se compile pas
- Vérifiez que vous avez Java 21+ : `java -version`
- Supprimez le cache Gradle : `./gradlew clean`
- Rechargez le projet : `./gradlew --refresh-dependencies`

### Le jeu crash au lancement
- Vérifiez les logs dans `logs/latest.log`
- Assurez-vous que la version de NeoForge correspond à celle dans `gradle.properties`

### Les transformations ne fonctionnent pas visuellement
- C'est normal, le système utilise l'invisibilité simple
- Le rendu complet nécessite du code client-side additionnel

## Licence

MIT License - Libre d'utilisation et modification

## Crédits

Développé avec NeoForge pour Minecraft 1.21.x
