# 船坞舰种联动实际截图

2026-10-26，0.1.110。独立新建平坦测试世界，默认字体，1280×900窗口。搜索弹窗348×210，舰种与国籍各一行九项，共用横坐标和按钮宽度；主界面布局保持原坐标。

| 内容 | 实际截图 |
|---|---|
| 选择小型自动选中重炮、驱逐、潜艇 | [中文分组](shipyard-filter-dialog.png) |
| 当前实际筛选条件 | [摘要悬浮](shipyard-filter-summary.png) |
| 手动选齐BB、BC、CV后大型自动高亮 | [英文分组](shipyard-filter-english.png) |
| CVL悬浮显示完整舰种名 | [完整名称](shipyard-class-full-name.png) |

英文舰种依次采用BB、BC、CV、CA、CVL、CL、BM、DD、SS，依据[舰R官方说明](https://www.jianniang.com/2017-01-04/1483505333d372.html)。船型控制对应舰种整组，部分选择不高亮船型，但保留实际匹配结果；再次点击完整组则移除该组，其他组选择继续保留。

64项GUI检查通过，包含三组双向联动、跨组部分选择、重开恢复、实际网络应用、九列对齐及英文缩写／悬浮。报告见[验证记录](../../../build/shipyard-class-coupling-20261026/validation.json)。本轮仅编译219项GameTest，未运行专用服务器或其他设施GUI；其他字体、缩放、真实多人及第三方接口继续保留人工范围。
