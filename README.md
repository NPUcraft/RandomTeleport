# RandomTeleport

Random safe RTP for **Paper / Spigot** with per-dimension ranges, world border clamp, optional delay, `/rtp back`, bilingual chat (EN/ZH), and economy via **Vault** or **CoinsEngine**.

## Features

- `**/rtp`** — Finds a random safe location in the player’s **current world** (surface scan for overworld/end; Y-random solid footing in the nether). Optional checks for water, webs, cave vines, dripstone, and minimum distance to other players. **`max-sync-chunk-loads-per-rtp`** caps how many unloaded chunks may be loaded synchronously per search (reduces lag; `0` = uncapped). While searching, an **action bar** can show status; if `**teleport-delay-ticks`** is greater than 0, the bar shows a **countdown** and **cost preview** (see `messages.*.delay-actionbar` / `delay-cost-*`).
- `**/rtp back`** — Returns to the **single** location stored before the **last successful** RTP (not a history stack). Each new successful RTP overwrites it; a successful back clears it. Separate **back** prices per dimension in config.
- `**/rtp help`** — Formatted help: `**help-header**`, multi-line `**help-line-range**` / `**help-line-no-range**` per world, then `**help-footer**`. Requires `randomteleport.use`. If no world is allowed, sends `**help-no-worlds**` (blank or omitted → built-in EN/ZH fallback for that language file).
- `**/rtp reload**` — Reloads config (`randomteleport.admin.reload`). On YAML/parse failure, the plugin keeps the previous settings, logs a stack trace, and sends `**reload-failed**`.
- **Economy** — `economy.provider`: `auto` | `vault` | `coinsengine`. With `auto`, Vault is used when hooked, otherwise CoinsEngine. `**economy.enabled: false`** disables charges. Success chat can include `**{paid}`** via `**success-paid**` / `**back-success-paid**`. Charges on successful teleport; refunds if teleport fails after withdrawal. **CoinsEngine**: if the balance after withdraw does not match the expected amount, the plugin restores the pre-withdraw balance and treats the payment as failed (`INSUFFICIENT_FUNDS` path).
- **PlaceholderAPI** (optional) — Expansion `randomteleport` (see below).

## Requirements

- Server: **Paper** or **Spigot** with `api-version` 1.20 or newer.
- **Java 17+**.
- Optional: **Vault** + an economy plugin; **CoinsEngine** (for `coinsengine` / `auto` fallback); **PlaceholderAPI**.

## Build

```bash
mvn clean package
```

Output: `target/RandomTeleport-1.0.0.jar` (version from `pom.xml`).

## Install

1. Copy the jar into your server `plugins` folder.
2. Start the server (or use PlugMan-style reload after first install is **not** recommended).
3. Edit `plugins/RandomTeleport/config.yml` as needed.
4. Run `**/rtp reload`** or restart to apply config changes.

## Commands


| Command                 | Description                                                                 |
| ----------------------- | --------------------------------------------------------------------------- |
| `/rtp`                  | Random teleport in the current world (alias: `randomtp`, `randomteleport`). |
| `/rtp back`             | Teleport back after a successful RTP (economy optional).                    |
| `/rtp help`             | Show per-world range and costs.                                             |
| `/rtp reload`           | Reload configuration.                                                       |


## Permissions


| Permission                       | Default | Description                          |
| -------------------------------- | ------- | ------------------------------------ |
| `randomteleport.use`             | `true`  | Use `/rtp` and `/rtp help`.          |
| `randomteleport.back`            | `true`  | Use `/rtp back`.                     |
| `randomteleport.bypass.cooldown` | op      | Ignore RTP cooldown.                 |
| `randomteleport.bypass.cost`     | op      | Ignore RTP and back economy charges. |
| `randomteleport.admin.reload`    | op      | Use `/rtp reload`.                   |


## Configuration (overview)

All options live in `**config.yml`**. After editing, use `**/rtp reload**` (or restart).


| Section                       | Purpose                                                                                                                                                                                                                                                                                                                                                                              |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| *(root)*                      | `debug`, `cooldown-seconds`, `max-location-attempts`, `max-sync-chunk-loads-per-rtp` (cap sync chunk loads per search; `0` = unlimited), `teleport-delay-ticks`, `delay-cancel-move-blocks`, `min-blocks-from-other-players`, `respect-world-border`, `world-border-margin`, `language`, `fallback-language`.                                                                        |
| `economy`                     | `enabled`, `provider`, `currency-id` (CoinsEngine). Per-dimension `**cost*`* / `**cost-***` (RTP) and `**cost-back**` / `**cost-back-***` (`/rtp back`). Omit `**cost-***` to inherit `**cost**`. Omit `**cost-back**` entirely to default `**cost-back**` to `**cost**`; omit a `**cost-back-***` to inherit `**cost-back**`.                                                       |
| `worlds`                      | `mode`: `all` \| `whitelist` \| `blacklist`, plus world name list.                                                                                                                                                                                                                                                                                                                    |
| `ranges`                      | `overworld`, `nether`, `the_end` — min/max X/Z; intersected with world border when `respect-world-border` is true.                                                                                                                                                                                                                                                                   |
| `safety`                      | Extra blocks to treat as unsafe (water, webs, etc.), in addition to lava, fire, and other built-in unsafe blocks.                                                                                                                                                                                                                                                                    |
| `effects`                     | Master `enabled`, `**repeat-countdown-title**` (refresh countdown title each tick during delay), and sounds/titles for search, countdown, success, failure, delay cancel, reload.                                                                                                                                                                                                    |
| `messages.zh` / `messages.en` | Chat/action bar strings; `language` / `fallback-language` control selection. Notable keys: `**delay-actionbar**` (`{seconds}`, `{ticks}`, `{cost_detail}`), `**success**` / `**back-success**` with `**{paid}**`, `**success-paid**` / `**back-success-paid**`, help keys above, `**help-no-permission**` (empty → use `**no-permission**`). Help entries may use YAML multiline block scalars. |


`config.yml` includes **Chinese inline comments** for each section (placeholders and behavior); default message text is provided under both `**messages.zh`** and `**messages.en`**.

## PlaceholderAPI

Registered expansion: `**randomteleport**`. Use **lowercase** identifiers; words are separated by **underscores** (e.g. `cost_back`, not `cost-back`).


| Placeholder                  | Meaning                                                                                 |
| ---------------------------- | --------------------------------------------------------------------------------------- |
| `%randomteleport_cooldown%`  | Cooldown seconds remaining (alias: `%randomteleport_cooldown_seconds%`).                 |
| `%randomteleport_cost%`      | RTP (`/rtp`) cost for the player’s current world (dimension-based).                     |
| `%randomteleport_cost_back%` | `/rtp back` cost for the player’s current world (uses `economy.cost-back-`* in config). |
| `%randomteleport_pending%`   | `true` / `false` — delayed RTP in progress.                                             |
| `%randomteleport_has_back%`  | `true` / `false` — stored return location exists.                                       |


## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for the full text.

---

*README also available in Chinese: [README_zh.md](README_zh.md).*