# SkillRanks

<div align="center">

<a href="https://ko-fi.com/bkrbnkr"><img alt="Support me on Ko-fi" src="https://img.shields.io/badge/Ko--fi-buy_me_a_coffee-FF5E5B?style=for-the-badge&logo=kofi&logoColor=white"></a>

</div>

A Spigot/Paper 1.20+ plugin that grants a player a **rank**, a permission group, or any console
command(s) you configure, based on the numeric value of a
[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) placeholder: a stat, a
score, or a rating computed from several stats with the `math` expansion. Point a rank tree at a
placeholder, list the ranks with the value range each one needs, and SkillRanks promotes and
demotes players as the number moves, on join, on a timer, or when they ask.

---

## Features

- **Rank trees**: define any number of independent rank progressions (one for combat skill, one
  for economy, …), each under its own key in `ranks`. A player holds at most **one** rank per tree,
  and trees never see each other's values.
- **Placeholder-driven, per tree**: every tree tracks its own PlaceholderAPI placeholder
  (`placeholder`), so different trees can rank players on different stats. Eligibility for each
  rank is a numeric range (`>N`, `>=N`, `<N`, `<=N`, `N` or `N-M`; negative bounds work) checked
  against that tree's value.
- **Commands, not hard-coded groups**: a rank is whatever its `add-command` list does, so it
  works with LuckPerms, any other permission plugin, or nothing permission-related at all.
  `remove-command` undoes it when the player moves to a different rank in the same tree.
- **Manual or automatic**: players re-check their own rank with `/updaterank <tree>`, or the
  plugin does it for them shortly after they join (`auto-check.on-join`) and on a repeating timer
  (`auto-check.interval-seconds`). Automatic checks respect each tree's `permission` gate.
- **Force resync**: `/updaterank <tree> force` re-runs the matched rank's `add-command`s even
  when the rank looks unchanged, for when the permission plugin's state has drifted (a group
  removed by hand, a wiped database).
- **Placeholder cache**: a resolved value is cached per player and per placeholder for
  `placeholder-cache-ms`, so an expensive `math` expression is not re-evaluated on every check.
  Failures are cached too, so a bad value is warned about once per window rather than every tick.
