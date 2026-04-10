# RandomTeleport

面向 **Paper / Spigot** 的随机安全传送插件：按维度配置范围、与世界边界求交、可选传送延迟、`**/rtp back`**、中英双语聊天消息，并支持 **Vault** 或 **CoinsEngine** 扣费。

## 功能概览

- `**/rtp`** — 在玩家**当前世界**内随机选点并传送到安全位置（主世界/末地偏地表扫描；下界在合法高度随机找落脚点）。可按配置排除水、蜘蛛网、洞穴藤蔓、滴水石锥等，并可要求与其他玩家保持最小距离。**`max-sync-chunk-loads-per-rtp`** 限制单次搜索最多同步加载多少个未生成区块（减轻主线程卡顿；`0` 为不限制）。寻路时可用**动作条**提示；若 `**teleport-delay-ticks`** 大于 0，延迟期间动作条显示**倒计时**与**费用说明**（见 `messages.*` 的 `delay-actionbar`、`delay-cost-free`、`delay-cost-will-charge`），可与标题倒计时（`effects.repeat-countdown-title`）同时使用。
- `**/rtp back`** — 回到**最近一次成功 RTP 之前**记录的位置（**每名玩家只保存一条**，不是历史栈；再次 RTP 成功会覆盖；成功返回后清空）。**返回**价格可与 RTP 分开配置，按维度生效。
- `**/rtp help`** — `**help-header**`、每个世界多行的 `**help-line-range**` / `**help-line-no-range**`、末尾 `**help-footer**`（需 `randomteleport.use`）。若无任何允许的世界，则发送 `**help-no-worlds**`（留空或不写该项 → 使用该语言文件内置中文/英文回退句）。
- `**/rtp reload**` — 重载配置（权限 `randomteleport.admin.reload`）。若 YAML 损坏或解析失败，将保留上一版配置、在控制台打印异常，并向执行者发送 `**reload-failed**`。
- **经济** — `economy.provider`：`auto` | `vault` | `coinsengine`（`auto`：优先 Vault，否则 CoinsEngine）。`**economy.enabled: false`** 时不扣费。成功传送等文案可通过 `**success**` / `**back-success**` 的 `**{paid}**` 与 `**success-paid**` / `**back-success-paid**` 合并显示扣费信息。传送成功时扣款；扣款后若传送失败会尝试退款。**CoinsEngine**：扣款后余额与预期不一致时，会尽量恢复到扣款前余额并按失败处理（与「余额不足」相同的玩家提示路径）。
- **PlaceholderAPI**（可选）— 扩展名 `randomteleport`（见下文）。

## 运行环境

- 服务端：**Paper** 或 **Spigot**，`api-version` 1.20 及以上。  
- **Java 17+**。  
- 可选：**Vault** + 任意对接 Vault 的经济插件；**CoinsEngine**；**PlaceholderAPI**。

## 编译

```bash
mvn clean package
```

生成文件：`target/RandomTeleport-1.0.0.jar`（版本以 `pom.xml` 为准）。

## 安装

1. 将 jar 放入服务端 `plugins` 目录。
2. 启动服务器完成首次生成配置（不建议仅靠热插拔代替完整重启）。
3. 按需修改 `plugins/RandomTeleport/config.yml`。
4. 执行 `**/rtp reload**` 或重启使配置生效。

## 命令


| 命令                     | 说明                                         |
| ---------------------- | ------------------------------------------ |
| `/rtp`                 | 在当前世界随机传送（别名：`randomtp`、`randomteleport`）。 |
| `/rtp back`            | 返回上次 RTP 成功前的位置（可单独计费）。                    |
| `/rtp help`             | 查看各世界可传送范围与费用。                             |
| `/rtp reload`          | 重载配置。                                      |


## 权限


