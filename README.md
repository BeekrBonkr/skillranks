# SkillRanks

A Spigot plugin that grants a player a "rank" (a permission group, or any
console command(s) you configure) based on the numeric value of a
[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
placeholder — for example a stat, score, or a computed rating pulled from
another plugin.

## Features

- **Rank trees** — define any number of independent rank progressions
  (e.g. one for combat skill, one for economy rank). A player holds at
  most one rank per tree.
- **Placeholder-driven** — eligibility for each rank is a numeric range
  (`>N`, `>=N`, `<N`, `<=N`, `N`, or `N-M`) evaluated against any
  PlaceholderAPI placeholder, so it works with whatever stat/skill
  plugin you already have.
- **Manual or automatic** — players can re-check their own rank with a
  command, or the plugin can do it for them automatically on join and/or
  on a repeating timer.
- **Config migration** — updating the plugin won't silently leave your
  `config.yml` missing new options; see [Config versioning](#config-versioning).

## Requirements

- Spigot/Paper 1.13+ (tested against 1.20.4)
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
- Whatever plugin provides the stat(s) you want to rank players on
  (referenced via a PlaceholderAPI placeholder)

## Installation

1. Drop the built jar (see [Building](#building)) into your server's
   `plugins/` folder along with PlaceholderAPI.
2. Start the server once to generate `plugins/SkillRanks/config.yml`.
3. Edit the config to define your rank tree(s) and placeholder (see below).
4. Run `/plreload` (or restart) after editing PlaceholderAPI expansions.

## Configuration

`config.yml` is fully commented inline — open it and read the comments
next to each key. At a high level:

- `placeholder-to-listen-for` — the PlaceholderAPI placeholder whose
  value (a whole number) determines a player's rank.
- `placeholder-cache-ms` — how long a resolved placeholder value is
  cached per player before being re-evaluated.
- `command` — the command players run to manually re-check their rank
  (e.g. `/updaterank <tree>`), permission `skillranks.updaterank`.
- `auto-check` — automatic, command-free rank checks:
  - `on-join` / `join-delay-ticks` — re-check shortly after a player joins.
  - `interval-seconds` — periodically re-check every online player (0 disables).
- `ranks` — one or more independent rank trees, each with a `permission`
  gate and an ordered list of `skills` (highest rank first), each
  defining a `range`, a `message`, and `add-command`/`remove-command`
  console commands run on promotion/demotion.
- `lang` — all player-facing messages.
- `debug` — logs one line per rank check (tree, level, outcome); useful
  while tuning your `range` values and placeholder math.

### Config versioning

`config.yml` carries an internal `config-version`. On startup, if your
on-disk config predates the version the plugin ships, SkillRanks:

1. Backs up your existing file to `config-v<old version>.yml.bak`.
2. Fills in any newly-introduced keys with their default values, without
   touching anything you've already customized (your `ranks`, messages,
   etc. are left as-is).
3. Saves the merged result back to `config.yml` and logs what happened.

Note: because of a limitation in Bukkit's config API, the automatic
merge/save step does not preserve YAML comments — if you want the
newly-documented explanations for existing keys, refer to the freshly
shipped `config.yml` in the plugin jar, or diff against your `.bak`.
Comments are preserved as long as your file is untouched by a migration
(i.e. on a fresh install).

## Building

Requires JDK 16+ and Maven, with network access to Spigot's repository,
JitPack, `repo.olziedev.com`, and `repo.extendedclip.com` (see `pom.xml`).

```
mvn clean package
```

The shaded jar is written to `target/SkillRanks <version>.jar`.
