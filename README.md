<div align="center">

<img src="docs/logo.svg" width="112" height="112" alt="WallHaven Logo" />

# WallHaven

**纯粹、原画质、极具现代感的第三方 WallHaven.cc Android 原生客户端**

*Built with Kotlin · Jetpack Compose · Material Design 3 · AGSL Liquid Glass*

<p>
  <a href="https://github.com/Keepsmile31/WallHaven/releases/latest">
    <img src="https://img.shields.io/github/v/release/Keepsmile31/WallHaven?style=flat-square&logo=github&color=FF6B35&label=Release" alt="Release" />
  </a>
  <a href="https://github.com/Keepsmile31/WallHaven/releases">
    <img src="https://img.shields.io/badge/Version-v2.6.3%20(27)-2E7D32?style=flat-square&logo=android&logoColor=white" alt="Version" />
  </a>
  <a href="https://developer.android.com">
    <img src="https://img.shields.io/badge/Android-14%2B%20(API%2034%2B)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 14+" />
  </a>
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  </a>
  <a href="https://developer.android.com/compose">
    <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  </a>
</p>

<p>
  <a href="https://t.me/WallHaven8">
    <img src="https://img.shields.io/badge/Telegram-%40WallHaven8-26A5E4?style=flat-square&logo=telegram&logoColor=white" alt="Telegram" />
  </a>
  <a href="https://github.com/Keepsmile31/WallHaven/stargazers">
    <img src="https://img.shields.io/github/stars/Keepsmile31/WallHaven?style=flat-square&logo=github&color=EAB308&label=Stars" alt="GitHub Stars" />
  </a>
  <a href="https://github.com/Keepsmile31/WallHaven/releases">
    <img src="https://img.shields.io/github/downloads/Keepsmile31/WallHaven/total?style=flat-square&logo=icloud&color=0EA5E9&label=Downloads" alt="GitHub Downloads" />
  </a>
  <a href="https://github.com/Keepsmile31/WallHaven/issues">
    <img src="https://img.shields.io/github/issues/Keepsmile31/WallHaven?style=flat-square&color=8B5CF6&label=Issues" alt="GitHub Issues" />
  </a>
</p>

<p>
  <a href="#intro">📖 项目简介</a> ·
  <a href="#features">✨ 核心功能</a> ·
  <a href="#ui-design">🎨 视觉设计</a> ·
  <a href="#downloads">📥 下载安装</a> ·
  <a href="#tech-stack">🛠️ 技术架构</a> ·
  <a href="#build">📦 本地构建</a> ·
  <a href="#disclaimer">⚠️ 免责声明</a>
</p>

</div>

---

<a id="intro"></a>
## 📖 项目简介

