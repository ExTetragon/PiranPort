# 船坞方块模型与运行时接入

2026-10-31，吊架细亮边修订版。根据五种改造核心原图组织浅灰坞墙、钢灰舰体、红船底与木甲板，保持鲜明蓝、黄、红强调色；蓝色龙门吊和黄色吊臂顶面沿边分布细高光，中央保留主色。保留桁架花纹与加密黄黑斜纹；坞体各面按照实际结构绘制，GTM仅提供工业细节风格参考。

![吊架细亮边修订预览](previews/shipyard-preview-v5-edge-highlights.png)

当前模型已在0.1.125接入船坞方块和物品，并同步四向碰撞箱、选中框及破坏粒子。目录沿用初稿路径，图中仍为Blockbench原生预览；游戏资源与运行时检查见下文。

| 项目 | 规格 |
|---|---|
| 范围 | 16×15.5×16模型单位，完整包含在单个方块内 |
| 几何 | 56个轴对齐长方体，6个职责分组 |
| 结构 | 坞池、承台与轨道、待建舰体、门式吊架、悬臂吊、控制箱 |
| 材质 | 128×64像素图集，按部位布置16×16材质区，33色调色板 |
| 像素密度 | 主体每模型单位1纹理像素；单柱警示斜纹每单位2像素；吊架顶面细亮边每单位4像素，复用图集预留区域 |
| 源文件 | 原生Blockbench通用模型与Aseprite图集 |

## 文件

- [Blockbench模型](sources/shipyard_draft.bbmodel)，嵌入完整纹理，可独立打开编辑。
- [Aseprite图集源文件](sources/shipyard_atlas.aseprite)与[PNG图集](textures/shipyard_atlas.png)。
- [Java方块模型源稿](models/shipyard_draft.json)保留几何与UV；发行模型另外继承原版方块物品变换，并配置边界面裁切和独立粒子材质。
- [正面斜视](previews/front_three_quarter.png)、[背面斜视](previews/rear_three_quarter.png)、[俯视](previews/top.png)、[正视](previews/front.png)、[底视](previews/bottom.png)。
- [像素与几何规格](design-spec.json)、[检查记录](validation.json)、[Aseprite检查](aseprite-validation.json)。
- [配色与花纹定义](pattern_spec.py)、[几何与UV构建](build_plan.py)保留可重绘规格。

图集由实时Aseprite绘制、导出。模型通过本机Jason Blockbench MCP创建，预览是Blockbench对实际模型的正交渲染；评审图只做背景、缩放和排版。

## 当前造型

船体相对首版宽度增加20%、长度增加15%、高度增加12%，仍位于坞池与吊架之间。保留两侧、后方间隙，收紧龙门吊底座的内缘，舰体不与两套吊装结构相交。

颜色从舰R原图中的中性灰、蓝色、黄漆、红船底及木甲板取样后调整。蓝色吊架、黄色臂杆和红船底使用更鲜明的配色，浅灰坞墙、钢灰舰体与木甲板逐像素保持上一版。原图采样坐标、原始色与当前调整色见[取色记录](references/wgr-palette-samples.json)。

窄侧面UV继续避开材质首行高光，薄桁架从结构纹路区域取样。吊架8个顶面改用预留区域中的专用细边纹理，每模型单位4像素，亮边宽度为0.25模型单位；中央无高光色斑。几何与碰撞保持不变。[前后对照](previews/edge-highlight-comparison.png)及[调整前归档](revisions/before-edge-highlights/README.md)保留原貌。

33色调色板与上一版完全相同，仅重新布置蓝黄吊架的高光像素和8个顶面UV；其他现有材质保持原样。[本轮检查](edge-highlight-validation.json)核对沿边像素、顶面内侧无亮斑、原生导出与生产资源一致性。[上一轮饱和度对照](previews/saturation-uv-comparison.png)及[当时检查](saturation-uv-validation.json)保留调色依据。

外壁表现混凝土浇筑板缝与墙脚，长边包边表现压边、金属轨面和锚固点，后墙按自身宽度独立安排板缝。内壁使用分层钢衬，内部底板为钢板、侧边排水槽和承台轨道；底面为与轨道、承台位置对应的纵梁、横撑与小型检修盖。