| 权限                               | 默认     | 说明                       |
| -------------------------------- | ------ | ------------------------ |
| `randomteleport.use`             | `true` | 使用 `/rtp` 与 `/rtp help`。 |
| `randomteleport.back`            | `true` | 使用 `/rtp back`。          |
| `randomteleport.bypass.cooldown` | op     | 忽略 RTP 冷却。               |
| `randomteleport.bypass.cost`     | op     | 忽略 RTP 与返回的经济扣费。          |
| `randomteleport.admin.reload`    | op     | 使用 `/rtp reload`。        |


## 配置说明（摘要）

全部项在 `**config.yml**` 中，修改后请 `**/rtp reload**` 或重启。


| 节点                            | 作用                                                                                                                                                                                                                                                                                                  |
| ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| *（根键）*                        | `debug`、`cooldown-seconds`、`max-location-attempts`、`max-sync-chunk-loads-per-rtp`（限制单次搜索的同步区块加载，`0` 为不限制）、`teleport-delay-ticks`、`delay-cancel-move-blocks`、`min-blocks-from-other-players`、`respect-world-border`、`world-border-margin`、`language`、`fallback-language`。                                                                                   |
| `economy`                     | `**enabled*`*、`provider`、CoinsEngine 的 `**currency-id**`。各维度 `**cost` / `cost-***`（RTP）与 `**cost-back` / `cost-back-***`（返回）。未写某维度的 `**cost-***` 时继承 `**cost**`；未写 `**cost-back**` 时默认与 `**cost**` 相同；未写某维度的 `**cost-back-***` 时继承 `**cost-back**`。                                                 |
| `worlds`                      | `mode`：`all` | `whitelist` | `blacklist`，以及世界名列表。                                                                                                                                                                                                                                                   |
| `ranges`                      | `overworld`、`nether`、`the_end` 的 X/Z 范围；若开启 `respect-world-border` 会与边界求交。                                                                                                                                                                                                                          |
| `safety`                      | 在岩浆、火等内置不安全类型之外，额外视为不安全的方块（水等）。                                                                                                                                                                                                                                                                     |
| `effects`                     | 总开关 `**enabled**`、`**repeat-countdown-title**`（延迟期间是否每刻刷新倒计时标题），以及搜索、倒计时、成功、失败、延迟取消、重载等音效与标题。                                                                                                                                                                                                       |
| `messages.zh` / `messages.en` | 聊天与动作条文案；`language`、`fallback-language` 控制语言。常用占位符：`**delay-actionbar**`（`{seconds}`、`{ticks}`、`{cost_detail}`）、`**success` / `back-success**` 的 `**{paid}**`（配合 `**success-paid**` / `**back-success-paid**`）、帮助相关键与 `**help-no-permission**`（留空则沿用 `**no-permission**`）。帮助条目可在 YAML 中用多行块（`|`）编写。 |


更细的说明与占位符见 `**config.yml` 内中文注释**（按小节分组的帮助提示）。

## PlaceholderAPI

扩展标识：`**randomteleport`**。占位符名一律**小写**，多个单词用**下划线**连接（例如 `cost_back`，不要写成 `cost-back`）。


| 占位符                          | 含义                                                    |
| ---------------------------- | ----------------------------------------------------- |
| `%randomteleport_cooldown%`  | 剩余冷却秒数（等价占位符：`%randomteleport_cooldown_seconds%`）。        |
| `%randomteleport_cost%`      | 当前世界 `**/rtp`** 价格（按维度读取 `economy.cost-*`）。           |
| `%randomteleport_cost_back%` | 当前世界 `**/rtp back**` 价格（按维度读取 `economy.cost-back-*`）。 |
| `%randomteleport_pending%`   | `true` / `false`，是否在延迟 RTP 中。                         |
| `%randomteleport_has_back%`  | `true` / `false`，是否存有可返回位置。                           |


## 开源协议

本项目采用 **MIT License**（MIT 许可证）。完整条款见仓库根目录 [LICENSE](LICENSE) 文件。

---

*English README: [README.md](README.md).*