<div align="center">

<img src="docs/logo.svg" width="104" height="104" alt="WallHaven Logo" />

# WallHaven

**纯粹、原画质、现代化的第三方 WallHaven.cc Android 原生客户端**

<p>
  <a href="https://github.com/Keepsmile31/WallHaven/releases/latest"><img src="https://img.shields.io/github/v/release/Keepsmile31/WallHaven?style=flat-square&logo=github&color=FF6B35&label=Release" alt="Release" /></a>
  <a href="https://github.com/Keepsmile31/WallHaven/releases"><img src="https://img.shields.io/badge/Version-v2.6.3%20(27)-2E7D32?style=flat-square&logo=android&logoColor=white" alt="Version" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-14%2B%20(API%2034%2B)-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 14+" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin%20%26%20Compose-Material%203-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin & Compose" /></a>
  <a href="https://t.me/WallHaven8"><img src="https://img.shields.io/badge/Telegram-%40WallHaven8-26A5E4?style=flat-square&logo=telegram&logoColor=white" alt="Telegram" /></a>
  <a href="https://github.com/Keepsmile31/WallHaven/stargazers"><img src="https://img.shields.io/github/stars/Keepsmile31/WallHaven?style=flat-square&color=EAB308&label=Stars" alt="Stars" /></a>
</p>

<p>
  <a href="#features">✨ 核心功能</a> ·
  <a href="#ui-design">🎨 视觉设计</a> ·
  <a href="#downloads">📥 下载安装</a> ·
  <a href="#tech-stack">🛠️ 技术栈</a> ·
  <a href="#disclaimer">⚠️ 免责声明</a>
</p>

</div>

---

> 💡 基于 **Kotlin + Jetpack Compose + Material Design 3** 构建，直连 WallHaven.cc 开放 API，**不对壁纸进行任何压缩、裁切或画质劣化**。

<a id="features"></a>
## ✨ 核心功能

| 模块 | 亮点 |
| :--- | :--- |
| 🧭 **多维探索** | 支持 **最新 / 热门 / 随机 / 榜单** 切换；单击已选标签平滑回顶，**双击「随机」即时刷新** |
| 🎛️ **筛选与检索** | 支持风格、比例、分辨率、色调、类别与纯度筛选（全局 / 分页独立）；内置 Room FTS4 与中英标签词库 |
| 🖼️ **超分看图** | 基于 `BitmapRegionDecoder` 分块高清解码，支持 1080P–8K 原图无损缩放与下拉关闭 |
| ⬇️ **下载与管理** | 支持多并发原图下载、一键设为桌面/锁屏壁纸，以及收藏与历史记录的批量管理 |
| ☁️ **同步与更新** | 支持本地 JSON 导入导出、**WebDAV 自动/手动备份**，以及冷启动静默检测 GitHub 新版本 |
| 🐞 **调试日志** | 自动记录异常与网络错误，支持查看、导出与清理 |

<a id="ui-design"></a>
## 🎨 视觉设计

- 🌈 **动态主题**：支持 Material You 动态取色、**9 款主题色**、**4 种配色风格**，以及浅色 / 深色 / **AMOLED 纯黑** 模式。
- 🫧 **AGSL 液态玻璃**：提供 **无 / 标准 / 极致** 三档折射质感，覆盖悬浮胶囊底栏、顶栏标签与开关控件。
- 📳 **沉浸体验**：全面适配 Edge-to-Edge 全面屏，支持滚动隐藏导航栏、触觉反馈与缓存自动清理。

<a id="downloads"></a>
## 📥 下载安装

> 📱 **系统要求**：Android 14+（API 34+） · `arm64-v8a` · 当前版本 `v2.6.3 (27)`

| 渠道 | 说明 | 链接 |
| :--- | :--- | :--- |
| 🐙 **GitHub Releases** | 官方发布渠道，推荐首选 | [下载最新版本](https://github.com/Keepsmile31/WallHaven/releases/latest) |
| ☁️ **123 网盘** | 国内高速分流下载 | [访问网盘](https://1815318986.share.123pan.cn/123pan/ngLtVv-8xfm3) |
| ✈️ **Telegram 频道** | 版本更新推送与交流反馈 | [加入 @WallHaven8](https://t.me/WallHaven8) |

<details>
<summary>🔑 <b>关于 API Key 与隐私说明（点击展开）</b></summary>

- 未填写 API Key 可正常浏览 **SFW** 与 **Sketchy** 内容；在 **设置 → API Key** 填入个人密钥后可解锁 **NSFW** 与更高请求限额。
- API Key 与 WebDAV 凭据仅保存在设备本地应用私有目录（`DataStore`）中，不上传至任何第三方服务器。

</details>

<a id="tech-stack"></a>
## 🛠️ 技术栈

| 类别 | 选型 |
| :--- | :--- |
| **UI 与架构** | Kotlin `2.2.10` · Jetpack Compose · Material 3 · MVVM · AGSL `RuntimeShader` |
| **网络与图片** | Retrofit `2.12.0` · OkHttp `4.12.0` · Moshi `1.15.2` · Coil `2.7.0` · `BitmapRegionDecoder` |
| **存储与同步** | Room `2.7.0`（FTS4）· DataStore `1.1.7` · 自研 WebDAV 客户端 |
| **测试** | JUnit 4 · Robolectric `4.16.1` · Compose UI Test |

<a id="disclaimer"></a>
## ⚠️ 免责声明

1. 本应用为基于 [WallHaven.cc](https://wallhaven.cc) 开放 API 的第三方开源客户端，与 WallHaven 官方无任何从属关系。
2. 客户端未对壁纸图片做任何压缩或篡改，所有壁纸版权及使用权限均归 **WallHaven.cc** 及原作者所有。
3. 请遵守当地法律法规与 WallHaven 服务条款，切勿用于恶意爬取或商业滥用；问题反馈欢迎提交 [GitHub Issues](https://github.com/Keepsmile31/WallHaven/issues)。

---

<div align="center">
  Made with ❤️ using Kotlin & Jetpack Compose · 如果对你有帮助，欢迎点亮 ⭐ <b>Star</b>
</div>
