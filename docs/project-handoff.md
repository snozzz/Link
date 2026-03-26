# Link 项目交接说明

更新时间：2026-03-26

这份文档用于在新对话里快速恢复上下文。请优先区分两类状态：

1. 已提交并推送到 GitHub `main` 的内容
2. 当前本地工作区里尚未提交的内容

## 一、仓库与环境

- 仓库：`git@github.com:snozzz/Link.git`
- 当前已提交 HEAD：`1eaf7ec`
- 当前稳定 Android 版本线：`0.1.1 / versionCode 2`
- 本地工作目录：`/home/snoz/sth_play/link`
- Windows Android SDK：`C:\Users\ROG\AppData\Local\Android\Sdk`
- Windows adb：`C:\Users\ROG\adb.exe`
- 已连接真机设备：`10AD4X0XL00026M`

## 二、已经完成并推送到 GitHub 的工作

### 1. Android 客户端基础骨架

已完成 Compose 工程骨架、基础导航、可爱风格的 UI 基础层，以及真实可安装运行的 Android 调试包。

相关文件：
- [LinkApp.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/ui/LinkApp.kt)
- [AuthenticatedNavHost.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/ui/navigation/AuthenticatedNavHost.kt)
- [app/build.gradle.kts](/home/snoz/sth_play/link/app/build.gradle.kts)

### 2. 登录门禁与本地安全会话存储

已完成邀请码登录原型页、本地会话保存、昵称与配对码持久化。当前这部分仍是本地原型，不是真正的服务端校验。

相关文件：
- [InviteGateScreen.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/feature/auth/InviteGateScreen.kt)
- [InviteGateViewModel.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/feature/auth/InviteGateViewModel.kt)
- [SecureSessionStore.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/core/security/SecureSessionStore.kt)

### 3. 首页与权限引导中文化

大部分说明文案已切成中文，但按你的要求保留了 `Link`、`Today`、`Moments`、`Messages` 这些名字不改。还补了权限引导页，说明为什么 `Usage Access` 需要手动去系统页开启。

相关文件：
- [PermissionGuideScreen.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/feature/setup/PermissionGuideScreen.kt)
- [HomeScreen.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/ui/screens/HomeScreen.kt)

### 4. Moments 本地 usage 时间线

已接入 `UsageStatsManager` 的本地时间线读取，能展示本机当天常用 App 和部分时间线信息。

相关文件：
- [UsageTimelineRepository.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/core/usage/UsageTimelineRepository.kt)
- [ActivityScreen.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/ui/screens/ActivityScreen.kt)

### 5. 本地聊天原型

`Messages` 页面已有本地消息流和发送体验原型，但还没有接真实服务器同步。

相关文件：
- [ChatScreen.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/ui/screens/ChatScreen.kt)
- [ChatViewModel.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/feature/chat/ChatViewModel.kt)
- [InMemoryChatRepository.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/core/chat/InMemoryChatRepository.kt)

### 6. 本地编译与真机安装链路

已经补齐 Gradle wrapper，可在 WSL 内构建，再通过 Windows adb 装到真机。

常用命令：

```bash
./gradlew --no-daemon assembleDebug
/mnt/c/Users/ROG/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```

## 三、已经验证过但后来回滚/放弃的内容

### 1. AccessibilityService 前台切换采集

做过一版辅助功能采集，目标是提升微信/分身 App 的前台切换识别，但这版已经完整回滚，不在当前稳定代码线里。

原因：
- 用户体验和权限敏感度更高
- 在当前机型上未形成稳定收益
- 需要重新做最小化采集和更细的事件过滤，不能直接沿用旧实现

### 2. `0.1.2` 与 `0.1.4` 相关 usage 统计尝试

做过几轮为了修 `aweme/微信` 和时长统计的实验性改动，结果引入了新的口径偏差或体验问题，最终已回到 `0.1.1` 这条线。

结论：
- 当前稳定线不是“功能最多”的线，而是“副作用最少”的线
- 后续若继续修 usage 时长和微信识别，必须小步推进，不要再直接替换整套统计逻辑

## 四、当前本地工作区里尚未提交的内容

当前工作区不是干净的，有一组后端相关改动还没提交。新对话里要明确这一点，不要误以为这些已经在 GitHub 上。

当前未提交文件：
- `docs/api-contract.md`
- `server/README.md`
- `server/app/main.py`
- `server/app/models.py`
- `server/app/db.py` 新文件
- `server/.venv/` 本地虚拟环境，不应提交
- `server/link.db` 本地 sqlite 数据库，不应提交

### 这些未提交内容大致做了什么

已经起了一个 FastAPI + SQLite 的后端草稿，覆盖了下面几类接口：

