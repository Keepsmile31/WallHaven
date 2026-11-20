<div align="center">

<img src="docs/logo.svg" width="108" height="108" alt="WallHaven Logo" />

# WallHaven

**纯粹、原画质、现代化的第三方 WallHaven.cc Android 原生客户端**

<p>
  <a href="https://github.com/Keepsmile31/WallHaven/releases/latest"><img src="https://img.shields.io/github/v/release/Keepsmile31/WallHaven?style=flat-square&logo=github&color=FF6B35&label=Release" alt="Release" /></a>
  <a href="https://github.com/Keepsmile31/WallHaven/releases"><img src="https://img.shields.io/badge/Version-v2.6.3%20(27)-2E7D32?style=flat-square&logo=semanticrelease" alt="Version" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-14%2B%20(API%2034%2B)-3DDC84?style=flat-square&logo=android" alt="Android 14+" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin" alt="Kotlin" /></a>
  <a href="https://developer.android.com/compose"><img src="https://img.shields.io/badge/Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose" /></a>
  <a href="https://t.me/WallHaven8"><img src="https://img.shields.io/badge/Telegram-%40WallHaven8-26A5E4?style=flat-square&logo=telegram" alt="Telegram" /></a>
  <a href="https://github.com/Keepsmile31/WallHaven/stargazers"><img src="https://img.shields.io/github/stars/Keepsmile31/WallHaven?style=flat-square&logo=github&color=EAB308&label=Stars" alt="Stars" /></a>
</p>

<p>
  <span><img src="docs/icons/features.svg" width="18" height="18" align="absmiddle" alt="" />&nbsp;<a href="#features">核心功能</a></span> &nbsp;&nbsp;
  <span><img src="docs/icons/palette.svg" width="18" height="18" align="absmiddle" alt="" />&nbsp;<a href="#design">视觉设计</a></span> &nbsp;&nbsp;
  <span><img src="docs/icons/download.svg" width="18" height="18" align="absmiddle" alt="" />&nbsp;<a href="#downloads">下载安装</a></span> &nbsp;&nbsp;
  <span><img src="docs/icons/layers.svg" width="18" height="18" align="absmiddle" alt="" />&nbsp;<a href="#tech-stack">技术栈</a></span> &nbsp;&nbsp;
  <span><img src="docs/icons/policy.svg" width="18" height="18" align="absmiddle" alt="" />&nbsp;<a href="#disclaimer">免责声明</a></span>
</p>

</div>

---

