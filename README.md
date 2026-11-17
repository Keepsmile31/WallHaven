<div align="center">

<img src="docs/logo.svg" width="116" height="116" alt="WallHaven Logo" />

# WallHaven

**基于 Kotlin 与 Jetpack Compose 构建的第三方 WallHaven.cc 原图高清壁纸客户端**

[![Release](https://img.shields.io/github/v/release/Keepsmile31/WallHaven?style=flat-square&logo=github&color=FF6B35&label=Release)](https://github.com/Keepsmile31/WallHaven/releases/latest)
[![Version](https://img.shields.io/badge/Version-v2.6.3%20(27)-2E7D32?style=flat-square&logo=android&logoColor=white)](https://github.com/Keepsmile31/WallHaven/releases)
[![Android](https://img.shields.io/badge/Android-14%2B%20(API%2034%2B)-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Telegram](https://img.shields.io/badge/Telegram-WallHaven8-26A5E4?style=flat-square&logo=telegram&logoColor=white)](https://t.me/WallHaven8)
[![Stars](https://img.shields.io/github/stars/Keepsmile31/WallHaven?style=flat-square&color=EAB308)](https://github.com/Keepsmile31/WallHaven/stargazers)
[![Downloads](https://img.shields.io/github/downloads/Keepsmile31/WallHaven/total?style=flat-square&color=0EA5E9)](https://github.com/Keepsmile31/WallHaven/releases)

[✨ 功能特性](#-核心功能) · [📥 下载安装](#-下载安装) · [🎨 视觉与交互](#-视觉与交互设计) · [🛠️ 技术栈](#️-技术栈与架构) · [⚠️ 免责声明](#️-免责声明)

</div>

---

## 📖 项目简介

**WallHaven.cc** 是一个广受欢迎的世界级免费高清壁纸网站，但原生仅有网页端，在移动端浏览、筛选与保存原图时体验并不理想。本项目应运而生——采用纯原生 **Kotlin + Jetpack Compose + Material Design 3** 打造，提供流畅顺滑的移动端壁纸探索体验。

> 💡 **原图坚持**：客户端直接依托 WallHaven.cc 开放 API 获取内容，**不对任何壁纸图片进行压缩、裁切或画质劣化**，只为还原每一张壁纸的原始细节。

---

## ✨ 核心功能

| 模块 | 特性亮点 |
| :--- | :--- |
| 🧭 **多维探索流** | 支持 **最新 / 热门 / 随机 / 榜单** 四大分栏滑动与点击切换；单击当前选中标签平滑返回顶部，**双击「随机」标签即时刷新壁纸流** |
| 🎛️ **精细化筛选** | 支持风格场景、宽高比、最低分辨率门槛、指定分辨率、主色调、排序方式、内容类别与纯度分级；支持 **全局共享** 与 **分页独立** 两种筛选模式 |
| 🔍 **智能搜索与中文词库** | 内置 Room FTS 全文检索与中英文标签翻译词典，支持实时标签联想、色板色彩检索、搜索历史管理与批量清理 |
| 🖼️ **超高清全屏看图** | 基于 `BitmapRegionDecoder` 实现分块高清渲染，支持 **1080P 至 8K 超分原图** 1:1 无损放大细节；支持双指捏合、双击局部放大与下拉手势关闭 |
| ⬇️ **原图下载与壁纸设置** | 支持可配置并发数的原图下载队列、进度提示与系统相册保存；支持一键设置为 **桌面壁纸 / 锁屏壁纸 / 桌面与锁屏** |
| ☁️ **数据备份与云同步** | 支持本地 JSON 一键导出/导入（设置、收藏、浏览与下载历史、搜索记录），并支持 **WebDAV 每日自动同步与手动备份** |
| 🔔 **自动更新提醒** | 应用冷启动自动静默检查 GitHub Releases 最新版本，支持 **立即更新 / 以后再说 / 不再提示**（新版本发布后自动恢复提醒） |
| 🐞 **本地调试日志** | 自动捕获全局异常与网络错误日志，支持一键导出分享与清空，便于快速定位问题 |

---

## 🎨 视觉与交互设计

- 🌈 **Material You 动态取色**：支持 Android 原生动态取色，同时内置 **9 款精选主题色** 与 **4 种配色风格**（经典活力 / 鲜明大胆 / 柔和素雅 / 高对比度）。
- 🌙 **深色模式与 AMOLED 纯黑**：支持跟随系统 / 浅色 / 深色模式切换，提供省电纯粹的 AMOLED 纯黑底色选项。
- 🫧 **AGSL 液态玻璃质感**：支持 **无 / 标准 / 极致** 三档液态玻璃折射效果，贯穿悬浮胶囊底部导航栏、顶部分段标签与开关控件。
- 📳 **细腻触感与沉浸布局**：全面适配 Edge-to-Edge 全面屏、挖孔屏与滚动自动隐藏导航栏，搭配轻量震动反馈提升操作手感。

---

## 📥 下载安装

> 📱 **系统要求**：Android 14+（API 34+） · 架构：`arm64-v8a`

| 下载渠道 | 说明 | 链接 |
| :--- | :--- | :--- |
| 🐙 **GitHub Releases** | 官方源码构建发布，推荐首选 | [前往下载最新版本](https://github.com/Keepsmile31/WallHaven/releases/latest) |
| ☁️ **123 网盘** | 国内高速分流下载通道 | [点击访问网盘](https://1815318986.share.123pan.cn/123pan/ngLtVv-8xfm3) |
| ✈️ **Telegram 频道** | 版本推送、更新动态与交流反馈 | [加入 @WallHaven8](https://t.me/WallHaven8) |

<details>
<summary>🔑 关于个人 API Key 说明（点击展开）</summary>

- 未配置 API Key 时，可正常浏览 **SFW（全年龄）** 与 **Sketchy** 壁纸内容。
- 如需检索 **NSFW（限制级）** 资源或提高 API 请求限额，请在应用内 **设置 → API Key** 中填入您的 WallHaven 个人密钥（可前往 WallHaven 官网账户页获取）。
- 您的 API Key 与 WebDAV 凭据仅安全存储于本地设备中，不会上传至任何第三方服务器。

</details>

---

## 🛠️ 技术栈与架构

本项目采用 **MVVM** 架构与 **Kotlin Coroutines / Flow** 进行响应式状态管理，核心依赖如下：

| 类别 | 技术选型 |
| :--- | :--- |
| **语言与构建** | Kotlin `2.2.10` · AGP `9.1.1` · KSP · Gradle Kotlin DSL |
| **UI 框架** | Jetpack Compose · Material Design 3 · AGSL RuntimeShader |
| **网络与序列化** | Retrofit `2.12.0` · OkHttp `4.12.0` · Moshi `1.15.2` |
| **图片加载与超分** | Coil `2.7.0` · `BitmapRegionDecoder` 分块高清解码 |
| **本地存储** | Room `2.7.0`（含 FTS 检索） · DataStore Preferences `1.1.7` |
| **云同步** | 自研轻量 WebDAV 客户端 + JSON 数据迁移备份 |
| **测试框架** | JUnit 4 · Robolectric `4.16.1` · Compose UI Test |

---

## 💬 交流与反馈

- 🐛 **问题反馈与建议**：欢迎提交 [GitHub Issues](https://github.com/Keepsmile31/WallHaven/issues)，如遇到崩溃或异常，建议附上设置页导出的调试日志。
- 📢 **社区与更新通知**：欢迎加入 Telegram 频道 [@WallHaven8](https://t.me/WallHaven8)。

---

## ⚠️ 免责声明

1. **非官方客户端**：本应用为基于 [WallHaven.cc](https://wallhaven.cc) 开放 API 开发的第三方开源客户端，与 WallHaven 官方无任何从属或合作关系。
2. **版权归属**：客户端未对 WallHaven.cc 提供的壁纸图片做任何压缩、裁切或二次篡改；所有壁纸图片的版权及使用权限均归 **WallHaven.cc** 及原作者所有。
3. **合理使用**：请遵守相关法律法规与 WallHaven 服务条款，**切勿将本客户端或 API 接口用于恶意爬取、商业滥用等违规用途**。

---

<div align="center">

Made with ❤️ using Kotlin & Jetpack Compose

如果这个项目对你有帮助，欢迎点亮一颗 ⭐ **Star**！

</div>
