# 皮兰港 PiranPort

*「我认为列克星敦就是这个世界的主宰，所有人都应该成为她忠实的奴仆」*

**皮兰港 PiranPort** — 一个面向 **Minecraft 1.21.1 (NeoForge)** 的 **战舰少女R** 主题综合性 mod：玩家可以扮演舰娘，在生存玩法中开采资源、制作舰装，与主世界、海图副本和深海世界中的深海敌人战斗，并体验舰R风格的烹饪与家具。

> **状态：早期开发。** NeoForge 21.1.259 的项目骨架已经就位并可成功构建；具体内容尚未实现。

## 计划内容

### 舰装系统

- **舰装核心与舰装栏**：大/中/小型及潜艇核心，决定负重上限、栏位数量、基础航速与弹药/燃料容量
- **皮肤与舰装模型**：皮肤核心、船体模型、模块化武器模型与舰娘放飞动作
- **负重与变身**：负重计算、舰娘状态、变身与解装

### 装备与战斗

- **炮击**：多口径火炮，HE、AP 与防空弹，爆炸/穿甲伤害
- **雷击**：鱼雷发射器，磁性、声导、氧气鱼雷与再装填设施
- **航空**：侦察机、战斗机、攻击机，火控锁定、整备与回收
- **反潜**：声呐、深水炸弹投射器与潜艇
- **导弹与火箭弹**：反舰导弹、防空导弹与火箭弹
- **装甲与手持道具**：附加装甲，以及损害管制、发烟筒、照明弹等舰R 主题道具

### 资源与方块

- 矾土矿、盐，以及铝、盐相关材料
- 8 种作物、7 种树木（含季节叶片变色）
- 建筑装饰方块与功能方块：武器合成台、弹药合成台、弹药装填器、蓝图收纳箱

### 食物与 Buff

- 锅、灶、刀与砧板、石磨等烹饪方块
- 可放置并分次食用的食物方块
- 高速规避、经验提升、进水、着火、被发现、装填加速等 Buff

### NPC 与海图

- 深海与舰娘 NPC
- 主世界遗迹：传送门遗迹、补给站、前哨站、深海基地
- 海图副本与章节关卡、深海世界维度

## 开发路线

| 版本 | 内容 |
|---|---|
| 0.1.x | 基础物品、食物注册，矿物、作物、树木生成及相关合成 |
| 0.2.x | 方块食物，装饰家具和厨具方块，港区机器 |
| 0.3.x | 战斗系统 1：火炮、深弹、装甲，手持道具及 Buff |
| 0.4.x | 战斗系统 2：鱼雷、飞机、导弹及其 AI |
| 0.5.x | 舰装栏、皮肤栏及变身系统 |
| 0.6.x | 舰娘深海 NPC 模型及 AI |
| 0.7.x | 主世界遗迹、建筑结构，新群系 |
| 0.8.x | 副本维度、传送门、剧情，成就系统 |
| 0.9.x | 整体测试，数值调整 |

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
./gradlew runData        # 数据生成
```

在 IntelliJ IDEA 中以 Gradle 项目打开本目录，可获得 Minecraft 与 NeoForge 类的代码补全。还需要在 `Settings > Build, Execution, Deployment > Build Tools > Gradle` 把 **Gradle JVM** 设为 JDK 21。

## 项目结构

```
src/main/java/com/lex0v0/piranport/  主类、客户端入口、配置
src/main/resources/META-INF/          neoforge.mods.toml
src/main/resources/assets/piranport/  语言文件等资源
src/generated/resources/              数据生成输出
```

## 模组信息

| | |
|---|---|
| 显示名称 | 皮兰港 PiranPort |
| Mod ID | `piranport` |
| 包名 | `com.lex0v0.piranport` |
| 组织 | lex0v0 |
| Maven Group | `com.lex0v0.piranport` |
| 产物 | `piranport-0.1.0.jar` |

## 版本策略

- 开发与测试固定使用 NeoForge `21.1.259`。
- `neoforge.mods.toml` 中的依赖下限为 `[21.1.0,)`，以兼容全部 21.1.x。

## 许可证

GPL-3.0 — 见 [LICENSE](LICENSE)。

[English](README.en.md)
