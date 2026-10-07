# PvPCore

Combat tag, combat timer display (boss bar / action bar / chat / hidden), no-elytra in combat, and optional PvP restrictions for **Paper 1.21.11** (Java 21).

**Everything is disabled by default.** Install it, then turn features on in `plugins/PvPCore/config.yml` and run `/pvpcore reload`.

---

## File tree

```
PvPCore/
├── .github/workflows/build.yml      # optional: build the jar on GitHub, no installs needed
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew / gradlew.bat            # Gradle wrapper (downloads Gradle for you)
├── gradle/wrapper/...
└── src/main/
    ├── java/com/pvpcore/
    │   ├── PvPCore.java             # main class: startup, reload, scheduler task
    │   ├── combat/CombatManager.java
    │   ├── commands/PvPCoreCommand.java
    │   ├── config/DisplayMode.java
    │   ├── config/Messages.java
    │   ├── config/PluginSettings.java
    │   ├── display/CombatDisplay.java
    │   └── listeners/
    │       ├── CombatListener.java      # tagging, death, combat logging
    │       ├── ElytraListener.java      # no gliding / no firework boost
    │       ├── NoticeCooldown.java      # stops "you can't do that" spam
    │       └── RestrictionListener.java # blocked commands, ender pearls
    └── resources/
        ├── config.yml
        └── plugin.yml
```

---

## Building the jar

You need the jar file `PvPCore-1.0.0.jar`. Pick **one** of these two ways.

### Option A: build in the cloud with GitHub (nothing to install)

1. Create a free account at github.com and make a new **private** repository.
2. On the repository page click **Add file → Upload files** and drag in everything from this folder (including the hidden `.github` folder — on Mac press `Cmd+Shift+.` in Finder to see it; on Windows enable *View → Hidden items*).
3. Click **Commit changes**. GitHub starts building automatically.
4. Open the **Actions** tab, click the latest run, wait for the green tick (about 2 minutes).
5. Scroll to **Artifacts** and download **PvPCore-jar**. Unzip it — inside is `PvPCore-1.0.0.jar`.

### Option B: build on your own computer

**1. Install Java 21 (JDK)**

- Go to https://adoptium.net/temurin/releases/?version=21
- Choose your operating system, package type **JDK**, and download the installer (`.msi` for Windows, `.pkg` for Mac).
- Run it. On Windows, tick **"Set JAVA_HOME variable"** during install.
- Check it worked: open **Command Prompt** (Windows) or **Terminal** (Mac) and run:
  ```
  java -version
  ```
  It should say `21`.

**2. Build**

Open Command Prompt / Terminal **inside the PvPCore folder**:
- Windows: open the folder in File Explorer, click the address bar, type `cmd`, press Enter.
- Mac: right-click the folder → **New Terminal at Folder** (or `cd` into it).

Then run:

| Windows | Mac |
|---|---|
| `gradlew.bat build` | `chmod +x gradlew` then `./gradlew build` |

The first build downloads Gradle and the Paper API, so it can take a few minutes. When you see **BUILD SUCCESSFUL**, the jar is at:

```
build/libs/PvPCore-1.0.0.jar
```

---

## Installing on your hosted server (e.g. MintServers)

1. Log in to your server panel and open the **File Manager**.
2. Open the `plugins` folder and **upload** `PvPCore-1.0.0.jar`.
3. **Restart** the server (a full restart, not `/reload`).
4. A new folder `plugins/PvPCore/` appears with `config.yml`. The console will say *"Combat tag is disabled in config.yml - PvPCore is idle."* — that is expected.
5. Edit `config.yml` in the File Manager, set `combat-tag.enabled: true` plus any other features you want, save, then run `/pvpcore reload` in game or in the console.

---

## Commands & permissions

| Command | Permission | Default |
|---|---|---|
| `/pvpcore reload` | `pvpcore.admin` | op |
| `/pvpcore status [player]` | `pvpcore.status` | everyone |
| `/pvpcore untag <player>` | `pvpcore.admin` | op |
| *(never gets tagged)* | `pvpcore.bypass` | nobody, not even ops |

Alias: `/pvpc`. Tab completion is included.

---

## How the features fit together

- **combat-tag** is the master switch. All other features only affect tagged players, so they need it on.
- Creative and spectator players are never tagged.
- Hits cancelled by other plugins (spawn protection, WorldGuard no-PvP regions, etc.) never tag anyone.
- Hurting yourself (your own arrow, pearl, or TNT) never tags you.
- Citizens NPCs are treated as mobs, not players.
- `no-elytra.block-firework-boost` works on its own, even if `no-elytra.enabled` is false.
- In BOSSBAR and ACTIONBAR modes the chat tag/untag messages are also sent; set a message to `""` to turn it off.
- Combat logging ignores kicks by default (`punish-on-kick: false`), so server restarts and staff kicks never kill anyone.

---

## Test checklist

Use two accounts (or a friend). Make sure neither has `pvpcore.bypass` and both are in survival.

1. **Idle by default** — fresh install: hit each other. Nothing happens; console says PvPCore is idle.
2. **Combat tag** — set `combat-tag.enabled: true`, reload. Hit the other player: both get *"You are now in combat"*. Wait 15 s: both get *"no longer in combat"*.
3. **Timer reset** — hit again at 10 s left: timer goes back to 15 (`reset-timer-on-new-hit: true`). Set it to `false`, reload, repeat: timer keeps counting down.
4. **Projectiles** — shoot the other player with a bow: both tagged.
5. **Display modes** — try each `display-mode` with a reload in between:
   - `BOSSBAR`: red bar at the top draining with the seconds; disappears when combat ends, on death, and on logout.
   - `ACTIONBAR`: countdown above the hotbar; clears when combat ends.
   - `CHAT`: only the two chat messages.
   - `HIDDEN`: nothing at all.
6. **Disabled worlds** — add your world name to `disabled-worlds`, reload, hit: no tag.
7. **Mob damage** — `tag-on-mob-damage: true`, get hit by a zombie: you are tagged.
8. **No elytra** — `no-elytra.enabled: true`. Get tagged, jump off something high and try to glide: blocked with a message. Start gliding first, then get hit by an arrow: you drop out of the glide.
9. **Firework boost** — `block-firework-boost: true`, tagged, use a rocket while gliding: blocked, rocket not used.
10. **Combat log** — `combat-log.enabled: true`. Get tagged and disconnect: you die, items drop, server sees the broadcast. Get kicked (`/kick`) while tagged: you do **not** die.
11. **Blocked commands** — `blocked-commands.enabled: true`, tagged, run `/spawn` and `/essentials:spawn`: both blocked.
12. **Ender pearls** — `block-ender-pearls.enabled: true`, tagged, throw a pearl: blocked, pearl stays in your hand.
13. **Untag on death** — `untag-on-death.enabled: true`, die while tagged: you are no longer tagged after respawn. With `false`, the timer continues after respawn.
14. **Commands** — `/pvpcore status <name>` shows time left; `/pvpcore untag <name>` clears it; `/pvpcore reload` applies config edits instantly.
15. **Bypass** — give yourself `pvpcore.bypass` (e.g. with LuckPerms): you are never tagged.

---

## Notes on the Paper API

- Built against `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`.
- Uses Paper-specific events `PlayerElytraBoostEvent` and `PlayerLaunchProjectileEvent`, so it needs **Paper** (or a Paper fork like Purpur), not Spigot.
- Uses `plugin.yml` (classic Bukkit plugin) rather than `paper-plugin.yml`, so commands work through the normal command map.
