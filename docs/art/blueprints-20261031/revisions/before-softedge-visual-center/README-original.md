# 蓝图物品贴图

> 原0.1.135–0.1.144统一计入0.1.124，后续相邻蓝图细调沿用该号。修订目录和素材引用使用原版本标签，见[旧新对照](../../美术连续修订版本合并说明.md)。

2026-10-31，版本0.1.124（连续美术修订合并基线）。两张16×16透明背景图已接入物品模型，源文件保留Aseprite分层，视觉效果待确认。

![两类蓝图与堆叠数量预览](preview-centered.png)

| 物品 | 图案 | 可编辑文件 | 游戏资源 |
|---|---|---|---|
| 舰船建造图纸 | 蓝纸中央的简化银白舵轮 | [ship_blueprint.aseprite](sources/ship_blueprint.aseprite) | [ship_blueprint.png](../../../src/main/resources/assets/piranport/textures/item/ship_blueprint.png) |
| 装备开发图纸 | 蓝纸中央的铜色炮弹 | [equipment_blueprint.aseprite](sources/equipment_blueprint.aseprite) | [equipment_blueprint.png](../../../src/main/resources/assets/piranport/textures/item/equipment_blueprint.png) |

两图均以原生16×16像素绘制。共用原版纸张的斜放轮廓，纸边收窄到约一像素，只保留两处纸白高光。淡色网格分别平行于纸页的长、短边，不随画布横竖排列；明、暗纸面分别使用接近底色的线色。

两种图案移至纸面中央略偏上，给右下角的堆叠数字留出空间。舵轮保留原0.1.136归档的7×7形状，零起点左上坐标为5、3；四个握柄和内孔原样保留，无额外外描边。

炮弹从10像素高缩至7像素高，实际外接框4×7，零起点左上坐标为7、3。两种图案采用相同的7×7布局范围，均为21个非透明像素；炮弹保留细长轮廓，不横向拉宽。轮体现在全部落在纸面内，恢复原始银白色阶。

源文件包含`Paper`与`Emblem`两层，纸面层未改动。舰船图使用10种可见颜色，装备图使用11种。透明度仅为0或255，无抗锯齿或半透明边；像素规格和底纹线段见[design-spec.json](design-spec.json)。

移动前的同版本素材、规格和源文件存于[修订归档](revisions/before-centered/README.md)。本次沿用0.1.124，不新增版本号或交付计数。

## 参考来源

- 舰R v5.5.1本地拆包`assets/data/ui/common.unity3d`中的`common_source_7`与`common_source_8`，分别参考“蓝纸＋舵轮”和“蓝纸＋炮弹”。原图为46×46，符号按16×16重新绘制。
- 原版Minecraft1.21.1的[item/paper.png](E:/Leisure/Coding/Minecraft/mc-refs/minecraft-1.21.1/assets/minecraft/textures/item/paper.png)，沿用其轮廓并减少白色纸边。
- 原始包路径、Texture2D标识与参考哈希见[来源索引](references/sources.json)，对照见[参考图](reference-contact.png)。

## 验证

两图由实时Aseprite导出，源文件与生产PNG一致。Aseprite校验结果见[aseprite-validation.json](aseprite-validation.json)；两种底纹线色刻意接近纸面底色，用于降低网格对比度。

文件尺寸、逐像素规格和发行包核对见[validation.json](validation.json)。构建通过；与同版本合并基线相比，发行包只改变两张蓝图PNG。

预览使用最近邻整数放大，并以原版1.21.1的`ascii.png`字形，按`GuiGraphics.renderItemDecorations`的坐标叠加1、16、64张数量。两种图案的主体仍可辨；预览为静态拼版，本轮未启动游戏。

此前32×32初版存于[历史归档](revisions/32px-v1/README.md)。
