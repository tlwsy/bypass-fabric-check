# Bypass Fabric Check

![License](https://img.shields.io/badge/License-GPLv3-blue.svg) ![Fabric](https://img.shields.io/badge/Loader-Fabric-green) ![MC](https://img.shields.io/badge/Minecraft-1.21%2B-lightgrey)
[English](#english) | [简体中文](#chinese)

---

<a name="english"></a>
## 🇬🇧 English Description

### Introduction
**Bypass Fabric Check** is a lightweight Minecraft Fabric mod designed for **Minecraft 1.21+**. It solves the issue where Vanilla clients cannot join a server or LAN world hosted with Fabric API.

It works by intercepting and canceling the `FabricConfigurationTask` sent by the server to non-local players during the Configuration Phase. This allows Vanilla clients (who do not understand this packet) to skip the handshake check and join successfully.

### Features
* **Bypass Handshake**: Automatically skips Fabric API handshake checks for remote connections.
* **Vanilla Compatibility**: Allows friends with pure Vanilla clients to join your Fabric LAN world.
* **Server-Side Only**: This mod is only required on the Host/Server. Clients do not need to install it.

### Fixes Error
> This server requires Fabric Loader and Fabric API installed on your client!

---

<a name="chinese"></a>
## 🇨🇳 简体中文介绍

### 简介
**Bypass Fabric Check** 是一个轻量级的 Minecraft Fabric 模组，适用于 **Minecraft 1.21+** 版本。旨在解决原版客户端 (Vanilla Client) 无法加入安装了 Fabric API 的局域网 (LAN) 或服务端的问题。

该模组的工作原理是：在网络协议的“配置阶段 (Configuration Phase)”，拦截服务端发送给远程玩家的 `FabricConfigurationTask` 握手包。通过跳过这一检查，原版客户端（因无法识别该数据包而被拒）即可顺利进入服务器。

### 功能特点
* **绕过握手检查**: 强制跳过对远程玩家的 Fabric 安装检查。
* **原版兼容**: 允许没有任何模组的朋友（纯原版客户端）直接连接你的 Fabric 主机。
* **服务端专用**: 只需要安装在 Host/服务端，客户端无需安装任何模组。

### 解决了什么报错？
> This server requires Fabric Loader and Fabric API installed on your client!
