<div align="center">

# 🐱 CatsAndMice — Red light, green light with moderators

*A competitive click game for your Minecraft server*

![CatsAndMice preview](https://raw.githubusercontent.com/Loot1/CatsAndMice/master/.github/assets/image.png)

</div>

---

## 📖 What is CatsAndMice?

CatsAndMice is a Minecraft adaptation of the original [Cats-n-Mice Discord bot](https://github.com/Seblor/Cats-n-Mice) by **Seblor**.

Players click a hologram to increase a shared score. Moderators (cats 🐱) can reset the score at any time — the goal for players (mice 🐭) is to reach the highest score possible before it gets wiped! The current score and last clicks are displayed in real-time on a clickable hologram.

---

## ✨ Features

- **Clickable hologram** — real-time leaderboard powered by DecentHolograms
- **Role-based gameplay** — mice click to increase the score, cats reset it
- **Click cooldown** — configurable delay between each click (default: 5 minutes)
- **Best score tracking** — records the best score with the player name, date and time
- **Mock (fake) player names** — automatically fills empty hologram slots to keep the display full
- **Configurable display order** — most recent click at the bottom or at the top
- **LuckPerms support** — displays rank prefixes next to player names in the hologram
- **Discord webhook alerts** — sends a notification when a score threshold is reached
- **Fully customizable** — every message, color and setting is configurable in `config.yml`
- **Async data saving** — click history and hologram location are saved automatically

---

## 🚀 Installation

1. Make sure [DecentHolograms](https://www.spigotmc.org/resources/96927/) is installed on your server
2. Download the **CatsAndMice** JAR and place it in your `plugins/` folder
3. Start or restart your server
4. Stand where you want the hologram and run `/mice create`
5. Edit `plugins/CatsAndMice/config.yml` to customize messages and settings
6. Run `/mice reload` to apply your changes

---

## ⚙️ Commands & Permissions

**Commands:**

| Command | Permission | Description |
|---------|------------|-------------|
| `/mice create` | `catsandmice.create` | Creates the game hologram at your location |
| `/mice reload` | `catsandmice.reload` | Reloads the configuration |
| `/mice help` | `catsandmice.help` | Displays the help menu |

**Permissions:**

| Permission | Description |
|------------|-------------|
| `catsandmice.mice` | Allows clicking the hologram to increase the score |
| `catsandmice.cat` | Allows clicking the hologram to reset the score |
| `catsandmice.bypass` | Bypasses the click cooldown delay |
| `catsandmice.*` | Grants all plugin permissions (OP by default) |

---

## 🔗 Discord Webhook (optional)

CatsAndMice can send an alert to a Discord channel when the score reaches a configurable threshold.

1. Create a webhook in your Discord channel settings
2. Set `webhook.enabled: true` in `config.yml`
3. Paste your webhook URL in `webhook.url`
4. Set your score threshold with `webhook.threshold`

---

## 📦 Dependencies

| Dependency | Type | Link |
|------------|------|------|
| DecentHolograms | **Required** | [SpigotMC](https://www.spigotmc.org/resources/96927/) |
| LuckPerms | Optional | [luckperms.net](https://luckperms.net/) |

**Server:** Spigot / Paper 1.20.6 or higher — **Java 17+**

---

## 👥 Credits

| Role | Author |
|------|--------|
| Original idea & Discord bot | [Seblor](https://github.com/Seblor/Cats-n-Mice) |
| First Minecraft plugin version | [Eniox59](https://github.com/Eniox5) |
| Plugin completion & final work | Loot1 |

