# Guide de Démarrage Rapide - Prop Hunt Mod

## 🚀 Installation en 5 minutes

### Étape 1 : Vérifier Java 21
```bash
java -version
```
**Doit afficher Java 21 ou supérieur !** Sinon, téléchargez depuis https://adoptium.net/

### Étape 2 : Ouvrir le projet
1. Lancez IntelliJ IDEA
2. File → Open → Sélectionnez le dossier `PropHuntMod`
3. Attendez que Gradle télécharge tout (5-10 minutes la première fois)

### Étape 3 : Lancer le jeu
Dans le terminal IntelliJ :
```bash
./gradlew runClient
```
(Windows: `gradlew.bat runClient`)

Le jeu Minecraft va se lancer avec le mod installé !

---

## 🎮 Test rapide du mod

Une fois Minecraft lancé :

1. **Créer un monde** en Creative ou avec cheats activés
2. **Devenir OP** : Appuyez sur `T` et tapez `/op VotreNom`
3. **Tester les commandes** :
   ```
   /prophunt join
   /prophunt start
   /prophunt transform
   ```

---

## 📝 Commandes essentielles

| Commande | Description |
|----------|-------------|
| `/prophunt join` | Rejoindre la partie |
| `/prophunt start` | Démarrer (admin) |
| `/prophunt transform` | Se transformer en bloc proche |
| `/prophunt transform oak_planks` | Se transformer en planches de chêne |
| `/prophunt revert` | Redevenir normal |
| `/prophunt status` | Voir l'état de la partie |
| `/prophunt stop` | Arrêter la partie (admin) |

---

## 🎯 Scénario de test complet

### Test solo (mode créatif)
```bash
1. /op VotreNom
2. /prophunt join
3. /prophunt start
4. /prophunt transform barrel
5. /prophunt revert
```

### Test multijoueur (2+ joueurs)
```bash
# Joueur 1 (admin)
/op Joueur1
/prophunt join
[Attendre les autres joueurs]
/prophunt start

# Autres joueurs
/prophunt join
[Attendre le démarrage]

# Props (pendant la phase de cachette - 30 sec)
/prophunt transform
[Se cacher dans l'environnement]

# Hunters (après 30 sec)
[Chercher et éliminer les props]
```

---

## 🛠️ Compilation du mod

Pour créer le fichier .jar :
```bash
./gradlew build
```

Le fichier sera dans : `build/libs/prophunt-1.0.0.jar`

---

## ⚡ Résolution rapide de problèmes

### "java.lang.UnsupportedClassVersionError"
→ Vous n'avez pas Java 21. Installez-le depuis https://adoptium.net/

### "Task 'runClient' not found"
→ Rechargez Gradle :
```bash
./gradlew --refresh-dependencies
```

### Le mod ne démarre pas
→ Vérifiez les logs : `logs/latest.log`

### Gradle est lent
→ Normal la première fois ! Cela télécharge Minecraft et NeoForge.

---

## 🎨 Personnaliser le mod

### Changer la durée du jeu
Fichier : `PropHuntGame.java:21`
```java
private int maxGameTime = 6000; // 6000 ticks = 5 minutes
private int hideTime = 600;     // 600 ticks = 30 secondes
```

### Changer le ratio d'équipes
Fichier : `PropHuntGame.java:117`
```java
int hunterCount = Math.max(1, playerList.size() / 3); // 1/3 = hunters
```

### Ajouter des blocs de transformation
Fichier : `PropSelectionScreen.java:26`
```java
availableBlocks.add(Blocks.VOTRE_BLOC_ICI);
```

---

## 📚 Prochaines étapes

1. Lisez le **README.md** complet pour plus de détails
2. Explorez le code dans `src/main/java/`
3. Testez avec des amis !
4. Personnalisez le mod selon vos envies

---

## 💡 Astuces de développement

- **Hot reload** : Certains changements nécessitent de relancer le jeu
- **Logs** : Utilisez `PropHuntMod.LOGGER.info("message")` pour déboguer
- **Tests rapides** : Utilisez le mode Creative pour tester rapidement

---

## 🆘 Besoin d'aide ?

- Documentation NeoForge : https://docs.neoforged.net/
- Discord NeoForged : https://discord.neoforged.net/
- Wiki Minecraft modding : https://forge.gemwire.uk/wiki/Main_Page

Bon modding ! 🎮