> [!NOTE]
> **设计初衷**：基于 **Kotlin** 与 **Jetpack Compose（Material Design 3）** 从零构建，直连 [WallHaven.cc](https://wallhaven.cc) 开放 API，**不对任何壁纸进行压缩、裁切或画质劣化**，完整保留原图细节。

<a id="features"></a>
## <img src="docs/icons/features.svg" width="24" height="24" align="absmiddle" alt="" /> 核心功能

| 功能模块 | 特性说明 |
| :--- | :--- |
| **发现探索** | 支持 **最新 / 热门 / 随机 / 榜单** 四大分栏切换；单击已选标签平滑回顶，双击「随机」即时刷新壁纸流 |
| **精细筛选** | 支持风格题材、宽高比、分辨率门槛、主色调、排序、内容类别（常规 / 动漫 / 人物）与纯度分级（一般 / 粗糙 / 限制级），支持 **全局共享** 与 **分页独立** |
| **双语检索** | 内置 Room FTS4 全文检索与中英文标签词典，支持实时标签联想、色板按色寻图及详情页标签 / 色块下钻 |
| **高清看图** | 基于 `BitmapRegionDecoder` 实现多层级分块解码，支持 1080P 至 8K 原图无损缩放、双击放大与下拉退出 |
| **下载管理** | 支持多线程并发下载原图、一键设为 **桌面 / 锁屏 / 双屏** 壁纸，以及收藏与浏览 / 下载 / 搜索历史批量管理 |
| **同步更新** | 支持本地 JSON 导入导出、**WebDAV 每日自动备份**，启动静默检测 GitHub 新版本；内置调试日志导出工具 |

<a id="design"></a>
## <img src="docs/icons/palette.svg" width="24" height="24" align="absmiddle" alt="" /> 视觉设计

| 设计维度 | 特性说明 |
| :--- | :--- |
| **动态色彩** | 支持 **Material You 动态取色**，内置 **9 款主题色彩** 与 **4 种配色风格**（经典活力 / 鲜明大胆 / 柔和素雅 / 高对比度），支持跟随系统 / 浅色 / 深色与 **AMOLED 纯黑** |
| **液态玻璃** | 基于 Android `RuntimeShader` 提供 **无 / 标准 / 极致** 三档 AGSL 折射质感，覆盖悬浮胶囊底栏、顶部分段标签与玻璃态开关 |
| **沉浸交互** | 全面适配 Edge-to-Edge 全面屏，支持滚动自动隐藏导航栏、触感震动反馈与定时自动清理缓存 |

<a id="downloads"></a>
## <img src="docs/icons/download.svg" width="24" height="24" align="absmiddle" alt="" /> 下载安装

> [!IMPORTANT]
> **运行环境**：Android 14+（API 34+） · `arm64-v8a` · 当前版本 `v2.6.3 (27)`

| 下载渠道 | 获取地址与说明 |
| :--- | :--- |
| <img src="docs/icons/github.svg" width="20" height="20" align="absmiddle" alt="" /> **GitHub** | [前往下载最新版本](https://github.com/Keepsmile31/WallHaven/releases/latest) — 官方源码构建发布，推荐首选 |
| <img src="docs/icons/123pan.svg" width="20" height="20" align="absmiddle" alt="" /> **123&nbsp;网盘** | [点击访问网盘](https://1815318986.share.123pan.cn/123pan/ngLtVv-8xfm3) — 国内高速分流下载镜像 |
| <img src="docs/icons/telegram.svg" width="20" height="20" align="absmiddle" alt="" /> **Telegram** | [加入 @WallHaven8](https://t.me/WallHaven8) — 版本更新推送与交流反馈 |

<details>
<summary><img src="docs/icons/key.svg" width="18" height="18" align="absmiddle" alt="" /> <strong>API Key 与隐私说明（点击展开）</strong></summary>

- **内容分级访问**：未填写个人 API Key 时可正常浏览 **一般（SFW）** 与 **粗糙（Sketchy）** 壁纸；在 **设置 → API Key** 填入密钥后，即可解锁 **限制级（NSFW）** 资源检索并提升请求限额。
- **本地存储保障**：API Key 与 WebDAV 账号凭据仅保存于设备本地应用私有目录（`DataStore`）中，除向您配置的目标服务端发起必要请求外，不会上传至任何第三方服务器。

</details>

<a id="tech-stack"></a>
## <img src="docs/icons/layers.svg" width="24" height="24" align="absmiddle" alt="" /> 技术栈

| 技术领域 | 核心选型 |
| :--- | :--- |
| **架构界面** | Kotlin `2.2.10` · Jetpack Compose · Material Design 3 · MVVM · AGSL `RuntimeShader` |
| **网络图像** | Retrofit `2.12.0` · OkHttp `4.12.0` · Moshi `1.15.2` · Coil `2.7.0` · `BitmapRegionDecoder` |
| **存储同步** | Room `2.7.0`（FTS4）· DataStore `1.1.7` · 自研 WebDAV 客户端 |

<a id="disclaimer"></a>
## <img src="docs/icons/policy.svg" width="24" height="24" align="absmiddle" alt="" /> 免责声明

1. **非官方客户端**：本应用为基于 [WallHaven.cc](https://wallhaven.cc) 开放 API 开发的第三方开源客户端，与 WallHaven 官方无任何从属或合作关系。
2. **版权归属声明**：客户端未对壁纸图片做任何压缩或二次篡改，所有壁纸作品的版权及使用权限均归 **WallHaven.cc** 及原作者所有。
3. **合规使用与反馈**：请遵守当地法律法规与 WallHaven 服务条款，切勿用于恶意爬取或商业滥用；如遇异常或建议，欢迎提交 [GitHub Issues](https://github.com/Keepsmile31/WallHaven/issues)。

---

<div align="center">

<p>Built with <img src="docs/icons/favorite.svg" width="18" height="18" align="absmiddle" alt="love" /> in <strong>Kotlin</strong> &amp; <strong>Compose</strong></p>

<p>如果这个项目对你有帮助，欢迎点亮 <img src="docs/icons/star.svg" width="18" height="18" align="absmiddle" alt="Star" /> <strong>Star</strong></p>

</div>
