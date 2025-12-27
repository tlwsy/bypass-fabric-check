# Bypass Fabric Check

![License](https://img.shields.io/badge/License-GPLv3-blue.svg) ![Fabric](https://img.shields.io/badge/Loader-Fabric-green) ![MC](https://img.shields.io/badge/Minecraft-1.21%2B-lightgrey)

## 简介 / Introduction
这是一个简单的 Minecraft Fabric 模组，旨在解决 **1.21+** 版本中，原版客户端 (Vanilla Client) 无法加入安装了 Fabric API 的局域网 (LAN) 或服务端的问题。

This is a simple Fabric mod designed to solve the issue where Vanilla Clients cannot join a LAN/Server with Fabric API installed in Minecraft 1.21+.

## 功能 / Features
* **绕过握手检查 (Bypass Handshake)**: 拦截服务端向非本地玩家发送的 `FabricConfigurationTask`。
* **原版兼容 (Vanilla Compatibility)**: 允许没有任何模组的朋友直接连接你的 Fabric 主机。
* **服务端专用 (Server-Side Only)**: 只需要安装在 Host/服务端，客户端无需安装。

## 解决了什么报错？ / Fixes Error
> This server requires Fabric Loader and Fabric API installed on your client!
