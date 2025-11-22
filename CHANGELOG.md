# Changelog - Prop Hunt Mod

Toutes les modifications notables de ce projet seront documentées dans ce fichier.

## [1.0.0] - 2024-XX-XX

### Ajouté
- ✨ Système de jeu Prop Hunt complet
- 👥 Système d'équipes (Props vs Hunters)
- 🎭 Transformation en blocs pour les Props
- ⏱️ Timer de partie avec phases (cachette/chasse)
- 🏆 Système de scores
- 💬 Commandes de jeu complètes :
  - `/prophunt join` - Rejoindre une partie
  - `/prophunt leave` - Quitter la partie
  - `/prophunt start` - Démarrer (admin)
  - `/prophunt stop` - Arrêter (admin)
  - `/prophunt transform` - Se transformer
  - `/prophunt revert` - Redevenir normal
  - `/prophunt status` - Statut de la partie
- 🎨 Interface de sélection de props (basique)
- ⚙️ Système de configuration
- 📝 Documentation complète (README, QUICKSTART)

### Caractéristiques du gameplay
- Phase de cachette : 30 secondes
- Phase de chasse : 5 minutes
- Ratio d'équipes : 1/3 Hunters, 2/3 Props
- Hunters aveugles pendant la phase de cachette
- Props reçoivent un boost de vitesse pour se cacher
- Props invisibles quand transformés
- Détection de fin de partie automatique

### Technique
- Compatible Minecraft 1.21.x
- Basé sur NeoForge 21.1.77
- Nécessite Java 21+
- Architecture modulaire et extensible

## [À venir] - Versions futures

### Prévu pour v1.1.0
- [ ] Rendu 3D complet des props transformés
- [ ] Ajustement dynamique de l'hitbox selon le bloc
- [ ] GUI améliorée avec aperçu 3D des blocs
- [ ] Effets sonores et particules
- [ ] Système de stats persistantes

### Prévu pour v1.2.0
- [ ] Système de maps dédiées
- [ ] Sélection de spawn points
- [ ] Zones interdites configurables
- [ ] Power-ups et objets spéciaux
- [ ] Mode spectateur pour joueurs éliminés

### Prévu pour v1.3.0
- [ ] Système de cosmétiques
- [ ] Achievements/succès
- [ ] Classements et statistiques
- [ ] Support de plusieurs parties simultanées
- [ ] API pour extensions

## Problèmes connus

### v1.0.0
- Le rendu des props utilise seulement l'invisibilité (pas de modèle 3D)
- L'hitbox n'est pas ajustée à la taille du bloc
- Pas de synchronisation réseau avancée pour les transformations
- GUI de sélection basique sans aperçu 3D
- Pas de fichier de configuration externe (valeurs codées en dur)

## Notes de migration

### De rien à v1.0.0
- Première version, installation fraîche

---

Format basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/)
Versioning selon [Semantic Versioning](https://semver.org/lang/fr/)
