# CatsAndMice - Minecraft Plugin

An interactive plugin for Minecraft servers that lets players participate in a competitive click game with real-time score display via a clickable hologram. Perfect for community events and server animations.

## 📋 Features

### 🎮 Game System
- Interactive score system with a clickable hologram
- Displays the last N clicks (real or fake players to fill empty slots)
- Configurable delay between each click (5 minutes by default)
- Best score recorded with the player's name, date and time
- Attractive visual interface with color codes

### 🔧 Advanced Features
- Full permission management
- Automatic data saving (async + sync on shutdown)
- LuckPerms prefix support (soft dependency)
- Configurable time format for hologram messages
- "Recent on bottom" or "recent on top" display order
- Mock (fake) player names to always fill the hologram
- Fully customizable messages

### 🤖 Discord Integration
- Webhook notifications for high scores
- Customizable alert messages with placeholders
- Configurable score threshold
- Optional role/user mentions
- Console alert logging

## 🚀 Installation

### Requirements
- Spigot/Paper Minecraft server 1.20.6 or higher
- Java 17 or higher
- [DecentHolograms 2.9.5+](https://www.spigotmc.org/resources/96927/) (**required**)
- [LuckPerms](https://luckperms.net/) (optional, for rank prefix display)

### Steps
1. Download the latest plugin JAR
2. Place the JAR file in the `plugins` folder
3. Start/Restart your server
4. Use `/mice create` to create the hologram at your location
5. Configure the plugin via `plugins/CatsAndMice/config.yml`

## ⚙️ Configuration

### Main Files
- `plugins/CatsAndMice/config.yml` — General configuration (messages, settings, webhook)
- `plugins/CatsAndMice/data.yml` — Persistent data (clicks history, hologram location)

### Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/mice create` | `catsandmice.create` | Creates a new game hologram at your location |
| `/mice reload` | `catsandmice.reload` | Reloads the configuration |
| `/mice help` | `catsandmice.help` | Displays the help message |

### Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `catsandmice.*` | Grants all plugin permissions | OP |
| `catsandmice.mice` | Allows playing the game (clicking the hologram) | OP |
| `catsandmice.cat` | Allows resetting the score by clicking the hologram | OP |
| `catsandmice.bypass` | Bypasses the click cooldown delay | OP |
| `catsandmice.create` | Allows creating the game hologram | OP |
| `catsandmice.reload` | Allows reloading the plugin configuration | OP |
| `catsandmice.help` | Allows viewing the help menu | OP |

## 🎨 Customization

### Hologram
- Customizable title via `messages.hologram.title`
- Top and bottom description blocks with best-score placeholders
- Configurable number of displayed click lines (`settings.last-clicks`)
- Click and reset line formats with placeholders
- Mock (fake) player names automatically generated to fill empty slots
- Option to display the most recent entry at the bottom or top

### Message Placeholders
| Placeholder | Description |
|-------------|-------------|
| `%player%` | Player name |
| `%score%` | Score value |
| `%prefix%` | LuckPerms prefix |
| `%time%` | Time of the click (format from `settings.time-format`) |
| `%day%` | Date of the click (dd/MM/yyyy) |

## 📊 Settings Reference

```yaml
settings:
  hologram-name: 'catsandmice_hologram'  # Hologram internal name
  last-clicks: 10                         # Number of click lines to display
  click-delay: 300                        # Cooldown between clicks (seconds)
  time-format: 'HH:mm'                   # Time format (e.g. HH:mm:ss)
  enable-mock-names: true                 # Fill empty slots with fake names
  notify-new-best-score: true             # Broadcast new best score to all players
  recent-on-bottom: true                  # Show most recent click at the bottom
```

## 🔗 Discord Webhook Setup

1. Create a webhook in your Discord channel settings
2. Enable webhooks in the config: `webhook.enabled: true`
3. Paste your webhook URL: `webhook.url: 'https://discord.com/api/webhooks/...'`
4. Set the score threshold for alerts: `webhook.threshold: 100`
5. Optionally enable mentions: `webhook.mention: '@everyone'`

### Full Webhook Configuration Example

```yaml
webhook:
  enabled: true
  url: 'https://discord.com/api/webhooks/your_webhook_here'
  threshold: 100
  mention: '@everyone'
  console-message: true
  alert-message: |-
    **🎮 CATS AND MICE - 1-2-3-Modo**
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━
    ⏰***High score alert***
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━
    🏆 **Player:** %player%
    ⚡ **Score reached:** %score%
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━
  console-alert: '&6[CatsAndMice] &aScore alert: %player% has reached %score% points'
```

## 🗂️ Project Structure

```
src/main/java/fr/loot1/catsandmice/
├── CatsAndMice.java              # Main plugin class
├── Click.java                    # Click data model
├── commands/
│   └── CatsAndMiceCommand.java   # /mice command handler
├── listeners/
│   └── HologramClickListener.java # Hologram click event listener
├── managers/
│   ├── ConfigManager.java        # Config file manager
│   ├── DataFileManager.java      # Data file manager (async I/O)
│   ├── GameManager.java          # Game logic (scores, resets, webhook)
│   └── HologramManager.java      # Hologram creation and update logic
└── utils/
    ├── DiscordWebhook.java        # Discord webhook HTTP client
    └── RanksHelper.java           # LuckPerms prefix helper
```

## Dependencies

| Dependency | Type | Link |
|------------|------|------|
| DecentHolograms | Required | [SpigotMC](https://www.spigotmc.org/resources/96927/) |
| LuckPerms | Optional | [luckperms.net](https://luckperms.net/) |
| Spigot/Paper 1.20.6+ | Server | — |
| Java 17+ | Runtime | — |

## Authors

| Role | Author                                              |
|------|-----------------------------------------------------|
| Original idea & initial Discord bot | [**Seblor**](https://github.com/Seblor/Cats-n-Mice) |
| First Minecraft plugin version | [**Eniox59**](https://github.com/Eniox5)            |
| Plugin completion & final work | **Loot1**                                           |

> ⚠️ This plugin is a Minecraft adaptation of **[Cats-n-Mice](https://github.com/Seblor/Cats-n-Mice)**, originally created by **Seblor** as a Discord bot.