- **Config migration**: updating the plugin never silently leaves your `config.yml` missing new
  options; see [Config versioning](#config-versioning).

---

## Requirements

| Dependency | Version | Required |
|---|---|---|
| [Spigot](https://www.spigotmc.org/) / [Paper](https://papermc.io/) | 1.20.1+ (built against the 1.20.4 API; `api-version: '1.20'`) | ✅ |
| Java | 16+ | ✅ |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | 2.11+, a hard dependency, the plugin will not enable without it | ✅ |
| A stat source | Whatever plugin provides the placeholder(s) you rank on (MBedwars, a stats plugin, the `player` expansion, …), plus the PlaceholderAPI `math` expansion if you combine several | ✅ |
| A permission plugin | Only if your `add-command`s set groups, LuckPerms in the shipped example | Optional |

---

## Installation

1. Drop `SkillRanks <version>.jar` (see [Building from Source](#building-from-source)) into your
   server's `plugins/` folder next to PlaceholderAPI.
2. Download the expansions your placeholders need, for the shipped example that is
   `/papi ecloud download math` (and `/papi ecloud download player` for the second example tree),
   then `/papi reload`.
3. Start the server once to generate `plugins/SkillRanks/config.yml`.
4. Edit the config to define your rank tree(s), placeholder(s) and ranges, see
   [Configuration](#configuration).
5. Restart the server. SkillRanks has no reload command of its own: rank trees are parsed once on
   enable, so config changes need a restart.

---

## Commands

The command name is itself configurable (`command` in `config.yml`, default `updaterank`); it is
registered at runtime rather than in `plugin.yml`, so renaming it needs nothing else.

| Command | Permission | Description |
|---|---|---|
| `/updaterank` | `skillranks.updaterank` | Show the help lines (`lang.help`) |
| `/updaterank <tree>` | `skillranks.updaterank` | Re-check your rank in that tree and promote/demote you if the value moved. Tab-completes the trees you may use |
| `/updaterank <tree> force` | `skillranks.updaterank.force` | Re-run the add-commands of the rank you qualify for even if you already hold it |

The command is **player-only**, console gets `lang.player-only`. There is no admin command to
check someone else; the automatic checks cover that.

---

## Permissions

Neither node is declared in `plugin.yml`, so operators hold both implicitly and everyone else needs
them granted explicitly.

| Permission | Default | Description |
|---|---|---|
| `skillranks.updaterank` | op (undeclared) | Use `/updaterank` |
| `skillranks.updaterank.force` | op (undeclared) | Use the `force` variant |
| *(per tree)* `ranks.<tree>.permission` | none | Whatever node a tree names is required to be eligible for it at all, for the command **and** the automatic checks. Set it to `""` to open the tree to everyone |

---

## Placeholders

SkillRanks does **not** register a PlaceholderAPI expansion, it only *reads* placeholders. What it
does substitute are two tokens of its own inside each rank's `message`, `add-command` and
`remove-command`:

| Token | Replaced with |
|---|---|
| `%player%` | The player's name |
| `%name%` | The rank's `name` (with `&` color codes) |
| `%cmd%` | In `lang.help` only, the command label as typed |

The rank a player currently holds is not exposed as a placeholder either; it is recorded as a flag
in the player's persistent data container (`skillranks:rank-<tree>-<skill>`), which is what lets
the plugin know which `remove-command` to run when they move on.

---

## Configuration

`config.yml` is fully commented inline, the file itself is the reference. The keys, with their
shipped defaults:

| Key | Default | What it does |
|---|---|---|
| `config-version` | `3` | Internal, leave it alone; drives [migration](#config-versioning) |
| `debug` | `false` | Log one line per rank check (tree, resolved value, outcome). Handy while tuning ranges, noisy otherwise |
| `placeholder-cache-ms` | `2000` | How long a resolved value is cached per player and placeholder; `0` re-resolves on every check |
| `command` | `updaterank` | The manual re-check command's name |
| `auto-check.on-join` | `true` | Re-check every tree the player may use shortly after they join |
| `auto-check.join-delay-ticks` | `40` | The wait before that join check, so stat plugins have loaded the player's data first |
| `auto-check.interval-seconds` | `300` | Re-check every online player on a timer; `0` disables it |
| `ranks.<tree>.permission` | `perm.node` | Gate for the tree (`""` = everyone) |
| `ranks.<tree>.placeholder` | *(see below)* | The placeholder the tree ranks on, must resolve to a whole number |
| `ranks.<tree>.skills.<id>.name` | none | Display name, used as `%name%` |
| `ranks.<tree>.skills.<id>.message` | none | Sent to the player on promotion into this rank |
| `ranks.<tree>.skills.<id>.range` | none | The value range this rank needs |
| `ranks.<tree>.skills.<id>.add-command` | `[]` | Console commands run when the player is promoted **to** this rank |
| `ranks.<tree>.skills.<id>.remove-command` | `[]` | Console commands run when the player moves **away** from this rank |
| `lang.*` | none | Every player-facing message; `""` silences one |

### A worked example

A single tree, `rank`, driven by a bedwars rating built from MBedwars stats with the `math`
expansion (`_0_` rounds it to a whole number, SkillRanks refuses decimals):

```yaml
ranks:
  rank:
    permission: "perm.node"          # "" to open the tree to everyone
    placeholder: "%math_0_{mbedwars_stats-wl}*100+{mbedwars_stats-final_kills}*20+{mbedwars_stats-kd}*50+{mbedwars_stats-wins}*10+{mbedwars_stats-beds_destroyed}*80%"
    skills:
      1:
        name: "&f&lInfinite"
        message: "Your rank has been updated to %name%"
        range: ">3000"
        add-command:
          - "lp user %player% permission set group.infinite"
        remove-command:
          - "lp user %player% permission unset group.infinite"
      2:
        name: "&f&lOxidized"
        message: "Your rank has been updated to %name%"
        range: "100-3000"
        add-command:
          - "lp user %player% permission set group.oxidized"
        remove-command:
          - "lp user %player% permission unset group.oxidized"
      3:
        name: "&f&lUnranked"
        message: "Your rank has been updated to %name%"
        range: "<100"
        add-command:
          - "lp user %player% permission set group.unranked"
        remove-command:
          - "lp user %player% permission unset group.unranked"
```

How a check resolves, in the order the code does it:

1. The tree's `permission` is checked (automatic checks silently skip trees the player may not
   use; the command answers `lang.invalid-rank`).
2. The placeholder is resolved (or read from the cache) and parsed as a whole number. Anything
   else, a decimal, an unresolved `%…%`, an empty string, is logged as a warning and the check is
   skipped, leaving the player's rank untouched.
3. The skills are walked **top to bottom in the order they appear in the file**, and the **first
   range that contains the value wins**, so list them highest first, as above.
4. If the player already holds the winning rank, nothing happens (`lang.nothing-changed` on the
   command, silence on automatic checks), unless `force` was given, in which case its
   `add-command`s run again.
5. Otherwise the rank they held in this tree (if any) has its `remove-command`s run, then the new
   rank's `add-command`s run, the `message` is sent, and the new rank is recorded on the player.

So a player whose rating climbs from 80 to 250 loses `group.unranked` and gains `group.oxidized`
in one check; falling back under 100 later reverses it. Ranks are per tree: a second tree such as
`example-rank-tree` (driven by `%player_level%` in the shipped file) is evaluated on its own and
never touches the first tree's group.

**Keep each tree's ranges contiguous.** A value that matches no range is `lang.no-rank` for the
player and *no change* for their rank, they are never demoted for falling into a gap, so a gap
means someone can sit on a stale rank until their value drifts back into a defined range. An
invalid or reversed range (`"50-10"`) is logged on startup and never matches.

### Config versioning

`config.yml` carries an internal `config-version`. On startup, if your on-disk file predates the
version the plugin ships, SkillRanks:

1. Backs up your existing file to `config-v<old version>.yml.bak`.
2. Applies any structural migrations. Upgrading to version 2 renames `lang.no-ranking` to
   `lang.no-rank`; upgrading to version 3 moves the old global `placeholder-to-listen-for` into a
   `placeholder` key inside each of your rank trees (same behavior as before, but each tree can
   now be pointed at a different placeholder afterwards).
3. Fills in any newly introduced keys with their default values without touching anything you
   customized. The `ranks` section is deliberately excluded from that top-up, so the shipped
   example trees are never merged into a real config.
4. Saves the merged result back to `config.yml` and logs what happened.

Because of a limitation in Bukkit's config API, that save does **not** preserve YAML comments,
for the explanations of existing keys, refer to the `.bak`, or to the fresh `config.yml` inside the
jar. Comments survive as long as your file is never migrated (a fresh install on the current
version).

---

## Building from Source

Requires JDK 16+ and Maven, with network access to the repositories listed in `pom.xml` (SpigotMC
snapshots, Sonatype OSS, JitPack, `repo.olziedev.com` and `repo.extendedclip.com`).

```bash
mvn clean package
```

Output: `target/SkillRanks <version>.jar`, the shaded jar (the command framework is bundled;
Spigot and PlaceholderAPI are `provided`). Note the space in the file name.

---

## Known limitations

- **No reload command.** Rank trees are parsed once on enable, and the command name is read when
  the command registers, so every config change needs a restart.
- **Whole numbers only.** A placeholder that resolves to a decimal (or to text) skips the check
  with a console warning. Round it in the placeholder (`%math_0_{…}%`).
- **Held ranks live in player data, not in the permission plugin.** SkillRanks remembers which
  rank it gave you as a flag on your player file, so it can only ever undo what it did itself on
  this server, if a group is removed by hand, use `/updaterank <tree> force`, and nothing is shared
  across a network.
- **Gaps never demote.** See [A worked example](#a-worked-example).
- **Migration drops comments.** See [Config versioning](#config-versioning).

---

## Support

<div align="center">

This plugin is free and open source, and I work on it in my spare time.<br>
If it saved you some time, you can buy me a coffee. No pressure - the code stays free either way.

<a href="https://ko-fi.com/bkrbnkr"><img alt="Support me on Ko-fi" src="https://img.shields.io/badge/Ko--fi-bkrbnkr-FF5E5B?style=for-the-badge&logo=kofi&logoColor=white"></a>

</div>

<!-- more ways to support go here -->
<!-- - [PayPal](...) -->
<!-- - [GitHub Sponsors](...) -->

---

## License

GPL-3.0, see [LICENSE](LICENSE).
