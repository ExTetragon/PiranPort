# AI 辅助 Minecraft 贴图与建模：DSH / Codex 插件与 MCP 调研清单

> 状态快照：2026-10-29。检索范围：B 站（含评论区）、GitHub、npm registry、各项目 README。
> 用途：为 PiranPort 后续正式美术（16×16 / 32×32 物品与方块贴图、方块与实体模型）选型，不替代 `docs/2.2-装备-贴图来源.md` 等既有美术来源记录。
> 结论先行：贴图和建模是两条链。贴图走「像素画 MCP（Aseprite）+ 材质工作台（matoga）+ 可选 AI 概念图」；建模走「Blockbench MCP / DSH 原生 Blockbench 插件」；YSM 皮肤只有专用插件能做顺；模组级闭环用 `dsh-mc-art-panel` 的 GameTest 裁判。

---

## 1. DSH 原生插件

| 插件 | 定位 | 关键能力 | 要求 / 安装 |
|---|---|---|---|
| **dsh-blockbench-agent**（[yan-mc/blockbench-ysm-agent](https://github.com/yan-mc/blockbench-ysm-agent)，v0.4.0） | 在 DSH 里驱动隐藏运行的 Blockbench | 30 个 `bb_*` 工具：语义建模（骨骼/方块/逐面 UV）、像素贴图（含 PBR `_n`/`_s`）、多机位无头渲染回传 PNG 附件、变化检测、检查点回滚、YSM 校验与 zip 导出、`/show /hide` 人工接管、游戏内加载验证 | Windows 10/11 + Blockbench 5.2.x + Electron + Node ≥ 20；下载 release zip，手动挂本地 bundle（改 `cordis.patch.yml` 的 `workspaceRoot`/`executable`） |
| **dsh-mc-art-panel**（[GMH13552/dsh-mc-art](https://github.com/GMH13552/dsh-mc-art)，npm 0.3.4） | 右侧「MC 资产」面板 + 「MC 模组工作室」预设 | 3D 取景框 + 九格物品栏；像素编辑器（铅笔/油漆桶/取色/橡皮、24 步撤销、按面编辑）；结构/群系编辑器；`@ 提意见`；配 `mc-art` 确定性美术引擎（调色板/明度/孤立像素/平铺接缝度量、方块实体 UV 审计）与 `mc-mod` skill；GameTestServer 退出码裁判 | `dsh plugin --profile web add <repo>/panel` 或 npm 包名；只写项目自己的资源包，不写 jar |
| **dsh-blender**（[CheshireJCat/blender](https://github.com/CheshireJCat/blender)，0.2.4） | 无头 Blender 3D 生产链 | 13 个工具（status/scene/import/python/preview/render/render_frames/export/validate/helper）+ 30 个 skill；GLB/FBX/OBJ/STL/USD/PLY 导出；26 个参考图/线框/多视图/UV/贴图/修复/动画 QA helper | DSH 0.2.0-rc.2 / 0.2.1-alpha.x；Node 22.12+；Blender 4.3+ 在 PATH；无需 add-on 或控制端口 |
| **@jojonadam/dsh-blender**（0.1.2） | 轻量 Blender 包 | headless bpy runner、渲染/导出 helper、建模 cookbook、程序化资产管线 | npm |
| **dsh-blender-mc**（[TreeCodeTyper/dsh-blender-mc](https://github.com/TreeCodeTyper/dsh-blender-mc)，0.4.6） | MC 存档 → Blender 封面/短视频 | MCprep、anvil/mca 读档、自动挑机位、搭场景、渲染、OpenCV 审图（朝向/接地/曝光/布光/景深）不合格自动重渲；支持设置页上传地图 zip 与皮肤 | npm；做展示图/封面，不做物品贴图 |
| **Tripo3D-Plugin-dsh**（[Tripo-Developer](https://github.com/Tripo-Developer/Tripo3D-Plugin-dsh)） | 文/图 → 带贴图、可绑定的 3D 资产 | 调 tripo-cli（text-to-3D / image-to-3D） | npm；适合先出体量再 retopo 进 Blockbench |
| **@dfy-plugins/dsh-image-generation**（0.1.4）/ **@lamplitisles/dsh-imagegen**（0.7.0） | 通用文生图 / 改图 | Skill + 工具 + 设置页 | npm；用于贴图概念稿 |
| **pbr-render**（dhb861832993-star） | GLB/GLTF PBR 预览 | IBL、轨道控制、baseColor/normal/roughness/metallic/AO/emissive 通道检查、线框 | DSH 插件；验收 `_n`/`_s` 用 |

> 另有 `dsh-minecraft`、`dsh-minecraft-agent`、`dsh-minecraft-ui`：让 agent 进游戏干活或换 UI 主题，**不是做美术**，不要混淆。

---

## 2. MCP 服务器（DSH 与 Codex 通用）

| MCP | 定位 | 关键能力 | 备注 |
|---|---|---|---|
| **[blockbench-mcp-plugin](https://github.com/jasonjgardner/blockbench-mcp-plugin)**（jasonjgardner，584★） | 目前最主流的 Blockbench MCP | 插件版 HTTP `http://localhost:3000/bb-mcp`；无头 stdio 版直接读写 `.bbmodel`（`npx -y github:jasonjgardner/blockbench-mcp-plugin --root ./models`）；README 直接给 Codex/Claude Code/VS Code 配置 | 插件地址 `https://jasonjgardner.github.io/blockbench-mcp-plugin/mcp.js`；两种服务可同时用 |
| **[BlockBenchMCP](https://github.com/RenasDemirbas/BlockBenchMCP)**（74 tools） | 建模 + 贴图 + 动画全家桶 | 骨骼/方块/镜像、逐面绘制、材质（height/normal/MER）、截图与多视图自检、glTF 导出、像素风渲染、undo 历史 | GPL-3.0-or-later；Blockbench 5.2+、Node 18+ |
| **[blockbench-mcp](https://github.com/WaitMyDawn/blockbench-mcp)**（Python） | 中文文档、明确面向 Codex | Pillow 像素绘制 op（fill/线性/径向渐变/阴影/noise/软笔刷）、Bedrock/Java Block/CEM/GeckoLib 导出、软渲染 + ASCII 三视图、双栈 undo、事务回滚 | 一期文档引擎，二期插件桥（截图/撤销/重载） |
| **[Minecraft-Blockbench-MCP](https://github.com/zkonikishi/Minecraft-Blockbench-MCP)** | BetterModel / ModelEngine / CraftEngine 工作流 | 建模、贴图、骨骼动画、bbmodel 与 CEM 导入 | 面向服务端模型插件生态 |
| **[aseprite-mcp](https://github.com/diivi/aseprite-mcp)**（839★）、**[pixel-mcp](https://github.com/willibrandon/pixel-mcp)**（155★）、**[aseprite-ai-artist](https://github.com/with-pebbly/aseprite-ai-artist)** | 像素画专用 | 调色板、抖动、阴影、动画、spritesheet；`aseprite-ai-artist` 明确支持 Claude Code / **Codex** / Gemini CLI / Cursor | 贴图主战场 |
| **[comfy-mcp](https://github.com/Comfy-Org/comfy-mcp)**（310★）、[comfyui-mcp-server](https://github.com/joenorton/comfyui-mcp-server)（410★） | 本地 SD/ComfyUI | 文生图/图像编辑工作流 | 出概念图，**不要直接缩图当 16×16** |
| **[mcdev-mcp](https://glama.ai/mcp/servers/use-ai-for-mc/mcdev-mcp)** | 进游戏运行期验证 | 调 DebugBridge 在 JVM 跑 Groovy、游戏状态快照 | 验收，不是画图 |

### 2.1 Codex 侧配置

```bash
# Blockbench 插件版（HTTP，需 Blockbench 已开并装插件）
codex mcp add blockbench --url http://localhost:3000/bb-mcp

# 无头 stdio（不依赖 Blockbench 运行）
codex mcp add bbmodel -- npx -y github:jasonjgardner/blockbench-mcp-plugin --root ./models

# 或走插件市场
codex plugin marketplace add jasonjgardner/blockbench-mcp-project --ref codex
codex plugin add blockbench-mcp@blockbench-mcp-project
```

Windows 下若 Codex 不能直接拉起 `npx`，把 command 设为 `cmd`，args 前置 `/c npx`。

### 2.2 DSH 侧配置

MCP 与 Codex 同源：HTTP 指向 `http://127.0.0.1:3000/bb-mcp`，或 stdio 指向 `npx -y github:jasonjgardner/...`。DSH 原生插件（第 1 节）不经过 MCP，直接注册 `bb_*`/面板工具。

---

## 3. Codex 插件与技能

- **SpriteCook for Codex**：在 Codex 插件市场安装，agent 直接生成 sprite、动画、tileset。
- **minecraft-imagegen skill**：`npx mdskill add Jahrome907/minecraft-agent-skills/minecraft-imagegen`，用 Codex 内置图像生成出 pack 图标、宣传图、概念贴图、缩略图。
- **Srar-Git/minecraft-modeling-item**：B 站「GPT 操作 blockbench 制作模型全教程（含 skill）」作者自制建模 skill。

---

## 4. 贴图 / 材质包专用工具（非 MCP）

| 工具 | 说明 |
|---|---|
| **[matoga](https://github.com/xiongmiaozai/matoga) v3.2.0**（MIT） | 本地 Web 材质工作台，`localhost:5273`。文件夹对比 + MD5 差异热力图、2D/3D PBR 实时预览、POM 视差、UV 编辑、节点材质、Blockbench 联动、资产库缩略图；不依赖安装、不上传文件 |
| **BlockPixel CraftLab**（[B 站 av116499746463486](https://www.bilibili.com/video/av116499746463486)） | 内置 300+ 原版方块/物品/实体纹理路径，三级分类+搜索；选纹理 → 选 PNG → 一键出 `.zip` 材质包 |
| **StarItem / SIC**（[B 站 av117420530406536](https://www.bilibili.com/video/av117420530406536)） | UP 主训练的物品贴图 AI，输入名字出透明背景 PNG，直接进材质包/模组；**仅非商用** |

---

## 5. B 站线索与评论区反馈

| 视频 | 数据 | 关键信息 |
|---|---|---|
| [DSH-Blockbench 原生插件](https://www.bilibili.com/video/av117462641148960)（严MC工作室老板） | 8922 播放 / 241 评论 | 就是 `yan-mc/blockbench-ysm-agent`；简介写明「YSM 建模/贴图/动画/无头渲染/游戏验证的闭环工具链（30 tools + Client UI）」 |
| [让 AI 用 Blockbench MCP 做蜗牛](https://www.bilibili.com/video/av116587826779663)（星断肃昭） | 8.8 万播放 / 270 评论 | 工具为 jasonjgardner 的 blockbench-mcp-plugin；评论区集中讨论能力边界 |
| [GPT 操作 blockbench 制作模型全教程（含 skill）](https://www.bilibili.com/video/av117516261198662)（RadorRador） | 1690 播放 | 附自制建模 skill `Srar-Git/minecraft-modeling-item` |
| [使用任意 AI + BlockBench 创建 MC 风格模型](https://www.bilibili.com/video/av117501144860482)（tsrdzy） | 1891 播放 | 用 Qoder + Qwen3.8-Flash；图文教程 `tsrcraft.cn/post/ac206f01` |
| [BlockPixel CraftLab](https://www.bilibili.com/video/av116499746463486) | 4164 播放 | 材质包一键生成工具 |
| [matoga 贴图对比 + 实时 PBR 预览](https://www.bilibili.com/video/av117511295079782) | 226 播放 | 开源 `xiongmiaozai/matoga` |
| [专门给 MC 画物品贴图的 AI](https://www.bilibili.com/video/av117420530406536) | 1.69 万播放 | StarItem，非商用 |

### 评论区原话（摘要）

- DSH-Blockbench 视频下 @白今梦礼：「装了插件，给全身立绘让 dsh 建模建，出来的还是**一坨**，甚至放了开源的奶油桃 ysm 让她参考。」→ **2D 立绘直喂 3D 不成立**，需 `/show` 接管 + 参考模型 + 多轮渲染自检。
- 蜗牛视频下 @很可能是会说话的狗：「只能做最简单的 mc 原版风格模型……我让他做个车，整个结构都崩了……**甚至没法做圆柱体**。」
- 同视频 @无米_Official 回复：「是 cline 不行，**ds 也没有多模态**。你要用 claude code + 带多模态视觉理解的模型」；@星断肃昭 补充「视频里我用的就是 Claude 模型」。
- @半片枫树叶：「**ysm 模型就算了，没有专门喂过的，是基本不可能的**。」

> 评论区结论：模型必须能看图（多模态），否则「改→渲染→看→改」的闭环断裂；Blockbench MCP 适合方块/低模，不适合曲面载具/角色；YSM 走专用插件。B 站评论接口未登录只返回第 1 页（241/270 条中的前 20 条），以上为抽样。

---

## 6. 推荐组合

| 目标 | 组合 |
|---|---|
| 16×16 / 32×32 物品、方块贴图 | **Aseprite MCP** 画 + **matoga** 对比和 PBR 预览；批量概念稿用 ComfyUI MCP 或 `dsh-image-generation` 出图 → 降采样 + 锁色板 |
| MC 低模、方块模型（Java/Bedrock/GeckoLib） | **blockbench-mcp-plugin**（Codex/DSH 通用）或 **dsh-blockbench-agent**（DSH 原生，回传渲染） |
| 模组级美术 + 可验证（贴图+模型+GameTest） | **dsh-mc-art-panel** + `mc-art` 引擎（确定性栅格化、像素度量、UV 审计） |
| YSM 皮肤 | **dsh-blockbench-agent**（唯一成体系） |
| 高模 / 渲染 / 展示图 | **dsh-blender** 或 Blender MCP；封面/动画用 **dsh-blender-mc** |
| 参考图快速起 3D | **Tripo3D-Plugin-dsh** / `tripo-cli`，再 retopo 进 Blockbench |

---

## 7. 避坑与许可

- **多模态是前提**：DS（DeepSeek）在该场景无视觉，cline + DS 组合已被评论区明确否定；要么换多模态模型，要么用「强制回传截图 + read_image」的插件（DSH Blockbench Agent 每次渲染返回 PNG 附件）。
- **图片会重复计费**：DSH 中图片从产生起每次请求都参与计费，渲染自检要控制张数与分辨率（该插件默认 512px、每轮 ≤4 张）。
- **16px 像素风不要用 SD 直出后缩图**：会糊、色板膨胀、接缝脏；用像素专用 MCP 或 `mc-art` 的调色板/接缝度量。
- **Blockbench 是 GPL-3.0-or-later**：BAE 属衍生发行，发布需随包提供对应源码/修改说明与许可证，不得暗示官方背书；RenasDemirbas 的 MCP 也是 GPL-3.0，商用注意。YSM-Utils 为 MIT，matoga 为 MIT。
- **Blockbench MCP 的 HTTP 桥只监听 127.0.0.1**；RenasDemirbas 1.6.0 及更早存在「任意网页可连桥」的问题，1.6.1 已修，务必用新版。
- **DSH 装完必须完全重启**：桌面端要把 Electron 主进程也退出（托盘常驻时关窗口不算），否则宿主与客户端版本不一致，会出现「宿主没有这个方法」。
- **本机安装提示**：desktop profile 只能用 DSH 自带 carrier CLI 管理插件；web profile 用 npm 全局 `dsh`（需 PATH 有 pnpm）。npm 全局 `dsh` 会拒绝 desktop。

---

## 8. 待核实 / 未验证

- `dsh-blockbench-agent` 与 `dsh-mc-art-panel` 未在本机实装验证；其 Windows/GPU 稳定性、隐藏窗口 WebGL 出图在远程桌面下的表现需按各自 MANUAL/ENGINEERING 先行冒烟。
- `dsh-blender` 的兼容性矩阵为作者自测（macOS），Windows 与 DSH 版本匹配需实测。
- BlockPixel CraftLab、StarItem 未查到公开仓库，只有 B 站获取方式，来源与许可不明，仅作候选。
- 评论区抽样不完整（B 站未登录只出第 1 页评论）。

---

## 9. 检索记录

- 2026-10-29：`platform_search`（B 站、GitHub）、npm registry search、GitHub raw README、B 站视频页 meta 与 `api.bilibili.com/x/v2/reply` 第 1 页。
- 主要来源：上述链接与 [awesome-dsh-plugin](https://github.com/awesome-dsh-plugin/awesome-dsh-plugin) 清单。

## 10. 本机接入与小样实测

2026-10-30：已接入 Aseprite AI Artist 0.8.2（live）与 Jason Blockbench MCP 1.10.1（桌面HTTP），本机分别为 Aseprite 1.3.18.6-dev、Blockbench 5.2.2、Node 24.19.0。完成16×16食物、32×32弹药、16×16平铺钢板及20方块Java模型小样；原生撤销／重做、导出引用与独立NeoForge1.21.1客户端资源加载均有记录。编辑器三视图已检查，世界内摆放与碰撞不在本轮验证范围。路径、固定版本、接入注意和证据见[本机接入与小样验证](art-samples/2026-10-30/README.md)。
