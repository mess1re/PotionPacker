# PotionPacker

**PotionPacker** is a lightweight utility plugin for Minecraft servers, which allows **potions to stack** using the 
**Data Component API (`MAX_STACK_SIZE`)** — without NMS or reflection.

---

## Features

- Stack **normal**, **splash**, and **lingering** potions
- Per-type stack size configuration
- Works with:
  - player inventories
  - cursor interactions
  - shift-click & double-click
  - hoppers and container transfers
  - dropped items in the world

- Live config reload support

---

## Configuration

```yaml
# PotionPacker configuration
# Valid range for each: 1..64

stack_sizes:
  # Regular drinkable potions
  potion: 16
  # Splash potions
  splash_potion: 16
  # Lingering potions
  lingering_potion: 16

messages:
  # MiniMessage is supported + "#RRGGBB" hex (auto-converted to "<#RRGGBB>")
  prefix: "#FFD700[PotionPacker] "

  no-permission: "%prefix%#FF5555You don't have permission."
  reloaded: "%prefix%#55FF55Configuration reloaded."

  help:
    - ""
    - "%prefix%#FFFFFFCommands:"
    - "#AAAAAA/pp reload #777777- reload the config"
    - ""

```

Setting a value to `1` restores **pure vanilla behavior**.

---

## Commands

| Command | Description |
|-------|------------|
| `/potionpacker reload` | Reload the configuration |

Alias: `/pp reload`

---

## Permissions

| Permission | Description |
|-----------|------------|
| `potionpacker.reload` | Allows reloading the config |

---

## Compatibility

-
- ✅ Paper **1.21.3+** and forks (Purpur, Pufferfish, etc.)
- ✅ Folia
- ❌ Spigot (no DataComponent API)
- ❌ Minecraft 1.20.x and older

---

## Building from source

PotionStacker uses **Gradle**.

### Requirements

- Java 21 JDK or newer
- Git

### Build

```bash
git clone https://github.com/mess1re/PotionPacker.git
cd PotionPacker
./gradlew build
```

The compiled JAR will be located in:

```
build/libs/
```

---

## License

PotionStacker is licensed under the **MIT License**.  
You are free to use, modify, and redistribute it.