舰体钢板、红船底的局部高光、纵向木甲板和舰桥窗带一并重绘。黄色臂杆的桁架节奏、柱身斜纹和双倍像素密度保持；造型、尺寸及两套吊装结构的间隙保持。

[更早的配色对照](previews/wgr-repaint-comparison.png)与[重绘前归档](revisions/before-wgr-repaint/README.md)保留调整依据。

首版源文件、素材和预览保留于[第一版归档](revisions/draft-01/README.md)。

## 原图参考

五张原图来自本机舰R v5.5.1拆包的`assets/data/ui/icon.unity3d`：`core_10141`、`core_10241`、`core_10341`、`core_10441`、`core_10541`，均为126×126。

参考原图统一保存在`E:/Leisure/Coding/Minecraft/mc-refs/WarshipGirlsR/v5.5.1/remodel-cores`；[来源索引](references/sources.json)记录原包路径、Texture2D标识和SHA-256。造型提取五图共有结构，舰体使用中性的施工状态，不绑定具体舰种或制造配方。

工业细节风格参考[GregTechCEu/GregTech-Modern](https://github.com/GregTechCEu/GregTech-Modern)固定提交`ec40d99d6a6e24d130bf4ddae0d7243281afad45`的六张材质。借鉴金属压边、拼接、长槽与紧固点的组织方式，图案按船坞结构原创绘制。原图位于统一参考库，详见[来源索引](references/gregtech-modern.json)及[参考拼图](previews/gregtech-modern-reference.png)。

## 原生美术检查

已核对56个元素、6个分组、单方块边界、舰体与吊装结构无相交、各面的UV、嵌入纹理与Aseprite导出一致，以及五个视角均无画面裁切。Aseprite图集检查通过；此部分为Blockbench预览检查，运行时验证单独记录。

## 运行时接入

0.1.125将模型和主图集接入船坞方块及其物品；物品继承原版地面、手持和展示变换。主图集与源文件逐字节一致，破坏粒子使用独立的16×16坞墙材质。

| 游戏资源 | 内容 |
|---|---|
| [方块模型](../../../src/main/resources/assets/piranport/models/block/shipyard.json) | 56个元素，沿用现有四向方块状态旋转 |
| [主图集](../../../src/main/resources/assets/piranport/textures/block/shipyard_atlas.png) | 128×64，与Aseprite导出一致 |
| [粒子贴图](../../../src/main/resources/assets/piranport/textures/block/shipyard_casing.png) | 独立坞墙纹理；[Aseprite源文件](sources/shipyard_casing.aseprite) |
| [轮廓代码](../../../src/main/java/com/lex0v0/piranport/block/ShipyardBlock.java) | 模型元素并集与四向缓存 |

碰撞箱与选中框均由模型56个长方体的并集组成，最高到15.5/16格；坞池、吊架和外围空处不再占据整格碰撞。四个方向与方块状态中的模型旋转一致，在类加载时计算并缓存。开放造型使用非整格遮挡与轮廓光照。

开发工厂和弹药生产线仍借用旧`block/shipyard.png`，该文件保持原样。菜单、配方、材料扣除、燃油、缓存、掉落与自动化逻辑保留原行为。

首次接入时，构建、324个JSON资源检查和232项required GameTest通过；验收客户端核对四向模型烘焙、贴图、粒子、物品变换及面片与服务端轮廓的对应。初次报告保留在[修订前运行时记录](revisions/before-saturation-uv/runtime-validation.json)。

本轮只重新布置吊架高光和8个顶面的UV，已重新执行Aseprite检查、324个JSON资源检查、构建和JAR资源一致性核对。56个元素和碰撞代码与上一版一致，未重复执行GameTest服务器或启动游戏客户端；预览来自Blockbench。

证据见[运行时验证](runtime-validation.json)与[完整验收报告](../../../build/shipyard-integration-20261031/artifacts/reports/acceptance/summary.json)。修改美术后须同步发行资源；修改几何后还须同步轮廓数据并运行四向回归。
