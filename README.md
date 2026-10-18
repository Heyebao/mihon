<div align="center">

<a href="https://mihon.app">
  <img src="./.github/assets/logo.png" alt="Mihon logo" title="Mihon logo" width="80" />
</a>

# Mihon 双页阅读实验分支

基于 [Mihon 官方项目](https://github.com/mihonapp/mihon) 的个人 fork，主要整理和改进 WebGPU 阅读器的双页配对、页型记录及阅读进度处理。

[本 fork 的构建入口](https://github.com/Heyebao/mihon/actions/workflows/build-debug.yml) · [已知问题](#known-issues) · [官方稳定版下载](https://mihon.app/download/) · [Apache-2.0 许可证](./LICENSE)

</div>

> **先看当前状态**：这是实验性修改分支，不是 Mihon 官方发布渠道。截至 **2026-10-18**，本仓库没有发布 Release，测试 APK 通过 GitHub Actions 提供，默认只保留 **3 天**。已收到“从左到右”双页异常的反馈，尚未确认根因或完成修复；编译、单测通过不等于所有设备上的双页行为都正常。

## 目录

- [项目定位与官方关系](#about)
- [已合并的改动与验证程度](#changes)
- [下载测试 APK](#download)
- [安装、备份与迁移](#install)
- [开启和使用双页阅读](#dual-page)
- [已知问题与功能边界](#known-issues)
- [常见问题与排查](#troubleshooting)
- [反馈问题与参与贡献](#feedback)
- [给开发者的构建与测试说明](#build)
- [同步官方更新并保留本 fork 改动](#upstream)
- [上游功能、鸣谢与许可证](#credits)

<a id="about"></a>

## 项目定位与官方关系

Mihon 是免费、开源的 Android 漫画阅读器，可用于阅读漫画、条漫、美漫等内容。本仓库 `Heyebao/mihon` 是它的个人修改分支，官方上游为 [`mihonapp/mihon`](https://github.com/mihonapp/mihon)。

本 fork 复用上游已有的 WebGPU 双页能力，主要处理图片信息晚到、图片缓存重建和双页重排时的一些边界情况。双页功能本身并非本 fork 从零实现；本分支的修改也不代表已经进入官方版本。

当前定位是供需要双页阅读的用户试用、反馈和复现问题。希望使用官方稳定版，请前往 [Mihon 官方下载页](https://mihon.app/download/)。本仓库的 Actions 测试包与官方稳定版、官方预览版是不同的分发来源，下载时请核对页面所属仓库。

本项目不托管漫画内容，也不代表任何内容提供方。使用方式、图源和具体漫画文件可能影响问题的复现结果。

<a id="changes"></a>

## 已合并的改动与验证程度

下表描述的是代码已包含的处理，不是“所有同类问题均已解决”的承诺。

| 范围 | 当前处理 | 验证程度 |
| --- | --- | --- |
| 源图片信息 | 在 `ReaderPage` 保存原始尺寸与页标签，减少 GPU 图片缓存回收、重建后丢失页型信息的情况 | 已合并，包含规则测试 |
| 未确认的配页 | 普通页面尺寸未知时暂不按形状组对，信息明确后再调整，减少把潜在跨页直接跨过去的情况 | 已合并，包含未知尺寸变为已知尺寸的规则测试 |
| 配置变化后的定位 | 重建双页时保留请求的源页，减少跳到第二张后切回单页却退到前一张的情况 | 已合并，包含目标页选择测试 |
| 双页进度 | 修复仅后半页解码成功就把进度推进到后半页的一条路径，两侧均成功后才上报伴页进度 | 已合并，包含进度选择测试 |
| 连续阅读进度 | 统一进度通知入口，修正滚动后回跳可能被错误去重的情况 | 已合并，仍需设备回归 |
| 翻页中的重排 | 双页翻动期间延后页型信息生效，停稳后更新配页与当前页，减少中途改变翻页目标 | 已合并，仍需慢加载和连续滑动实测 |
| 手动生成测试包 | 提供手动 Debug 构建，执行单测、生成 APK、上传 Actions 产物 | 已有成功构建，产物保留 3 天 |

相关合并记录：[PR #1](https://github.com/Heyebao/mihon/pull/1)、[PR #2](https://github.com/Heyebao/mihon/pull/2)。

已核验的一组代码验证记录如下，供追溯，不表示它永远是最新构建：

- 修复提交：[`ab816843`](https://github.com/Heyebao/mihon/commit/ab816843b0adaeb093d390c13f0962a7dc528f79)，已由 PR #2 合入 `main`。
- [完整代码检查与 Release 编译](https://github.com/Heyebao/mihon/actions/runs/39056047422)：格式检查、Debug 单元测试、SQLDelight 迁移验证和 Release 编译成功。
- [Debug 单测与 APK 构建](https://github.com/Heyebao/mihon/actions/runs/39056074879)：成功。

**这些检查没有替代手机实测。** 左右阅读、慢加载、旋转、跨章、快速翻动和连续阅读回跳仍是需要重点验证的场景。“从左到右”的已知异常见[下文](#known-issues)。

<a id="download"></a>

## 下载测试 APK

### 普通用户：下载已有构建，不需要安装开发环境

1. 登录 GitHub，打开本仓库的 [Build Debug APK](https://github.com/Heyebao/mihon/actions/workflows/build-debug.yml) 页面。
2. 在运行列表中选择一条**成功完成**的构建。核对分支、提交和运行时间；需要当前主分支时，应优先选择 `main`，不要仅凭绿色标记判断它包含所有新改动。
3. 进入该次运行的详情页，在底部找到 **Artifacts**。
4. 下载名称类似 `mihon-debug-apk-<完整提交号>` 的产物。这是 ZIP 压缩包，需要先解压。
5. 一般用户可选择其中的 `app-universal-debug.apk`。它包含多种 CPU 架构，文件较大；如果清楚设备架构，也可选对应的 APK。

| APK 文件中的标记 | 用途 |
| --- | --- |
| `universal` | 通用包，不确定架构时优先选择 |
| `arm64-v8a` | 常见的 64 位 ARM 手机和平板 |
| `armeabi-v7a` | 32 位 ARM 设备 |
| `x86`、`x86_64` | 对应架构的设备或模拟器 |

GitHub 要求下载 Actions 产物时已登录账号，并对仓库有读取权限；公开仓库的产物也需要登录。参见 [GitHub 官方产物下载说明](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts)。

### 已核验构建示例与有效期

截至本页核对日期，可追溯的测试构建为 [运行 #39056074879](https://github.com/Heyebao/mihon/actions/runs/39056074879)，源码提交为 `ab816843`。其[产物入口](https://github.com/Heyebao/mihon/actions/runs/39056074879/artifacts/12109083692)当时有效，过期时间为 **2026-10-21 02:06 左右（北京时间）**；以 GitHub 页面实际状态为准，提前删除也会使链接失效。

这个链接不是永久下载地址，也不承诺是你阅读本页时的最新版本。过期后请回到构建列表寻找后续产物。仓库目前没有可替代它的 Release 下载，不要把上方的**官方稳定版下载**当成本 fork 的测试包。

### 没有可下载产物时

- **普通访问者**：可以下载仍有效的公开构建产物；没有仓库写权限时，不能在这里替维护者启动构建。
- **仓库维护者或有写权限的协作者**：进入 [Build Debug APK](https://github.com/Heyebao/mihon/actions/workflows/build-debug.yml)，点击 **Run workflow**，选择 `main`，再运行。等待单测、打包和上传全部完成后下载。
- **自己的 fork**：也可在自己的仓库中启用并运行相同工作流；它产生的是你自己的构建，需自行确认源码、设置与签名。

当前 [Debug 工作流](./.github/workflows/build-debug.yml)使用 `workflow_dispatch` 手动触发，产物 `retention-days` 为 **3**。手动运行需要写权限，工作流还必须存在于默认分支；具体限制参见 [GitHub 手动运行说明](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)。失败、取消的构建不保证有 APK。

<a id="install"></a>

## 安装、备份与迁移

### 系统要求与版本辨认

- 当前源码要求 **Android 8.0 / API 26 或更高**。达到最低 Android 版本不等于所有设备都已通过 WebGPU 双页实测。
- 当前手动工作流生成的是 **Debug 测试包**，不是官方签名的稳定发布包。
- 对上面 `ab816843` 构建的通用 APK，已从 Manifest 静态核对包名为 **`app.mihon.dev`**，版本名为 `0.21.2-1`。官方稳定版使用 `app.mihon`；这两个包名不同，通常可以并存，应用数据各自独立。
- 这个核对只适用于所列测试包。其他变体、官方预览版或未来构建的包名可能不同，不能保证与所有 Mihon 变体都能并存。

下载 ZIP 后，在 Android 文件管理器里打开解压出的 APK，按系统提示允许该文件管理器进行此次安装，再完成安装。安装后请核对你打开的是对应测试应用。

### 先做应用内备份，保留原漫画文件

如果你已经在使用其他 Mihon 版本，先在原应用的“更多 → 设置 → 数据与存储”中创建备份，再试用或迁移；界面文字以实际版本为准。

备份能保存的内容与所选项目有关，例如书架、分类、阅读记录、历史、追踪信息及设置。**备份文件不等于漫画文件备份**：它不包含已下载章节、本地图源漫画文件、扩展 APK 或自定义封面。请保留原来的漫画目录和文件，恢复后按需要配置存储目录权限、重新安装扩展、重新登录追踪服务。找不到已有下载时，可参考官方说明重新索引下载。

详细步骤与兼容限制请看 [官方备份指南](https://mihon.app/docs/guides/backups)及[存储说明](https://mihon.app/docs/faq/storage)。不要假定任意新旧版本、任意 fork 的备份都永久互通；还原前保留原备份和原应用数据。

### 覆盖安装与更新

Android 覆盖安装除了包名相同，还要求签名兼容。独立云端构建若没有固定签名，**不能保证每次都能直接覆盖之前的 Debug 包**。如果提示签名冲突或“应用未安装”，先核对旧包身份、备份与原文件，不要把直接卸载旧应用当作第一步。

本仓库手动 Debug 工作流使用默认本地分发配置，未启用自动更新器。需要更新测试版时，请手动获取本仓库新的成功构建，并核对提交与签名。官方稳定版更新不会自动把这些自定义改动带进测试版。

<a id="dual-page"></a>

## 开启和使用双页阅读

本节针对**从左到右／从右到左的分页阅读**。当前双页默认是“从不”，高质量渲染器默认关闭，所以刚安装时看不到双页选项并不奇怪。

### 开启步骤

1. 先退出正在阅读的章节。
2. 进入 **更多 → 设置 → 高级 → 阅读 → 使用高质量渲染器**，打开它。
3. 重新进入漫画章节，让阅读器重新创建；如果仍未刷新，可退出应用后重新打开。
4. 点击画面呼出菜单，打开阅读设置，在 **阅读模式** 页选择 **从左到右**或**从右到左**。
5. 在其下方找到 **双页视图**，先选择 **始终**，确认入口和显示能工作。
6. 需要横屏自动双页时，再改为 **宽屏时**，并检查阅读器的旋转锁定与系统旋转设置。

上面的阅读模式名称仍可能带有“单页”字样；**双页视图是下面的独立开关**，不是阅读模式列表中新增的第七种模式。

| 选项 | 当前含义 |
| --- | --- |
| 从不 | 不启用双页 |
| 始终 | 在支持的分页阅读器中启用双页，不以屏幕宽高比为条件 |
| 宽屏时 | 阅读区域的宽度大于高度时启用；变窄后回到单页 |

“宽屏时”看的是**实际阅读区域**，不是只看手机是否横放。分屏、浮窗或某些折叠屏状态下，即使系统处于横屏，阅读区域也不一定足够宽。

### 两个容易混淆的设置

- **双页视图**：把相邻的两张源页排在同一画面，并对已识别的整幅跨页进行独占等处理，是本 fork 主要改进的路径。
- **拆分双页**：旧阅读器中把一张宽图分开显示的另一个功能。它不等于把两张竖图并排；不要为了开启双页而照着名字打开“拆分双页”。

当前“双页视图”开关保存为全局阅读偏好，可能影响其他漫画；**阅读方向本身**则是漫画的阅读模式设置。设置页出现“本作品”标题，并不表示这里每个开关都只作用于当前作品。

### 显示上的预期与限制

- 普通无明确页标签的章节通常保留首张作为封面，从后续页面开始配对，因此封面可能只占一侧；这不等于漏了一张页。
- 图片信息尚未明确时，可能先显示单页或加载占位，待尺寸和标签明确后再调整。灰色圆形占位表示某一页还在加载，单凭它不能判断文件已经丢失或被拆图。
- 当前未带明确方向标签的图片，宽高比**严格大于 `1.2`**才按整幅横图独占规则处理。恰好 `1.2` 或 `1.1` 不会仅因宽于高就自动独占；明确的页标签还可能优先影响判断。
- 长条和连续滚动阅读不启用这套双页模式。上下翻页没有上述双页设置入口，也不属于本 fork 当前说明和实测覆盖的主要范围。

<a id="known-issues"></a>

## 已知问题与功能边界

### 从左到右的双页异常仍在调查

已知反馈中，同一内容在设置选择 **从左到右**时出现跨页内容分离或配组异常，而**从右到左**在该样例中正常。这不等于所有从左到右的漫画都异常，也不能据此保证所有从右到左的内容都没有问题。

源码审查发现一个候选原因：图片自带的 `PageName=Left/Right` 物理页标签，可能与阅读方向决定的配页顺序发生冲突。**尚未核验问题原图的标签和完整输入，也未确认该反馈的最终根因；目前不能标为已修复。** 图片看起来分成两半，也不足以证明阅读器把一个文件实际切成了两张。

临时对照时可以分别测试两个阅读模式并记录差异，但不要为了让画面暂时正常就长期使用不符合漫画内容顺序的方向。请按[反馈清单](#feedback)提供原始样例，便于修正后验证。

### 手动配对偏移尚未集成

当前 `main` 没有“调整双页配对偏移”按钮，也没有承诺自动识别任意两张竖图是否属于同一幅跨页。其他分支中的偏移功能不代表已包含在本仓库。后续移植必须保留现有的源页信息、导航和进度修复，并补充方向、横图晚识别及偏移开关前后的回归测试。

### 尚需设备验证的场景

慢网时快速翻页、前后连续跨页、旋转与分屏变化、章末伴页加载失败、跨章，以及长图阅读回跳都仍需更多设备样例。规则单测能验证一部分配对决策，不能覆盖真实触摸、GPU 渲染和网络时序。

<a id="troubleshooting"></a>

## 常见问题与排查

| 现象 | 优先检查 |
| --- | --- |
| 阅读模式里只有从左到右、从右到左、上下等选项 | 双页在独立的“双页视图”行；先开启高质量渲染器，退出章节再进入，并选择左右分页模式 |
| 开了“宽屏时”仍是单页 | 先用“始终”对照；检查阅读区域宽高、应用旋转锁定、系统旋转和分屏状态 |
| 只有一页，旁边留空 | 核对是否为封面、章末单页、独占横图，或邻页仍在加载；连续错配请按问题反馈处理 |
| 漫画旁边有灰圆 | 对应一侧仍处于加载占位；记录等待后是否消失、下载后是否正常，以及此时页码 |
| 从左到右异常、从右到左正常 | 属于上面列出的待查问题；保存两个方向的对照和原图信息，不要直接认定已经切坏文件 |
| 找不到“调整配对偏移” | 当前未集成该功能 |
| Actions 下载按钮不可用 | 登录 GitHub，确认运行成功、产物尚未过期，查看运行详情底部的 Artifacts |
| 看不到 Run workflow | 普通访问者没有写权限；也需核对工作流是否存在于默认分支并启用了手动触发 |
| ZIP 无法直接安装 | 先解压，打开其中匹配架构的 `.apk`，而不是安装 ZIP 本身 |
| APK 提示无法覆盖安装 | 核对包名、版本和签名；先备份，不要直接删除原应用与漫画文件 |
| 恢复备份后没有下载章节 | 备份不包含漫画文件；保留原目录，配置访问权限，必要时重新索引 |

基础使用问题也可参考 [官方入门指南](https://mihon.app/docs/guides/getting-started)和[官方排障指南](https://mihon.app/docs/guides/troubleshooting)。本 fork 独有的行为请先在本仓库讨论，不要未经对照就作为官方版本的问题提交。

<a id="feedback"></a>

## 反馈问题与参与贡献

截至核对日期，本仓库尚未开启 Issues 和 Discussions。可以登录 GitHub，在现有的[双页修复 PR #2 讨论区](https://github.com/Heyebao/mihon/pull/2)补充可复现的问题信息；如果后续开启独立问题区，再优先使用该入口。涉及本 fork 的修改，请附上以下资料：

1. **版本来源**：Actions 运行链接、源码提交号、APK 文件名；只写“最新版”不足以定位。
2. **设备与窗口**：设备型号、Android 版本、横竖屏、是否分屏或折叠屏。
3. **阅读设置**：高质量渲染器是否开启、阅读模式的准确名称、双页视图选项、缩放/裁边相关设置。
4. **内容来源**：本地文件或在线图源；能够合法提供时附一个最小章节/图片样例，或可定位的作品与章节信息。
5. **页码与步骤**：从哪一页开始，点按还是滑动，是否等待加载，向前/向后是否不同，旋转或重进章节后的变化。
6. **对照证据**：从左到右和从右到左分别截图或录屏；注明预期两张怎样组合。原图文件有助于核对尺寸和页标签，截图不能替代所有输入信息。
7. **错误信息**：如果崩溃或出现解码错误，附可用日志，并先移除账号、令牌、个人目录等敏感内容。

贡献代码前请阅读 [行为准则](./CODE_OF_CONDUCT.md)与[贡献指南](./CONTRIBUTING.md)。较大的功能或行为变化建议先讨论范围；请把修复、界面入口、构建镜像和发布配置分开说明，便于核对影响。

欢迎补充同一批源页在 LTR/RTL 下的测试、横图信息晚到、连续横图、奇数尾页、跨章和旋转场景。新增测试应覆盖实际使用的判定路径，设备验证应注明设备和复现步骤。

<a id="build"></a>

## 给开发者的构建与测试说明

普通用户直接下载 Actions 产物即可，无需在电脑安装 Android Studio、JDK 或完整 SDK。自行构建需要 Android 开发环境与磁盘空间，请以源码和工作流为准。

### 当前工具链

| 项目 | 当前配置 | 来源 |
| --- | --- | --- |
| 最低 Android | API 26 / Android 8.0 | [`gradle/mihon.versions.toml`](./gradle/mihon.versions.toml) |
| Gradle 运行 JDK | 21 | [`.github/.java-version`](./.github/.java-version) |
| Android 编译平台 | 37.2 | [`gradle/mihon.versions.toml`](./gradle/mihon.versions.toml)与 Debug 工作流 |
| NDK | `29.0.14206865` | [`gradle/mihon.versions.toml`](./gradle/mihon.versions.toml) |
| Gradle Wrapper | 9.8.1 | [`gradle-wrapper.properties`](./gradle/wrapper/gradle-wrapper.properties) |

使用项目自带 Wrapper，不需要另装系统 Gradle。准备对应 SDK/NDK、接受必要许可证，并按 Android 工具要求配置 SDK 路径。JDK 运行版本与项目的 Java 字节码兼容级别不是同一个设置。

Linux/macOS，在项目根目录执行：

```bash
./gradlew spotlessCheck testDebugUnitTest verifySqlDelightMigration
./gradlew assembleDebug --stacktrace
```

Windows PowerShell：

```powershell
.\gradlew.bat spotlessCheck testDebugUnitTest verifySqlDelightMigration
.\gradlew.bat assembleDebug --stacktrace
```

Debug APK 位于 `app/build/outputs/apk/debug/`，通用包通常为 `app-universal-debug.apk`。不带自定义发布参数的本地/手动 Debug 构建默认不启用更新器。

当前[代码检查工作流](./.github/workflows/build.yml)还执行 `assembleRelease -Pdist=ci`。这里的 Release 编译用于验证，并不等于生成了官方签名、可覆盖官方稳定版的发布包；请勿仅凭文件名中的 `release` 判断安装兼容性。

纯 Markdown PR 会被当前 Android 构建工作流的路径过滤排除，文档修改应检查链接、内容和实际差异，不能把“未触发构建”写成“Android 测试通过”。源码修改应核对所对应提交的实际 CI 结果，并对阅读器行为做设备回归。

<a id="upstream"></a>

## 同步官方更新并保留本 fork 改动

fork 是独立仓库，不会因为官方发布新版本，就自动把新代码、安全修正或本分支的自定义改动合并好。同步上游是在保留本 fork 修改的基础上引入官方更新，可能出现需要人工判断的冲突。

### 不熟悉命令行时

以下操作需要该 fork 的写权限；普通访问者可以查看变更，不需要尝试替本仓库同步代码。

1. 在本仓库先保留当前分支的备份，确认尚未完成的修改已经保存。
2. 在 GitHub 仓库页面查看 **Sync fork → Update branch**。
3. 如果提示冲突或需要丢弃本 fork 提交，不要直接覆盖来消除提示；应交给能处理代码冲突的人，在维护分支中合并、检查，再提交到本 fork。
4. 同步后重新执行格式、单测、迁移与编译检查，并实测双页、已知问题和数据兼容性。
5. 只有新构建成功并确认对应提交后，才更新下载与问题状态；旧 Actions 包不会因为源码同步而自动变成新版。

### 命令行维护示例

先用 `git remote -v` 核对远端；`origin` 应指向自己的 fork。仅在尚未配置 `upstream` 时添加：

```bash
git remote add upstream https://github.com/mihonapp/mihon.git
```

在干净、已保存的工作树中，以当前 fork 的 `main` 为起点创建维护分支，例如：

```bash
git fetch origin
git switch -c maintenance/sync-upstream origin/main
git fetch upstream
git merge upstream/main
```

分支名可自行更换。发生冲突时先解决并审阅差异，不要在合并未完成时继续发布；然后执行上述验证，推送维护分支，向**本 fork 的 `main`** 创建 PR：

```bash
git push -u origin maintenance/sync-upstream
```

合并上游通常能保留 fork 独有提交，但冲突处理仍可能改变其实际行为，不能代替回归测试。不要为了“和官方一致”直接丢弃本分支的自定义修改。详细操作参见 [GitHub 同步 fork 文档](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/syncing-a-fork)。

<a id="credits"></a>

## 上游功能、鸣谢与许可证

### Mihon 上游提供的基础功能

- 本地内容阅读、多种阅读器、阅读方向和可配置选项。
- 书架分类、浅色/深色主题、定时更新章节。
- 应用数据备份与恢复；下载文件应另外保留。
- 追踪服务支持：[MangaBaka](https://mangabaka.org)、[MyAnimeList](https://myanimelist.net/)、[AniList](https://anilist.co/)、[Kitsu](https://kitsu.app/)、[MangaUpdates](https://mangaupdates.com)、[Shikimori](https://shikimori.one)、[Bangumi](https://bgm.tv/)、[Hikka](https://hikka.io/)。具体可用性以对应版本和服务状态为准。

官方入口：[网站](https://mihon.app/) · [下载](https://mihon.app/download/) · [更新日志](https://mihon.app/changelogs/) · [FAQ](https://mihon.app/docs/faq/general) · [Discord](https://discord.gg/mihon) · [翻译平台](https://hosted.weblate.org/engage/mihon/)。

感谢 Mihon、Tachiyomi 历史贡献者，以及相关开源库的作者与维护者。本 fork 的双页修改建立在这些上游工作之上。相关官方仓库：[Mihon](https://github.com/mihonapp/mihon)、[网站](https://github.com/mihonapp/website)、[bitmap.kt](https://github.com/mihonapp/bitmap.kt/)。

<a href="https://github.com/mihonapp/mihon/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=mihonapp/mihon" alt="Mihon app contributors" title="Mihon app contributors" width="800" />
</a>

本项目开发者与内容提供方没有隶属关系，本应用本身不托管内容。

### Apache License 2.0

保留原有版权声明，完整条款见仓库 [LICENSE](./LICENSE)。

```text
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