**[WallHaven.cc](https://wallhaven.cc)** 是全球最受欢迎的高质量免费壁纸社区之一，但原生缺乏移动端 App，在手机上浏览、按标签检索及保存无损原图均不够顺手。

本项目基于 **Kotlin + Jetpack Compose + Material Design 3** 从零原生构建，致力提供流畅跟手、克制纯粹的手机端壁纸探索、收藏与管理体验。

> 💡 **原图零妥协**：客户端直连 WallHaven.cc 官方开放 API，**绝不对任何壁纸进行压缩、裁剪或二次画质劣化**，完整保留从 1080P 到 8K 超高分辨率的每一处原图细节。

---

<a id="features"></a>
## ✨ 核心功能

| 模块 | 核心特性说明 |
| :--- | :--- |
| 🧭 **多维探索流** | 支持 **最新 / 热门 / 随机 / 榜单** 四大分栏水平滑动与顶栏切换；单击当前已选中标签平滑滚回顶部，**双击「随机」标签即时刷新一批壁纸** |
| 🎛️ **精细化筛选器** | 支持风格场景、宽高比、最低分辨率、指定分辨率、主色调、排序方式、内容类别（常规 / 动漫 / 人物）与纯度分级（SFW / Sketchy / NSFW）；支持 **全局共享** 与 **分页独立** 两种筛选作用域 |
| 🔍 **双语搜索与词库** | 内置 Room FTS4 全文检索与中英文标签词典，支持关键词自动翻译联想、色板按色寻图、详情页点击标签/色块无缝下钻及返回栈定位还原 |
| 🖼️ **超分分块看图** | 基于 `BitmapRegionDecoder` 实现多层级分块高清解码，**1080P 至 8K（如 `7717×4341`）超分原图** 1:1 放大依然锐利清晰；支持双指捏合、双击局部缩放与下拉手势退出 |
| ❤️ **收藏与历史管理** | 本地持久化收藏夹、浏览历史、下载历史与搜索记录；支持长按/多选进入批量模式，**一键批量下载、批量取消收藏或批量清理记录** |
| ⬇️ **原图下载与设壁纸** | 支持自定义本地下载目录与并发队列数（默认 3 并发），带系统通知进度；详情页支持一键设置为 **桌面壁纸 / 锁屏壁纸 / 桌面与锁屏** |
| ☁️ **本地备份与 WebDAV** | 支持完整 JSON 备份导出与导入（应用设置、收藏夹、浏览与下载历史、搜索记录），并内建 **WebDAV 每日自动同步与手动上传/拉取** |
| 🔔 **启动自动查更** | 冷启动异步静默查询 GitHub Releases 最新版本，弹窗展示更新日志，支持 **立即更新 / 提醒我 / 忽略此版本**（后续发布新版本自动恢复提醒） |
| 🐞 **本地调试日志** | 自动捕获未处理异常与网络请求报错，支持在设置页随时查看、一键导出或清空日志，方便反馈排查 |

<details>
<summary>⌨️ <b>高频手势与效率技巧速查（点击展开）</b></summary>

| 场景 | 操作方式 | 效果 |
| :--- | :--- | :--- |
| 探索页顶栏 | 单击**非当前**标签 | 切换至对应分栏并加载数据 |
| 探索页顶栏 | 单击**当前已选中**标签 | 平滑滚动返回列表顶部（Index 0） |
| 探索页顶栏 | **双击「随机」**标签 | 触发新一轮随机壁纸刷新 |
| 全屏看图 | 双击画面任意位置 | 在点击坐标处快速放大 / 还原 |
| 全屏看图 | 双指捏合 / 拖动 | 自由缩放与平移查看 1:1 像素细节 |
| 全屏看图 | 单击画面 / 下拉滑动 | 显隐操作浮层 / 顺滑手势关闭全屏 |
| 列表与记录 | 长按卡片或点击多选按钮 | 进入批量选择模式（批量下载 / 移出 / 删除） |

</details>

---

<a id="ui-design"></a>
## 🎨 视觉与交互设计

- 🌈 **Material You 与多维调色板**：支持 Android 原生动态取色，同时内置 **9 款精选主题色** 与 **4 种配色风格**（经典活力 / 鲜明大胆 / 柔和素雅 / 高对比度）。
- 🌙 **深色模式与 AMOLED 纯黑**：支持跟随系统 / 浅色 / 深色三态切换，并可单独开启 AMOLED 纯黑背景以兼顾 OLED 屏幕观感与续航。
- 🫧 **AGSL 液态玻璃质感**：依托 Android `RuntimeShader` 提供 **无 / 标准 / 极致** 三档液态玻璃折射效果，贯穿悬浮胶囊底部导航栏、顶部分段标签与玻璃态开关。
- 📳 **沉浸式全屏与空间控制**：全面适配 Edge-to-Edge 全面屏与手势导航条，支持 **滚动自动隐藏底栏**、**收藏数角标开关**、轻触觉震动反馈及 **缓存自动清理**（关闭 / 每 3 天 / 退出时清理）。

---

<a id="downloads"></a>
## 📥 下载安装

> 📱 **运行环境要求**：Android 14+（API 34+） · 指令集架构：`arm64-v8a` · 当前版本：`v2.6.3 (27)`

| 下载渠道 | 适用场景 | 直达链接 |
| :--- | :--- | :--- |
| 🐙 **GitHub Releases** | 官方发布源，提供最新 APK 与完整更新日志 | [前往下载最新版本](https://github.com/Keepsmile31/WallHaven/releases/latest) |
| ☁️ **123 网盘** | 国内高速分流下载镜像 | [点击访问网盘](https://1815318986.share.123pan.cn/123pan/ngLtVv-8xfm3) |
| ✈️ **Telegram 频道** | 第一时间获取版本更新推送与使用交流 | [加入 @WallHaven8](https://t.me/WallHaven8) |

<details>
<summary>🔑 <b>关于个人 API Key 与内容纯度说明（点击展开）</b></summary>

1. **无需密钥即可使用**：未填写 API Key 时，默认可流畅浏览与搜索 **SFW（全年龄）** 及 **Sketchy** 级别壁纸。
2. **解锁完整内容与高限额**：若需检索 **NSFW（限制级）** 壁纸或提升 API 请求频率上限，请前往 [WallHaven 官网账户设置](https://wallhaven.cc/settings/account) 获取个人 API Key，并在应用内 **设置 → API Key** 中保存。
3. **隐私安全**：您的 API Key 及 WebDAV 账号密码仅存储于设备本地的应用私有沙盒目录（`DataStore`）中，除向您配置的目标服务端发起必要请求外，绝不上传至任何第三方。

</details>

<details>
<summary>☁️ <b>关于 WebDAV 云同步说明（点击展开）</b></summary>

- 支持兼容标准 WebDAV 协议的网盘服务（如坚果云、Alist、Nextcloud、NAS 自建服务等）。
- 同步内容涵盖：**应用偏好设置、收藏夹列表、浏览历史、下载历史与搜索历史**。
- 可开启「每日自动备份」，亦可在切换设备时随时手动执行「立即备份」或「从云端恢复」。

</details>

---

<a id="tech-stack"></a>
## 🛠️ 技术栈与架构

项目采用清晰的 **MVVM 单向数据流架构**，结合 **Kotlin Coroutines & Flow** 驱动状态更新，核心技术栈如下：

| 类别 | 技术选型与版本 |
| :--- | :--- |
| **语言与构建** | Kotlin `2.2.10` · AGP `9.1.1` · KSP `2.3.5` · Gradle Kotlin DSL |
| **UI 与动效** | Jetpack Compose（BOM `2024.09.00`）· Material Design 3 · Navigation Compose `2.8.9` · AGSL `RuntimeShader` |
| **网络与序列化** | Retrofit `2.12.0` · OkHttp `4.12.0` · Moshi `1.15.2`（KSP Codegen）· Coroutines `1.10.2` |
| **图片与超分引擎** | Coil `2.7.0` · 自研 `SubsampledImageTileRenderer`（`BitmapRegionDecoder` 分块高清解码） |
| **本地持久化** | Room `2.7.0`（含 FTS4 全文检索）· DataStore Preferences `1.1.7` |
| **备份与云同步** | 自研轻量 `WebDavClient` · JSON 全量导出/导入管理器 |
| **自动化测试** | JUnit 4 · Robolectric `4.16.1` · Coroutines Test · Compose UI Test |

<details>
<summary>📂 <b>工程核心目录概览（点击展开）</b></summary>

```text
app/src/main/java/com/example/
├── data/
│   ├── api/            # WallHaven & GitHub Releases Retrofit 接口定义
│   ├── backup/         # 本地 JSON 备份导出/导入逻辑
│   ├── db/             # Room 数据库、DAO、Entity 与 FTS4 标签检索表
│   ├── model/          # 网络响应与领域数据模型
│   ├── preferences/    # DataStore 偏好设置与自动更新记录
│   ├── repository/     # 壁纸数据、更新检测与标签中英翻译词库仓库
│   └── webdav/         # 轻量 WebDAV 客户端实现
├── ui/
│   ├── components/     # 液态玻璃组件、分块全屏看图器、筛选弹窗、详情页与更新弹窗
│   ├── screens/        # 探索、搜索、收藏、历史与多级设置页面
│   ├── theme/          # Material 3 色彩系统、排版与动态主题引擎
│   └── MainViewModel.kt
└── util/               # 下载队列管理、壁纸设置、缓存清理、震动与异常日志管理
```

</details>

---

<a id="build"></a>
## 📦 本地构建

### 环境准备

- **Android Studio**：建议 Ladybug / Meerkat 或更高版本
- **JDK**：JDK 17 或 JDK 21
- **Android SDK**：Compile SDK `37`（`minSdk 34` / `targetSdk 37`）

### 构建步骤

1. **克隆源码仓库**
   ```bash
   git clone https://github.com/Keepsmile31/WallHaven.git
   cd WallHaven
   ```
2. **编译 Debug 安装包**
   ```bash
   ./gradlew assembleDebug
   # 若本地未包含 gradlew，可使用已安装的 Gradle：
   # gradle assembleDebug
   ```
3. **运行本地单元测试与 Robolectric 验证**
   ```bash
   ./gradlew testDebugUnitTest
   # 或：gradle :app:testDebugUnitTest
   ```

---

## 💬 交流与反馈

- 🐛 **问题反馈与功能建议**：欢迎前往 [GitHub Issues](https://github.com/Keepsmile31/WallHaven/issues) 提交反馈。若遇到闪退或网络异常，可在应用 **设置 → 调试日志** 中导出日志并随 Issue 附上。
- 📢 **社区与版本更新通知**：欢迎加入 Telegram 频道 [@WallHaven8](https://t.me/WallHaven8) 获取最新构建动态。

---

<a id="disclaimer"></a>
## ⚠️ 免责声明

1. **非官方第三方客户端**：本项目基于 [WallHaven.cc](https://wallhaven.cc) 开放 API 开发，与 WallHaven 官方无任何从属、授权或合作关系。
2. **版权归属说明**：本客户端未对 WallHaven.cc 返回的壁纸资源进行画质压缩、裁切或篡改；所有壁纸作品的版权及使用许可均归 **WallHaven.cc** 及原作者所有。
3. **合规使用约定**：请自觉遵守当地法律法规及 WallHaven 平台服务条款，**切勿将本客户端或 API 接口用于恶意爬取、商业滥用或其他违规用途**。

---

<div align="center">

Made with ❤️ using **Kotlin** & **Jetpack Compose**

如果这个项目对你有帮助，欢迎点亮一颗 ⭐ **Star** 支持！

</div>
