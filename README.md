# PotionPacker

**PotionPacker** is a lightweight utility plugin for Minecraft servers that allows **potions to stack** using the  
**Data Component API (`MAX_STACK_SIZE`)** — without NMS or reflection.

---

## Features

- Stack **normal**, **splash**, and **lingering** potions
- Per-type stack size configuration
- Optional **profile-based system** with permissions and priorities
- Works with:
  - player inventories
  - cursor interactions
  - shift-click & double-click
  - hoppers and container transfers
  - dropped items in the world
- Live configuration reload support
- Folia-safe scheduling

---

### Profile system
PotionPacker supports an optional **profile-based mode**:
- Profiles are enabled via a config toggle
- Each profile is bound to a permission:
  ```
  potionpacker.profile.<profile_name>
  ```
- If a player has multiple profile permissions, the profile with the **highest priority** is used
- Profiles inherit missing values from the `default` profile

Changes can be applied **without restart** using `/pp reload`.

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
| `potionpacker.profile.<name>` | Assigns a stack profile to a player |

---

## Compatibility

- ✅ Paper **1.21.3+** and forks (Purpur, Pufferfish, etc.)
- ✅ Folia
- ❌ Spigot (no Data Component API)
- ❌ Minecraft 1.20.x and older

---

## Building from source

PotionPacker uses **Gradle**.

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

PotionPacker is licensed under the **MIT License**.  
You are free to use, modify, and redistribute it.
