# Bypass Fabric Check

![License](https://img.shields.io/badge/License-GPLv3-blue.svg) ![Fabric](https://img.shields.io/badge/Loader-Fabric-green) ![MC](https://img.shields.io/badge/Minecraft-1.21%2B-lightgrey)

## 简介 / Introduction
这是一个简单的 Minecraft Fabric 模组，旨在解决 **1.21+** 版本中，原版客户端 (Vanilla Client) 无法加入安装了 Fabric API 的局域网 (LAN) 或服务端的问题。

This is a simple Fabric mod designed to solve the issue where Vanilla Clients cannot join a LAN/Server with Fabric API installed in Minecraft 1.21+.

## Minecraft 26.3
当前版本 **1.0.11** 使用 Minecraft **26.3** 构建，需要 **Java 25+**、**Fabric Loader 0.19.3+** 和适用于 26.3 的 **Fabric API**（构建使用 `0.162.0+26.3`）。将模组和 Fabric API 放入 Host/服务端的 `mods` 文件夹。

Version **1.0.11** builds against Minecraft **26.3** and requires **Java 25+**, **Fabric Loader 0.19.3+**, and **Fabric API for 26.3** (built with `0.162.0+26.3`). Install the mod and Fabric API in the host/server's `mods` folder.

最低 Loader 版本采用 0.19.3，以满足适用于 26.3 的 Fabric API 的依赖要求，并支持本模组使用的 Java 25 Mixin 兼容级别；构建也使用此最低版本。

The minimum Loader version is 0.19.3, as required by Fabric API for 26.3; it also supports the mod's Java 25 Mixin compatibility level. The build also uses this minimum version.

使用 Java 25 运行 `bash ./gradlew build`（Windows：`gradlew.bat build`），生成的模组位于 `build/libs/bypass-fabric-check-1.0.11.jar`。

Build with Java 25 using `bash ./gradlew build` (Windows: `gradlew.bat build`). The mod jar is generated at `build/libs/bypass-fabric-check-1.0.11.jar`.

## 功能 / Features
* **绕过握手检查 (Bypass Handshake)**: 拦截服务端向非本地玩家发送的 `FabricConfigurationTask`。
* **原版兼容 (Vanilla Compatibility)**: 允许没有任何模组的朋友直接连接你的 Fabric 主机。
* **服务端专用 (Server-Side Only)**: 只需要安装在 Host/服务端，客户端无需安装。

## 解决了什么报错？ / Fixes Error
> This server requires Fabric Loader and Fabric API installed on your client!
