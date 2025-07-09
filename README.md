# ClickGame - Plugin Minecraft

Un plugin Minecraft interactif avec un système de score basé sur des clics. Les joueurs peuvent augmenter leur score en cliquant sur l'hologramme avec un délai entre chaque clic. Les administrateurs peuvent contourner ce délai. Le plugin inclut également des notifications Discord via webhook pour les événements importants.

## Fonctionnalités

- Système de score interactif avec hologramme cliquable
- Meilleur score enregistré automatiquement avec le nom du joueur
- Délai configurable entre chaque clic (5 minutes par défaut)
- Possibilité de contourner le délai avec une permission
- Gestion facile au niveaux des permissions
- Sauvegarde automatique des données dans `data.yml`
- Notifications Discord via webhook pour les événements importants
- Mentions personnalisables dans les notifications Discord

## Commandes

- `/clickgame create` - Créer un nouvel hologramme à votre position `!!Attention le créé sous les pieds!!` (nécessite `clickgame.command`)
- `/clickgame reload` - Recharger la configuration (nécessite `clickgame.command`)

## Permissions

- `clickgame.command` - Accès aux commandes `/clickgame` 
- `clickgame.bypass` - Contourne le délai entre les clics 
- `clickgame.reset` - Permet de réinitialiser le score en cliquant sur l'hologramme 
- `clickgame.*` - Donne accès à toutes les fonctionnalités 

## Installation

1. Téléchargez la dernière version du plugin
2. Placez le fichier JAR dans le dossier `plugins` de votre serveur
3. Installez [DecentHolograms](https://www.spigotmc.org/resources/96927/) si ce n'est pas déjà fait
4. Redémarrez le serveur
5. Utilisez `/clickgame create` pour créer l'hologramme à votre position actuelle

## Configuration

Le plugin crée automatiquement les fichiers de configuration dans `plugins/ClickGame/` :

### config.yml
Configuration des messages et des paramètres du jeu :

#### Paramètres généraux
- `settings.click-delay` : Délai en secondes entre chaque clic (par défaut: 300 secondes / 5 minutes)
- `settings.wait-message` : Message affiché lorsqu'un joueur doit attendre avant de pouvoir cliquer à nouveau
- `settings.new-best-score` : Message affiché lorsqu'un nouveau meilleur score est atteint
- `settings.time-format` : Format de l'heure affichée dans les messages
- `settings.debug.log-records` : Active les logs détaillés pour le débogage (par défaut: false)

#### Configuration des webhooks Discord
- `webhook.enabled` : Active ou désactive les notifications Discord (par défaut: false)
- `webhook.url` : URL du webhook Discord (remplacer par votre URL)
- `webhook.threshold` : Seuil de score pour déclencher une notification (par défaut: 100)
- `webhook.mention-enabled` : Active les mentions dans les notifications (par défaut: false)
- `webhook.mention` : Mention à utiliser (@everyone, @here ou <@&ROLE_ID>)
- `webhook.alert-message` : Message personnalisé pour les notifications d'alerte

### data.yml
Sauvegarde des scores et du meilleur joueur (ne pas modifier manuellement)

## Configuration des Webhooks Discord

### Création d'un webhook Discord

1. **Créer un webhook** :
   - Allez dans les paramètres de votre serveur Discord
   - Sélectionnez "Intégrations" puis "Créer un webhook"
   - Copiez l'URL du webhook généré

2. **Configuration du plugin** :
   - Ouvrez le fichier `config.yml`
   - Activez les webhooks : `webhook.enabled: true`
   - Collez votre URL de webhook : `webhook.url: 'https://discord.com/api/webhooks/...'`
   - Personnalisez les autres paramètres selon vos besoins

### Exemple de configuration complète

```yaml
webhook:
  enabled: true
  url: 'https://discord.com/api/webhooks/votre_url_ici'
  username: 'ClickGame Bot'
  avatar-url: 'https://i.imgur.com/example.png'
  threshold: 100
  mention-enabled: true
  mention: '@everyone'
  alert-message: |-
    🎉 **Nouveau record !**
    👤 Joueur: %player%
    🏆 Score: %score%
    ⏰ Date: %date%
```

### Variables disponibles dans les messages

- `%player%` : Nom du joueur
- `%score%` : Score atteint
- `%date%` : Date et heure actuelles (format configuré dans `settings.time-format`)

## Dépendances

- [DecentHolograms](https://www.spigotmc.org/resources/96927/) (obligatoire)
- Spigot/Paper 1.20.6 ou supérieur
- Java 17 ou supérieur

## Auteur

Développé par Eniox59
