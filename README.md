# ClickGame - Plugin Minecraft

Un plugin Minecraft interactif avec un système de score basé sur des clics. Les joueurs peuvent augmenter leur score en cliquant sur l'hologramme avec un délai entre chaque clic. Les administrateurs peuvent contourner ce délai ou réinitialiser le score. Le plugin inclut également des notifications Discord via webhook pour les événements importants.

## Fonctionnalités

- Système de score interactif avec hologramme cliquable
- Meilleur score enregistré automatiquement avec le nom du joueur
- Délai configurable entre chaque clic (5 minutes par défaut)
- Possibilité de contourner le délai avec la permission `clickgame.bypass`
- Réinitialisation du score possible avec la permission `clickgame.reset`
- Gestion facile au niveau des permissions
- Sauvegarde automatique des données dans `data.yml`
- Notifications Discord via webhook pour les scores élevés
- Mentions personnalisables dans les notifications Discord
- Système de logs détaillés pour le débogage

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

#### Paramètres de débogage
- `settings.debug.log-clicks` : Active les logs des clics (par défaut: false)
- `settings.debug.log-records` : Active les logs des records (par défaut: false)
- `settings.debug.log-hologram-update` : Active les logs des mises à jour d'hologramme (par défaut: false)

#### Configuration des webhooks Discord
- `webhook.enabled` : Active ou désactive les notifications Discord (par défaut: false)
- `webhook.url` : URL du webhook Discord (remplacer par votre URL)
- `webhook.threshold` : Seuil de score pour déclencher une notification (par défaut: 100)
- `webhook.mention-enabled` : Active les mentions dans les notifications (par défaut: false)
- `webhook.mention` : Mention à utiliser (@everyone, @here ou <@&ROLE_ID>)
- `webhook.alert-message` : Message personnalisé pour les notifications d'alerte

### data.yml
Sauvegarde des scores et du meilleur joueur.

## Optimisation

### Configuration recommandée pour de meilleures performances

#### Paramètres de débogage
- `log-clicks`: Désactivez en production (`false`) pour réduire l'utilisation du CPU
- `log-records`: Activez uniquement si nécessaire pour le débogage
- `log-hologram-update`: Désactivez en production pour réduire la charge serveur

#### Optimisation des webhooks
- Évitez d'utiliser `@everyone` dans les mentions si vous avez beaucoup de membres
- Augmentez le seuil (`webhook.threshold`) pour réduire le nombre de notifications
- Désactivez les webhooks si non nécessaires

#### Gestion de la mémoire
- Le plugin utilise un cache pour les messages fréquemment utilisés
- Les données sont sauvegardées de manière asynchrone pour éviter les ralentissements
- Les tâches planifiées sont optimisées pour minimiser l'impact sur les performances

#### Recommandations pour les serveurs chargés
1. Augmentez le `click-delay` si nécessaire (par exemple à 10 minutes)
2. Désactivez les animations inutiles dans la configuration
3. Limitez les logs au strict nécessaire
4. Utilisez un serveur dédié pour les webhooks si vous avez beaucoup de trafic

## Configuration des Webhooks Discord

### Création d'un webhook Discord

1. **Créer un webhook** :
   - Allez dans les paramètres de votre salon Discord (icône d'engrenage)
   - Sélectionnez "Intégrations" puis "Créer un webhook"
   - Personnalisez le nom et l'avatar du webhook si nécessaire
   - Copiez l'URL du webhook généré

2. **Configuration du plugin** :
   - Ouvrez le fichier `config.yml`
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
