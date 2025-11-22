# Prop Hunt - NeoForge 1.21.1

**The classic Prop Hunt gamemode for Minecraft!** Hide as blocks, hunt your friends, and enjoy chaotic fun on your server.

---

## Description

Prop Hunt is a hide-and-seek minigame where players are divided into two teams:

- **Props** - Transform into blocks and hide! Use your freeze ability to blend in perfectly with the environment.
- **Hunters** - Find and eliminate all the props before time runs out!

### Features

- **Block Transformation** - Props can transform into any block, including custom player heads with NBT data
- **Freeze System** - Props can freeze in place to look like real blocks (with configurable duration and cooldown)
- **Custom Maps** - Create unlimited maps with separate spawn points for hunters and props
- **Hunter Loadout** - Configure custom weapons and ammunition for hunters
- **HUD Display** - Real-time game information, freeze bar, player list with teams
- **Sound System** - Props periodically whistle to help hunters, plus victory/defeat sounds
- **Special Items**:
  - **Prop Locator** (Hunters) - Every minute, receive an item that plays a sound on a random prop
  - **Decoy** (Props) - Every minute, receive an item that plays a decoy sound on a random prop to confuse hunters
- **Fully Configurable** - Adjust game time, freeze duration, hunter ratio, and more

---

## Quick Start Guide

### Step 1: Create a Map

1. Go to the center of your arena
2. Run: `/prophunt create <mapname>`

### Step 2: Set Spawn Points

1. Go to where **Hunters** should spawn
2. Run: `/prophunt sethunterspawn <mapname>`
3. Go to where **Props** should spawn
4. Run: `/prophunt setpropsspawn <mapname>`

### Step 3: Add Prop Blocks

Props will randomly transform into blocks from this list:

1. Hold a block in your hand (works with custom player heads!)
2. Run: `/prophunt addblock <mapname> hand`
3. Repeat for all blocks you want props to become

### Step 4: Configure Hunter Loadout (Optional)

1. Hold the weapon you want hunters to have
2. Run: `/prophunt hunteraddweapon hand`
3. Hold ammunition items, ammo where given to player each prop kill he made
4. Run: `/prophunt addammo hand`

### Step 5: Start the Game!

1. Run: `/prophunt start <mapname>`
2. Players have 60 seconds to join with: `/prophunt join`
3. Game begins automatically when the lobby timer ends (minimum 2 players)

---

## Commands Reference

### Game Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/prophunt start <map>` | OP | Start a game on the specified map |
| `/prophunt stop` | OP | Stop the current game |
| `/prophunt join` | All | Join the current game lobby |
| `/prophunt leave` | All | Leave the current game |
| `/prophunt status` | All | Show current game status |
| `/prophunt testmode` | OP | Start solo test mode as a Prop |

### Map Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/prophunt create <name>` | OP | Create a new map at your location |
| `/prophunt maps` | All | List all available maps |
| `/prophunt mapinfo <name>` | All | Show detailed map information |
| `/prophunt sethunterspawn <map>` | OP | Set hunter spawn at your position |
| `/prophunt setpropsspawn <map>` | OP | Set props spawn at your position |
| `/prophunt addblock <map> hand` | All | Add held block to map's prop list |
| `/prophunt showblock <map>` | All | Show all blocks configured for a map |
| `/prophunt clearblock <map>` | OP | Remove all blocks from a map |

### Hunter Loadout Commands
| Command | Permission | Description |
|---------|------------|-------------|
| `/prophunt hunteraddweapon hand` | OP | Add held item as hunter weapon |
| `/prophunt addammo hand` | OP | Add held item as ammo (given on kills) |
| `/prophunt hunterloadout` | All | Show current hunter loadout |
| `/prophunt clearloadout` | OP | Clear all hunter loadout items |

### Prop Commands (In-Game)
| Command | Permission | Description |
|---------|------------|-------------|
| `/prophunt transform` | All | Transform into a nearby block |
| `/prophunt transform hand` | All | Transform into held block (debug) |
| `/prophunt transform <block>` | All | Transform into specific block |
| `/prophunt revert` | All | Revert transformation |

---

## Configuration

Edit `config/prophunt-common.toml` to customize:

```toml
[freeze]
# Maximum freeze time in seconds (5-120)
maxFreezeTime = 30

# Cooldown after freeze is exhausted (5-120)
freezeCooldown = 30

# Freeze regeneration speed (0 = no regen, 2 = 2x faster)
freezeRegenRate = 2

[sound]
# Interval between prop whistles in seconds (10-120)
propSoundInterval = 30

[game]
# Hide phase duration in seconds (10-120)
hidePhaseTime = 30

# Total game time in seconds (60-1800)
gameTime = 300

# Minimum players to start (1-10)
minPlayers = 2

# Ratio of hunters (0.1-0.9, e.g., 0.33 = 1/3 hunters)
hunterRatio = 0.33
```

---

## Game Phases

1. **LOBBY** (60 seconds) - Players join with `/prophunt join`
2. **HIDING** (30 seconds) - Props hide, hunters are blinded and frozen
3. **PLAYING** (5 minutes) - Hunters search for props!
4. **ENDING** - Winner announced with victory sound

### Win Conditions

- **Props Win**: Survive until time runs out
- **Hunters Win**: Eliminate all props before time ends

---

## Prop Abilities

### Freeze (Sneak)
- Hold **Shift** to freeze in place
- While frozen, you look exactly like a real block
- Freeze bar depletes while sneaking
- When depleted, enters cooldown before you can freeze again
- Regenerates when not sneaking

### Speed Boost
- Props have permanent **Speed II** during the game
- Use it to escape hunters!

### Decoy Item
- Every minute, receive a **Decoy** item
- Put it in your **offhand** to activate
- Plays a fart sound on a random prop (including yourself)
- Use strategically to confuse hunters!

---

## Hunter Abilities

### Custom Loadout
- Receive configured weapons at game start
- Get bonus ammo for each prop eliminated

### Prop Locator Item
- Every minute, receive a **Prop Locator** compass
- Put it in your **offhand** to activate
- Plays a loud random sound at a random prop's location
- Listen carefully to find hidden props!

---

## Tips

### For Props
- Choose blocks that blend with the environment
- Don't move when hunters are nearby - freeze!
- Use decoys to distract hunters from your real position
- The whistle reveals your location - be ready to run after!

### For Hunters
- Listen for the periodic whistles
- Use your Prop Locator strategically
- Check blocks that seem out of place
- Work together to cover more ground

---

## Requirements

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.x
- **Side**: Server-side (clients need the mod for HUD)

---

## Support

Found a bug? Have a suggestion?
Report issues on our GitHub repository!

---

*Have fun hunting (or hiding)!*
