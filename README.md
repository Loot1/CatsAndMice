# 🐭 1-2-3 Modo - Plugin Minecraft

Un plugin interactif pour les serveurs Minecraft qui permet aux joueurs de participer à un jeu de clics compétitif avec affichage des scores en temps réel via un hologramme. Parfait pour les événements communautaires et les animations de serveur.

## 📋 Fonctionnalités

### 🎮 Système de jeu
- Système de score interactif avec hologramme cliquable
- Affichage des 10 derniers clics (réels ou factices)
- Délai configurable entre chaque clic (5 minutes par défaut)
- Meilleur score enregistré avec le nom du joueur
- Interface visuelle attrayante avec codes couleurs

### 🔧 Fonctionnalités avancées
- Gestion complète des permissions
- Sauvegarde automatique des données
- Support des préfixes personnalisés (LuckPerms)
- Système de logs détaillés pour le débogage
- Personnalisation complète des messages

### 🤖 Intégration Discord
- Notifications via webhook pour les scores élevés
- Messages personnalisables avec mentions
- Seuil de score configurable
- Support des rôles et mentions

## 🚀 Installation

### Prérequis
- Serveur Minecraft Spigot/Paper 1.20.6 (ou version supérieure)
- Java 22
- DecentHolograms 2.9.5

### Étapes d'installation
1. Téléchargez la dernière version du plugin
2. Placez le fichier JAR dans le dossier `plugins`
3. Démarrez/Redémarrez votre serveur
4. Utilisez la commande `/clickgame create` pour créer l'hologramme
5. Configurez le plugin via `plugins/ClickGame/config.yml`

## ⚙️ Configuration

### Fichiers principaux
- `plugins/ClickGame/config.yml` - Configuration générale
- `plugins/ClickGame/data.yml` - Sauvegarde des données

### Commandes
| Commande | Permission | Description |
|----------|------------|-------------|
| `/clickgame create` | `clickgame.command` | Crée un nouvel hologramme |
| `/clickgame reload` | `clickgame.command` | Recharge la configuration |

### Permissions
| Permission | Description |
|------------|-------------|
| `clickgame.command` | Accès aux commandes du plugin |
| `clickgame.bypass` | Contourne le délai entre les clics que pour les joueurs |
| `clickgame.reset` | Permet de réinitialiser le score |
| `clickgame.*` | Donne accès à toutes les fonctionnalités |

## 🎨 Personnalisation

### Hologramme
- Titre personnalisable via `hologram.display.title`
- Format des messages de clic/réinitialisation personnalisable
- Affichage des 10 derniers clics
- Gestion des faux joueurs pour maintenir l'affichage

### Messages
Tous les messages sont personnalisables dans le fichier de configuration, notamment :
- Messages de score
- Messages d'erreur
- Messages de notification
- Messages de réinitialisation

## 📊 Optimisation

### Paramètres recommandés
- **Hologramme** :
  - Désactivez les mises à jour inutiles
  - Limitez le nombre de lignes affichées
  - Utilisez des messages courts

- **Performances** :
  - Désactivez les logs en production
  - Augmentez le délai entre les clics si nécessaire
  - Utilisez un serveur dédié pour les webhooks

### Configuration des webhooks
1. Créez un webhook dans les paramètres de votre salon Discord
2. Activez les webhooks dans la configuration
3. Configurez le seuil de notification
4. Personnalisez le message d'alerte

## 📅 Version
**Dernière version** : 1.0.2  
**Dernière mise à jour** : 11/07/2025
   - Activez les webhooks : `webhook.enabled: true`
   - Collez votre URL de webhook : `webhook.url: 'https://discord.com/api/webhooks/...'`
   - Définissez le seuil de score pour les alertes : `webhook.threshold: 100`
   - Activez les mentions si nécessaire : `webhook.mention-enabled: true`
   - Définissez la mention : `webhook.mention: '@everyone'`

### Exemple de configuration complète

```yaml
webhook:
  enabled: true
  url: 'https://discord.com/api/webhooks/votre_url_ici'
  threshold: 100
  mention-enabled: true
  mention: '@everyone'
  alert-message: |-
    **🎮 CLICKGAME - 1-2-3-Modo**
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━
    ⏰***Alerte score élevé***
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━
    🏆 **Joueur:** %player%
    ⚡ **Score atteint:** %score%
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Variables disponibles dans les messages

- `%player%` : Nom du joueur
- `%score%` : Score atteint
- `%date%` : Date et heure actuelles (format configuré dans `settings.time-format`)
- `%best-score%` : Meilleur score actuel
- `%best-player%` : Nom du meilleur joueur

## Dépendances

- [DecentHolograms](https://www.spigotmc.org/resources/96927/) (obligatoire)
- Spigot/Paper 1.20.6 ou supérieur
- Java 17 ou supérieur

## Auteur

Développé par Eniox59
