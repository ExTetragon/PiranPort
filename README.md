# 皮兰港 PiranPort

*「我认为列克星敦就是这个世界的主宰，所有人都应该成为她忠实的奴仆」*

**皮兰港 PiranPort** — 一个面向 **Minecraft 1.21.1 (NeoForge)** 的 **战舰少女R** 主题综合性 mod：玩家可以扮演舰娘，在生存玩法中开采资源、烹饪食物、制作舰装与武器，与主世界、海图副本和深海世界中的深海敌人战斗，并探索独立剧情。内容分为战斗、后勤、剧情三大系统。

> **状态：早期开发，当前版本 0.1.54。** 已有 60 个食物/相关物品、14 个矿物资源物品及 2 个蓝图物品。燃料桶、流体、矿物加工与世界生成已实现；蓝图已支持 23 项许可、合并扩展、宝箱获取及累计收集进度，合并固定产出一张，制造设施尚未实现。自动验证和边界见[开发日志](docs/开发日志.md#blueprint-types-20261023)与[蓝图实现方案](docs/3.2.1-蓝图-实现方案.md)。贴图仍处于占位阶段，世界内界面及第三方模组交互另需人工验收。

## 计划内容

皮兰港分为战斗、后勤、剧情三大系统。

### 战斗系统

- **舰装与负重**：船型与舰装核心、舰装栏、负重
- **武器**：火炮、鱼雷、飞机、深弹、导弹与火箭弹
- **装备**：装甲、声呐、近战、其他远程武器、盾牌和防具、道具
- **状态效果**：高速规避、经验提升、进水、着火、被发现！、装填加速

### 后勤系统

- **资源**：矾土矿、硝土块、原油/燃油、铝/钢、燃料桶；树木与作物仍在后续计划中
- **军工**：船坞、蓝图、武器合成、弹药合成、装弹
- **烹饪**：海军锅、灶、刀与砧板、石磨、方块食物
- **建筑与装饰**
- **皮肤**：船体模型、武器模型、特殊动作

### 剧情系统

- **NPC**：艾拉、深海
- **遗迹**：传送门、补给站、前哨站、深海基地
- **海图**：出击渠、关卡
- **深海意志世界**：主岛与 Boss、外岛
- **成就**

## 开发路线

| 版本 | 内容 |
|---|---|
| 0.1.x | 后勤系统：资源、烹饪模块 |
| 0.2.x | 战斗系统：武器 1（火炮与深弹）、装备、状态效果；后勤系统：军工模块 |
| 0.3.x | 战斗系统：武器 2（鱼雷、飞机、导弹） |
| 0.4.x | 战斗系统：舰装与负重；后勤系统：皮肤模块 |
| 0.5.x | 剧情系统：遗迹；后勤系统：建筑与装饰 |
| 0.6.x | 剧情系统：NPC 模块 |
| 0.7.x | 剧情系统：海图、成就模块 |
| 0.8.x | 剧情系统：深海意志世界模块 |

## 开发环境

| 组件 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.259 |
| ModDevGradle | 2.0.148 |
| Gradle | 9.2.1（Wrapper） |
| JDK | 21 |
| Parchment | 1.21.1 / 2024.11.17 |

## 构建与运行

`JAVA_HOME` 必须指向 JDK 21。如果系统变量仍指向旧版本，可以只在当前终端覆盖：

```bash
export JAVA_HOME="C:/Program Files/Java/jdk-21"
```

```bash
./gradlew build          # 构建
./gradlew runClient      # 启动客户端
./gradlew runServer      # 启动服务端
./gradlew runData        # 生成矿物掉落/标签及图纸配方、奖励和进度
./gradlew runGameTestServer # 执行 79 项服务端自动回归
./gradlew runAcceptanceClient # 独立验收客户端（JEI、spark）
```

完整验收与报告（Python 3.11+，默认不启动图形客户端）：

```powershell
python tools/run_acceptance.py --worldgen
# 首次准备独立客户端的辅助模组，版本来自锁文件并校验 SHA-512
python tools/setup_acceptance_helpers.py
# 同时验证辅助客户端启动、图纸模型及中英文资源，到达主菜单后自动退出
python tools/run_acceptance.py --worldgen --client-smoke
```

报告写入 `build/acceptance/reports/acceptance/summary.json` 和 `summary.md`。`--worldgen` 每次创建独立测试世界；自然油湖密度、视觉效果和第三方管道等未覆盖范围继续保留人工验收。

在 IntelliJ IDEA 中以 Gradle 项目打开本目录，可获得 Minecraft 与 NeoForge 类的代码补全。还需要在 `Settings > Build, Execution, Deployment > Build Tools > Gradle` 把 **Gradle JVM** 设为 JDK 21。

## 燃料桶配置

当前游戏实例的 `config/piranport-common.toml`（TOML 注释固定中英双语，配置界面名称与悬浮说明随游戏语言切换）：

```toml
fuelBarrelCapacity = 16000
fuelBarrelAxialConnection = true
```

默认单桶 16 桶、连接双桶共 32 桶。容量配置单位为 mB，双桶上限为两倍；降容保留旧流体。轴向连接开关在放置时记录：关闭期间放置的新桶保持独立，已有桶不变；之后重新放置才采用新的开关值。具体兼容规则见[矿物实现方案](docs/3.1-矿物资源-实现方案.md#46-公共配置与放置策略)。

## 项目结构

```
src/main/java/com/lex0v0/piranport/           主类、客户端入口、配置
src/main/java/com/lex0v0/piranport/registry/  物品、方块、创造栏注册
src/main/resources/META-INF/                  neoforge.mods.toml
src/main/resources/assets/piranport/          语言文件等资源
src/generated/resources/                      数据生成输出
```

## 模组信息

| | |
|---|---|
| 显示名称 | 皮兰港 PiranPort |
| Mod ID | `piranport` |
| 包名 | `com.lex0v0.piranport` |
| 组织 | lex0v0 |
| Maven Group | `com.lex0v0.piranport` |
| 产物 | `piranport-0.1.54.jar` |

## 版本策略

- 当前版本为 `0.1.54`，累计 36 项功能实现和 18 项 bug 修复；统计依据见[开发日志](docs/开发日志.md)。
- 每完成一项实际功能或独立修复，末段加 1；文档修改不计。首位与中段仅在用户明确要求时更新。

- 开发与测试固定使用 NeoForge `21.1.259`。
- `neoforge.mods.toml` 中的依赖下限为 `[21.1.0,)`，依赖声明允许其他 21.1.x；目前实际验证版本为 21.1.259，未保证所有旧版本兼容。

## 许可证

GPL-3.0 — 见 [LICENSE](LICENSE)。

[English](README.en.md)

## 实现与验证文档

- [3.1 实现方案](docs/3.1-矿物资源-实现方案.md)
- [测试清单](docs/3.1-矿物资源-测试清单.md)
- [详细修复说明](docs/3.1-矿物资源-修复说明.md)
- [矿物资源表](docs/后勤-矿物.xlsx)
- [食物表及实现状态](docs/舰R食物.xlsx)
- [原始策划书](docs/策划书.pdf)
- [开发日志与实施校正](docs/开发日志.md)
- [项目现状与开发指引](AGENTS.md)

重复资源通过 runData 生成到 src/generated/resources，并与 Gradle Wrapper 一起纳入版本控制。发行 jar 不包含 GameTest 代码。
可用 python tools/verify_resources.py 检查 JSON 和贴图引用；缺失食物占位图可用 python tools/generate_food_placeholders.py 重建。
