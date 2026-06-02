# 界面设计方向

## 1. 总体风格

推荐采用“Editorial Portfolio + Product Dashboard”的混合风格：

- 首页和项目详情页像高级作品集，有强排版和沉浸式叙事。
- 模块实验室和后台管理像成熟产品控制台，重视密度、效率和可信度。
- 技术架构页像工程文档平台，信息清楚、有图、有证据。

不要使用泛滥的紫蓝渐变 AI 风格。整体应更接近：

- 黑白灰 + 单一强调色
- 高对比排版
- 细线框、低圆角、轻阴影
- 真实截图、架构图、代码片段
- 有节制的动效

## 2. 页面信息架构

```text
/
  首页

/resume
  在线简历

/experience
  实习与经历时间线

/projects
  项目列表

/projects/[slug]
  项目详情

/lab
  模块实验室

/lab/rbac
  权限系统 Demo

/lab/dashboard
  数据看板 Demo

/architecture
  技术架构与部署

/admin
  后台管理
```

## 3. 首页首屏结构

首屏目标：清楚、有记忆点、能马上进入作品。

布局建议：

- 顶部导航：Home / Resume / Projects / Lab / Architecture / Contact
- 左侧：姓名、岗位定位、简短描述、CTA
- 右侧：动态能力面板，不放抽象插画
- 底部：技术标签和当前状态

右侧动态面板可以做成：

- 最近项目状态卡
- 技术栈雷达/矩阵
- 代码提交活动
- 可点击命令面板预览
- 当前求职方向卡片

## 4. 项目详情页结构

项目页要像一份“面试讲解稿”，但视觉上不能像文档。

建议结构：

```text
项目标题区
  项目名 / 角色 / 时间 / 技术栈 / 在线演示 / GitHub

项目概览
  背景 / 目标 / 我的贡献 / 结果

架构展示
  前端结构 / 后端结构 / 数据流 / 部署结构

核心模块
  每个模块一张功能卡 + 交互演示入口

难点复盘
  问题 / 方案 / 权衡 / 效果

代码与接口
  关键代码片段 / API 示例 / 数据模型

总结
  收获 / 可改进点 / 面试讲解提示
```

## 5. 模块实验室设计

模块实验室是差异化亮点，不能只是列表。

首页：

- 左侧模块目录
- 右侧模块预览
- 每个模块显示技术点、业务价值、可演示内容

RBAC Demo：

- 用户列表
- 角色列表
- 菜单权限树
- 按钮权限开关
- 模拟不同角色登录后的页面变化

数据看板 Demo：

- 指标卡
- 折线图/柱状图
- 筛选器
- 表格联动
- 空状态、加载状态、错误状态

文件上传 Demo：

- 拖拽上传
- 进度条
- 上传队列
- 暂停/继续
- 技术说明面板

## 6. 后台管理界面

后台管理不要做得花哨，要体现“真实系统”。

建议布局：

- 顶部：搜索、通知、用户菜单
- 左侧：导航菜单
- 主区：表格、表单、详情抽屉
- 支持暗色模式，但默认可以是浅色

模块：

- Dashboard
- Project Management
- Experience Management
- Article Management
- Resume Management
- Analytics
- System Settings

## 7. 视觉组件清单

基础组件：

- Button
- Input
- Textarea
- Select
- Tabs
- Dialog
- Drawer
- Tooltip
- Badge
- Card
- Table
- Command Palette
- Timeline
- Code Block
- Stat Card
- Chart Card

展示组件：

- Project Showcase Card
- Experience Timeline Item
- Skill Matrix
- Architecture Diagram
- Module Demo Shell
- Case Study Section
- Interview Talking Points

## 8. 动效原则

适合使用：

- 页面进入时的轻量 reveal
- 项目卡片 hover 抬升/描边
- 时间线滚动激活
- 命令面板打开/关闭
- 模块切换过渡
- 图表数据加载动画

避免：

- 大面积连续闪烁
- 无意义漂浮元素
- 过度 3D
- 影响阅读的滚动劫持

## 9. 可参考的开源方向

参考不是直接照搬，而是借鉴结构和技术组合：

- Next.js + Tailwind + Framer Motion 的作品集结构。
- shadcn/ui 的组件体系和后台质感。
- MDX 驱动项目内容，降低后期维护成本。
- 3D 或 Canvas 仅作为局部增强，不作为主视觉依赖。

最终目标是形成自己的识别度：

- 页面高级但不空。
- 内容真实且可演示。
- 工程链路完整。
- 面试时每个页面都有话可讲。