- 健康检查：`GET /health`
- 邀请码创建：`POST /v1/internal/invites/create`
- 邀请码解锁：`POST /v1/auth/invite/unlock`
- 配对状态：`GET /v1/pair/status/{pair_id}`
- 消息同步：`POST /v1/messages/sync`
- usage 上传：`POST /v1/usage/upload`
- 对方最近 usage：`GET /v1/usage/latest/{pair_id}`

相关文件：
- [server/app/main.py](/home/snoz/sth_play/link/server/app/main.py)
- [server/app/models.py](/home/snoz/sth_play/link/server/app/models.py)
- [server/app/db.py](/home/snoz/sth_play/link/server/app/db.py)
- [server/README.md](/home/snoz/sth_play/link/server/README.md)
- [api-contract.md](/home/snoz/sth_play/link/docs/api-contract.md)

### 当前后端草稿的关键问题

这个草稿不是最终可用状态，至少有 3 个问题：

1. 邀请码目前是单次使用逻辑，不适合情侣双方加入同一对。
2. Android 端还没有真正接入网络层，所以现阶段即使后端能跑，App 也不会自动走这些接口。
3. `server/.venv` 和 `server/link.db` 目前还在工作区里，需要在提交前加到 `.gitignore`，避免脏文件进入仓库。

## 五、下一步建议的开发顺序

优先按下面顺序推进，不要同时改太多层。

### 第一步：把后端配对模型修正确

后端现在最该先修的是“邀请和配对”的数据模型。建议方案：

- 一个邀请码要能支撑一对情侣的两个成员加入，而不是只能用一次
- 邀请码和配对码的职责要分清
- 需要明确邀请码是否单独发给每个人，还是一组情侣共用一套 `invite_key + pair_code`

如果要走当前 Android 原型的交互，最自然的是：
- 用户输入 `昵称 + 配对码 + 邀请码`
- 服务端校验后签发 `session_token`
- 同一对最多 2 个成员

### 第二步：提交并推送后端第一版

在配对逻辑修好后，再统一做：
- 清理 `.gitignore`
- 跑导入测试和接口 smoke test
- 提交后端代码
- 推送到 GitHub

### 第三步：安卓端接入真实服务端

Android 端目前缺的是网络层和真实仓库实现。下一步应增加：

- 认证接口调用：登录页从“本地原型解锁”改成真实 `unlock`
- 消息仓库：从 `InMemoryChatRepository` 改成服务端同步仓库
- usage 上报：把 `Moments` 的本地时间线上传到后端
- 对方动态读取：从后端读取对方最新 usage 快照

### 第四步：再处理 usage 精度问题

在“服务端打通”之前，不建议继续深挖微信/分身识别问题。因为即使本机读得再复杂，没有同步链路也无法体现对方视角。

usage 精度问题后续建议单独做一个任务：
- 先固定统计口径
- 再处理包名映射
- 最后才考虑厂商分身兼容或辅助功能方案

## 六、注意事项

### 1. 不要把当前后端草稿误认为已经完成

后端文件虽然已经存在，但当前只是本地开发草稿，还没有形成一版稳定提交。

### 2. 不要把 `.venv` 和 `link.db` 提交进仓库

这两个都是本地开发产物：
- `server/.venv/`
- `server/link.db`

提交前必须忽略。

### 3. 当前最稳定的 Android 代码线是 `0.1.1`

不要直接基于之前被回滚的 `0.1.2`、`0.1.3`、`0.1.4` 实验逻辑继续叠改，尤其是：
- usage 时长硬算逻辑
- AccessibilityService 采集逻辑

### 4. Android 权限边界要继续保持清楚

`Usage Access` 不是普通运行时权限，不能像相机/相册那样直接弹一个标准授权框。现有的“权限引导页 + 跳系统设置”路径是符合 Android 机制的。

### 5. 新对话建议先读这 3 处

新对话一开始，建议先让助手看这几个文件：
- [本文件](/home/snoz/sth_play/link/docs/project-handoff.md)
- [server/app/main.py](/home/snoz/sth_play/link/server/app/main.py)
- [InviteGateViewModel.kt](/home/snoz/sth_play/link/app/src/main/java/com/snozzz/link/feature/auth/InviteGateViewModel.kt)

## 七、建议在新对话里直接给出的任务描述

可以直接复制下面这段给新对话：

```text
请先阅读 docs/project-handoff.md，然后基于当前仓库状态继续开发。优先处理服务端：先把邀请码/配对模型修正确，再完成后端第一版提交；之后再把 Android 端登录、消息同步、Moments 上传与对方动态读取接到真实服务端。注意当前 GitHub 已提交稳定线是 Android 0.1.1，本地还有未提交的后端草稿，不要把实验性 usage 修复或已回滚的 AccessibilityService 逻辑重新带回来。
```
