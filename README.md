# 易Mark (EasyMD)

一款基于 Android 平台的所见即所得 Markdown 笔记应用，采用 Kotlin + Jetpack Compose 构建，支持本地 / WebDAV / S3 多后端存储。

---

## 目录

- [功能特性](#功能特性)
  - [笔记管理](#笔记管理)
  - [编辑器](#编辑器)
  - [Markdown 支持](#markdown-支持)
  - [多笔记库与存储后端](#多笔记库与存储后端)
  - [标签系统](#标签系统)
  - [附件管理](#附件管理)
  - [界面与体验](#界面与体验)
- [技术架构](#技术架构)
  - [技术栈](#技术栈)
  - [架构模式](#架构模式)
  - [导航路由](#导航路由)
- [项目结构](#项目结构)
  - [完整目录树](#完整目录树)
  - [核心模块说明](#核心模块说明)
    - [数据层 (data/)](#数据层-data)
    - [存储后端 (data/storage/)](#存储后端-datastorage)
    - [附件系统 (data/attachment/)](#附件系统-dataattachment)
    - [UI 组件 (ui/components/)](#ui-组件-uicomponents)
    - [页面 (ui/screens/)](#ui-页面-uiscreens)
    - [ViewModel 层 (ui/viewmodel/)](#viewmodel-层-uiviewmodel)
    - [主题 (ui/theme/)](#主题-uitheme)
- [编辑器详解](#编辑器详解)
  - [块划分](#块划分)
  - [所见即所得渲染](#所见即所得渲染)
  - [语法高亮转换](#语法高亮转换)
  - [工具栏语法插入](#工具栏语法插入)
  - [标题自动空格](#标题自动空格)
  - [焦点管理](#焦点管理)
- [数据格式](#数据格式)
  - [笔记文件结构](#笔记文件结构)
  - [附件索引](#附件索引)
  - [笔记库配置](#笔记库配置)
- [构建配置](#构建配置)
  - [环境要求](#环境要求)
  - [依赖列表](#依赖列表)
  - [构建命令](#构建命令)
- [开发路线图](#开发路线图)
- [许可证](#许可证)

---

## 功能特性

### 笔记管理

- **创建笔记** — 点击 FAB 按钮快速新建空白笔记，自动进入编辑界面
- **编辑笔记** — 点击笔记卡片进入 WYSIWYG 编辑器，离开时自动保存
- **删除笔记** — 单条删除或长按进入多选模式批量删除
- **笔记列表** — 按更新时间倒序排列，支持 LazyColumn（手机）和 LazyVerticalGrid（平板）两种布局
- **全文搜索** — 搜索标题和正文内容
- **下拉刷新** — 手动同步笔记库内容

### 编辑器

- **所见即所得 (WYSIWYG)** — 全部内容块始终处于可编辑状态，无需切换"编辑/预览"模式
- **块级编辑** — 内容按空行切分为独立块（段落、标题、列表、代码块、引用块），每块独立编辑
- **实时语法高亮** — 编辑时 Markdown 格式符号以强调色显示，内容部分按语义应用粗体/斜体/删除线样式
- **上下文样式** — 标题块按层级（H1~H6）自动应用对应字号和粗体，代码块用等宽字体，引用块用斜体
- **Markdown 工具栏** — 底部横向滚动工具栏，提供 19 种语法快捷按钮（标题、粗体、斜体、列表、链接、图片、表格等）
- **工具栏自定义** — 可自由选择显示哪些语法按钮
- **标题自动空格** — 行首输入 `#` 后接非空格字符时，自动插入空格（`#标题` → `# 标题`）
- **图片/附件插入** — 通过系统文件选择器插入图片或附件，自动生成 Markdown 引用语法

### Markdown 支持

基于 [CommonMark](https://commonmark.org/) 规范（commonmark-java 0.21.0），支持以下语法：

| 类别 | 语法 |
|------|------|
| 标题 | H1 ~ H6 (`#` ~ `######`) |
| 文本样式 | 粗体 (`**text**`)、斜体 (`*text*`)、删除线 (`~~text~~`) |
| 代码 | 行内代码 (`` `code` ``)、围栏代码块 (` ```lang `) |
| 列表 | 无序列表 (`- ` / `* `)、有序列表 (`1. `)、任务列表 (`- [ ] ` / `- [x] `) |
| 引用 | 块引用 (`> `) |
| 链接 | 超链接 (`[text](url)`)、图片 (`![alt](url)`) |
| 分隔 | 水平分隔线 (`---` / `***`) |
| 扩展 | 脚注、表格语法（工具栏支持插入） |

### 多笔记库与存储后端

- **笔记库管理** — 支持创建多个独立的笔记库，每个库可配置不同的存储后端
- **全部笔记库** — 聚合模式下可同时浏览和管理所有库中的笔记
- **本地存储** — 笔记以 `.md` 文件保存在设备内部存储，支持自定义目录
- **WebDAV 存储** — 通过 WebDAV 协议同步笔记到远程服务器（支持 PROPFIND / GET / PUT / DELETE / MKCOL / MOVE）
- **S3 存储** — 通过 AWS Signature V4 认证连接 S3 兼容对象存储
- **连接测试** — 存储设置页面提供连接测试功能

### 标签系统

- **添加标签** — 编辑界面中可自由添加/删除标签
- **标签筛选** — 侧边栏中点击标签即可筛选该标签下的所有笔记
- **标签显示** — 笔记卡片底部显示前 2 个标签

### 附件管理

- **附件插入** — 支持插入图片和任意类型文件，自动复制到笔记的 `assets/` 子目录
- **SHA-256 校验** — 附件以 SHA-256 哈希值命名，避免重复存储
- **引用完整性检查** — 可检测文中引用与实际文件的对应关系，识别失效引用和孤立文件
- **孤立文件清理** — 自动清理未被任何笔记引用的附件文件
- **附件管理对话框** — 查看、删除笔记的所有附件

### 界面与体验

- **Material Design 3** — 采用最新 Material You 设计语言，青绿色（Teal）主色调
- **深色模式** — 支持浅色/深色/跟随系统三种模式
- **侧边抽屉导航** — 笔记库切换、标签筛选、设置入口集中管理
- **卡片式布局** — 笔记以卡片展示：标题、纯文字预览、相对时间、标签
- **平板适配** — 检测屏幕宽度自动切换网格布局，支持 2/3 列设置
- **下拉展开标题** — 编辑界面顶部下拉可显示标题栏
- **分享与导出** — 支持系统分享（文本）和导出为 `.md` 文件

---

## 技术架构

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0.21 |
| UI 框架 | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| 架构 | MVVM（Model-View-ViewModel） | — |
| 导航 | Navigation Compose | 2.7.7 |
| 状态管理 | StateFlow + collectAsState | — |
| Markdown 解析 | commonmark-java | 0.21.0 |
| HTTP 客户端 | OkHttp | 4.12.0 |
| HTML 解析 | Jsoup | 1.17.2 |
| 构建工具 | Gradle (Kotlin DSL) | 8.11.1 |
| 最低 SDK | Android 8.0 (API 26) | — |
| 目标/编译 SDK | Android 16 (API 36) | — |
| JDK | 17 | — |

### 架构模式

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer (Compose Screens & Components)                │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ NoteList    │  │ EditNote     │  │ Settings /    │  │
│  │ Screen      │  │ Screen       │  │ Storage       │  │
│  └──────┬──────┘  └──────┬───────┘  └───────┬───────┘  │
│         │                │                   │          │
│  ┌──────┴────────────────┴───────────────────┴───────┐  │
│  │            ViewModel Layer (StateFlow)            │  │
│  │  NoteListVM | EditNoteVM | ThemeVM | StorageVM   │  │
│  └──────────────────────┬───────────────────────────┘  │
├─────────────────────────┼──────────────────────────────┤
│  Data Layer             │                              │
│  ┌──────────────────────┴───────────────────────────┐  │
│  │  NoteRepository (file I/O, YAML front matter)    │  │
│  │  MediaHelper / AttachmentManager                 │  │
│  │  ToolbarSettingsRepository / LayoutSettings      │  │
│  └──────────────────────┬───────────────────────────┘  │
│                         │                              │
│  ┌──────────────────────┴───────────────────────────┐  │
│  │  Storage Backend (NoteStorage interface)          │  │
│  │  LocalStorage │ WebDAVStorage │ S3Storage        │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**数据流**：
1. ViewModel 持有 `StateFlow<Data>` 作为唯一真相源
2. Compose 通过 `collectAsState()` 订阅状态变化
3. 用户操作通过 ViewModel 方法更新状态
4. ViewModel 通过 Repository 持久化到存储后端
5. 存储后端对上层透明——Repository 通过 `StorageManager` 获取当前激活的 `NoteStorage` 实现

### 导航路由

应用使用单 Activity + Navigation Compose 架构，定义以下路由：

| 路由 | 页面 | 参数 |
|------|------|------|
| `noteList` | 笔记列表（起始页） | 无 |
| `editNote/{noteId}` | 编辑已有笔记 | `noteId: String` |
| `newNote` | 新建笔记 | 无 |
| `settings` | 应用设置 | 无 |
| `storageSettings/{libraryId}` | 编辑笔记库存储配置 | `libraryId: String` |
| `storageSettings` | 新建笔记库 | 无 |

---

## 项目结构

### 完整目录树

```
easymd/
├── build.gradle.kts                       # 根项目配置（AGP 8.7.3, Kotlin 2.0.21）
├── settings.gradle.kts                    # 项目设置
├── gradle.properties                      # Gradle 属性
├── gradlew.bat                            # Gradle Wrapper（Windows）
├── local.properties                       # 本地 SDK 路径
├── README.md
├── app/
│   ├── build.gradle.kts                   # 应用模块配置
│   ├── proguard-rules.pro                 # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/easymd/
│       │   ├── EasyMDApplication.kt       # Application 入口
│       │   ├── MainActivity.kt            # 主 Activity + 导航图
│       │   ├── data/
│       │   │   ├── Note.kt                # 笔记数据模型
│       │   │   ├── NoteRepository.kt      # 笔记数据仓库
│       │   │   ├── LayoutSettings.kt      # 平板列数设置
│       │   │   ├── MediaHelper.kt         # 媒体文件辅助类
│       │   │   ├── ToolbarSettingsRepository.kt  # 工具栏设置持久化
│       │   │   ├── attachment/
│       │   │   │   ├── AttachmentEntry.kt        # 附件条目数据模型
│       │   │   │   ├── AttachmentCheckResult.kt  # 引用完整性检查结果
│       │   │   │   └── AttachmentManager.kt      # 附件管理器
│       │   │   └── storage/
│       │   │       ├── StorageConfig.kt          # 存储配置数据模型
│       │   │       ├── NoteLibrary.kt            # 笔记库数据模型
│       │   │       ├── NoteStorage.kt            # 存储后端接口
│       │   │       ├── LocalStorage.kt           # 本地文件系统实现
│       │   │       ├── WebDAVStorage.kt          # WebDAV 协议实现
│       │   │       ├── S3Storage.kt              # S3 兼容存储实现
│       │   │       └── StorageManager.kt         # 多库管理器
│       │   └── ui/
│       │       ├── components/
│       │       │   ├── MarkdownText.kt           # Markdown 渲染组件
│       │       │   ├── MarkdownToolbar.kt        # 语法工具栏组件
│       │       │   ├── ToolbarSettingsDialog.kt  # 工具栏自定义对话框
│       │       │   └── WysiwygEditor.kt          # 所见即所得编辑器
│       │       ├── screens/
│       │       │   ├── NoteListScreen.kt         # 笔记列表页
│       │       │   ├── EditNoteScreen.kt         # 笔记编辑页
│       │       │   ├── SettingsScreen.kt         # 设置页
│       │       │   └── StorageSettingsScreen.kt  # 存储配置页
│       │       ├── theme/
│       │       │   ├── Theme.kt                  # Material 3 主题配置
│       │       │   └── Type.kt                   # 字体排版配置
│       │       └── viewmodel/
│       │           ├── NoteListViewModel.kt       # 笔记列表 ViewModel
│       │           ├── EditNoteViewModel.kt       # 编辑页 ViewModel
│       │           ├── ThemeViewModel.kt          # 主题模式 ViewModel
│       │           └── StorageSettingsViewModel.kt # 存储设置 ViewModel
│       └── res/
│           ├── mipmap-anydpi-v26/
│           │   ├── ic_launcher.xml
│           │   └── ic_launcher_round.xml
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── themes.xml
```

### 核心模块说明

#### 数据层 (data/)

##### Note — 笔记数据模型

```kotlin
data class Note(
    val id: String,          // UUID 唯一标识
    val title: String,       // 笔记标题
    val content: String,     // Markdown 正文
    val createdAt: Date,     // 创建时间
    val updatedAt: Date,     // 最后更新时间
    val tags: List<String>   // 标签列表
)
```

提供便捷方法：
- `getPreview()` — 取前 5 行非空行，截取 200 字符作为预览
- `getRelativeTime()` — 中文相对时间（"片刻之前"、"X 分钟前"、"X 天前"等）
- `getFormattedDate()` — 完整日期格式 `yyyy-MM-dd HH:mm`

##### NoteRepository — 笔记数据仓库

核心持久化层，负责笔记的 CRUD 操作。

- **存储格式**：每篇笔记对应一个 `{uuid}.md` 文件，文件头包含 YAML Front Matter
- **路径解析**：通过 SharedPreferences 维护 `filename → UUID` 的映射表
- **多库支持**：通过 `StorageManager` 获取当前激活的 `NoteStorage` 实现，对上层完全透明
- **附件联动**：保存笔记时自动迁移正文引用的附件到当前库；更新附件引用路径以适配不同存储后端

主要方法：

| 方法 | 说明 |
|------|------|
| `getAllNotes(libraryId)` | 获取指定库的所有笔记，按更新时间倒序 |
| `getNoteById(noteId)` | 根据 UUID 获取单篇笔记 |
| `saveNote(note, forceUpdateTimestamp)` | 保存（新建或更新），返回保存后的笔记对象 |
| `deleteNote(noteId)` | 删除笔记文件及其附件 |
| `searchNotes(libraryId, query)` | 全文搜索标题和正文 |
| `getAllTags(libraryId)` | 聚合所有笔记的标签 |
| `getNotesByTag(libraryId, tag)` | 按标签筛选笔记 |
| `testStorageConnection(libraryId)` | 测试存储后端连接 |

##### 其他数据类

| 类 | 说明 |
|------|------|
| `LayoutSettings` | SharedPreferences 单例，持久化平板列数偏好 |
| `MediaHelper` | 媒体文件辅助，封装 `AttachmentManager` 的 URI 操作 |
| `ToolbarSettingsRepository` | SharedPreferences 单例，持久化工具栏按钮启用状态，默认全部启用 |

#### 存储后端 (data/storage/)

##### NoteStorage 接口

```kotlin
interface NoteStorage {
    suspend fun listNoteFiles(): List<String>
    suspend fun readNoteFile(fileName: String): String?
    suspend fun writeNoteFile(fileName: String, content: String)
    suspend fun deleteNoteFile(fileName: String)
    suspend fun renameNoteFile(oldName: String, newName: String)
    suspend fun fileExists(fileName: String): Boolean
    suspend fun testConnection(): Result<Unit>
    // 可选附件操作
    suspend fun listAttachments(noteId: String): List<AttachmentEntry>
    suspend fun addAttachment(noteId: String, uri: Uri): AttachmentEntry?
    suspend fun deleteAttachment(attachId: String)
    suspend fun getAttachmentUri(attachId: String): Uri?
}
```

##### 实现对比

| 特性 | LocalStorage | WebDAVStorage | S3Storage |
|------|-------------|--------------|-----------|
| 底层 API | `java.io.File` | OkHttp + WebDAV 协议 | OkHttp + AWS Signature V4 |
| 文件列表 | `File.listFiles()` | PROPFIND 请求，XML 解析 | GET `?list-type=2`，XML 解析 |
| 读取 | `File.readText()` | GET 请求 | GET 请求 |
| 写入 | `File.writeText()` | PUT 请求 | PUT 请求（带 SHA-256 校验和） |
| 删除 | `File.delete()` | DELETE 请求 | DELETE 请求 |
| 重命名 | `File.renameTo()` | MOVE 请求 | PUT（复制）+ DELETE（删除） |
| 认证 | 无 | Basic Auth | AWS4-HMAC-SHA256 签名 |
| 附件支持 | 完整（本地 `assets/` 子目录） | 可选（同路径 `assets/` 子目录） | 可选（同前缀 `assets/` 子目录） |
| 目录创建 | `File.mkdirs()` | MKCOL 请求 | 零长度对象（S3 无目录概念） |

##### StorageManager — 多库管理器

- 通过 SharedPreferences（JSON 数组）持久化笔记库列表
- 工厂方法 `createStorage(config: StorageConfig): NoteStorage` 根据配置类型创建对应实现
- 管理当前激活的笔记库 ID

#### 附件系统 (data/attachment/)

##### AttachmentEntry

```kotlin
data class AttachmentEntry(
    val attachId: String,     // UUID，附件唯一标识
    val originalName: String, // 原始文件名
    val storedName: String,   // 存储文件名（SHA-256 哈希 + 扩展名）
    val mimeType: String,     // MIME 类型
    val size: Long,           // 文件大小（字节）
    val noteId: String,       // 所属笔记 ID
    val createdAt: Date,      // 创建时间
    val checksum: String      // SHA-256 校验和
)
```

##### AttachmentManager

- **存储位置**：笔记目录下的 `assets/` 子目录
- **去重机制**：通过 SHA-256 哈希命名，相同内容文件只存储一份
- **引用生成**：`getMarkdownReference(entry)` 根据文件类型生成 `![name](path)` 或 `[name](path)`
- **完整性检查**：`checkReferences(noteId, markdownText)` 解析正文中所有引用，与附件索引比对，返回 `AttachmentCheckResult`
- **孤立清理**：`cleanOrphanAttachments(noteId, markdownText)` 删除未被引用的附件文件
- **元数据索引**：附件元数据以 JSON 数组形式存储在 SharedPreferences 中

#### UI 组件 (ui/components/)

##### WysiwygEditor — 所见即所得编辑器

编辑器核心，详见[编辑器详解](#编辑器详解)章节。

##### MarkdownText — Markdown 渲染组件

用于在列表预览、设置页面等只读场景渲染 Markdown。

- 基于 commonmark-java 解析器
- 递归遍历 AST 节点树进行 Compose 渲染
- 支持的块级节点：Heading、Paragraph、BulletList、OrderedList、FencedCodeBlock、BlockQuote、ThematicBreak
- 支持的行内节点：Emphasis、StrongEmphasis、Code、Link、SoftLineBreak、HardLineBreak
- 自定义样式：链接用主色、代码用等宽字体+背景色、引用用斜体+左侧竖线

##### MarkdownToolbar — 语法工具栏

- 19 种 `MarkdownSyntax` 枚举值，每种定义 `prefix`、`suffix`、`displayName`、`newLine`、`group`
- 横向可滚动 `Row`，每个按钮显示一个简短标签
- 点击按钮调用 `viewModel.insertMarkdownSyntax(syntax)`
- 设置按钮打开 `ToolbarSettingsDialog`

语法按钮分组：

| 分组 | 按钮 |
|------|------|
| 标题 | H1, H2, H3, H4, H5, H6 |
| 文本样式 | 粗体, 斜体, 删除线 |
| 代码 | 行内代码, 代码块 |
| 块元素 | 引用, 水平线 |
| 列表 | 无序列表, 有序列表, 任务列表 |
| 链接/图片 | 链接, 图片 |
| 扩展 | 表格, 脚注 |

##### ToolbarSettingsDialog — 工具栏自定义对话框

- 按分组展示所有语法项，每个带复选框
- 支持按分组全选/取消
- 保存到 `ToolbarSettingsRepository`

#### 页面 (ui/screens/)

##### NoteListScreen — 笔记列表页

核心首页，功能要点：

- **列表展示**：`LazyColumn`（手机）或 `LazyVerticalGrid`（平板 ≥ 600dp），按更新时间倒序
- **卡片内容**：标题（粗体）、纯文字预览（最多 3 行）、底部行（相对时间 + 标签）
- **搜索栏**：顶部展开式搜索框，搜索标题和正文内容
- **侧边抽屉**：笔记库切换、标签列表、新建笔记库、设置入口
- **多选模式**：长按卡片进入多选，支持全选、批量删除
- **下拉刷新**：自定义旋转同步图标指示器，带下拉偏移动画
- **生命周期**：`ON_RESUME` 时自动刷新列表

##### EditNoteScreen — 笔记编辑页

功能要点：

- **标题栏**：`BasicTextField`，20sp，带占位提示"标题"
- **标签芯片**：标题下方水平排列，点击弹出标签编辑对话框
- **正文编辑器**：`WysiwygEditor` 占满剩余空间
- **底部工具栏**：正文获得焦点时显示 `MarkdownToolbar`
- **顶部操作**：保存（返回）、附件选择器、图片选择器、分享、更多菜单
- **更多菜单**：编辑标签、导出 Markdown、附件管理、引用完整性检查、清理孤立附件、删除笔记
- **下拉展开**：顶部下拉可展开标题栏区域
- **自动保存**：返回时自动保存，`DisposableEffect` 的 `onDispose` 兜底保存
- **返回拦截**：`BackHandler` 保存后返回

##### SettingsScreen — 设置页

- 主题模式选择（浅色 / 深色 / 跟随系统）
- 平板列数设置（2 列 / 3 列）
- 版本信息显示

##### StorageSettingsScreen — 存储配置页

- 存储类型选择（本地 / WebDAV / S3）
- 类型特定的配置字段（URL、路径、认证凭证等）
- 本地存储：路径输入 + 系统文件夹选择器
- WebDAV / S3：支持复用已有笔记库的配置
- 连接测试按钮
- 库名预览（根据配置自动推导）

#### ViewModel 层 (ui/viewmodel/)

##### NoteListViewModel

```
StateFlow 暴露:
  notes: List<Note>          — 当前显示的笔记列表
  tags: List<String>          — 所有标签
  searchQuery: String         — 搜索关键词
  filterTag: String?          — 当前标签筛选
  libraries: List<NoteLibrary>— 所有笔记库
  activeLibrary: NoteLibrary? — 当前激活的笔记库
  totalNoteCount: Int         — 总笔记数
  totalAttachmentCount: Int   — 总附件数

关键方法:
  refresh()                   — 重新加载
  switchLibrary(id)           — 切换笔记库
  setSearchQuery(query)       — 设置搜索词
  setFilterTag(tag)           — 按标签筛选
  addLibrary(lib)             — 添加笔记库
  deleteNote(id)              — 删除笔记
  deleteNotes(ids)            — 批量删除
```

##### EditNoteViewModel

```
StateFlow 暴露:
  title: String                           — 笔记标题
  contentTextField: TextFieldValue        — 正文（含光标位置）
  tags: List<String>                      — 标签列表
  attachments: List<AttachmentEntry>      — 附件列表
  focusedBlockIndex: Int                  — 当前聚焦的块索引

关键方法:
  loadNote(context, noteId)               — 加载或初始化笔记
  onTitleChange(newTitle)                 — 标题变更
  onContentChange(newContent)             — 正文变更（全局 TextFieldValue）
  onBlockContentChange(index, text, cursorOffset) — 块内容变更（带光标位置）
  insertMarkdownSyntax(syntax)            — 在光标处插入语法
  insertImage(context, uri)               — 插入图片附件
  insertFileReference(context, uri)       — 插入文件附件
  saveNote(context)                       — 保存笔记（防抖）
  deleteNote(context)                     — 删除笔记
  getShareIntent(context)                 — 获取系统分享 Intent
  getExportMarkdown()                     — 获取导出文本
  deleteAttachment(context, entry)        — 删除附件
  checkAttachmentIntegrity(context)       — 检查引用完整性
  cleanOrphanAttachments(context)         — 清理孤立附件
```

##### ThemeViewModel

- 持久化主题模式偏好到 SharedPreferences
- 暴露 `themeMode: StateFlow<ThemeMode>`（LIGHT / DARK / SYSTEM）
- 方法：`setThemeMode(mode)`

##### StorageSettingsViewModel

- 管理单个笔记库的存储配置编辑
- 方法：`loadLibrary(id)`、`updateConfig(config)`、`testConnection()`、`save()`

#### 主题 (ui/theme/)

##### Theme.kt

- 定义 `LightColors` 和 `DarkColors`（Material 3 `lightColorScheme` / `darkColorScheme`）
- 主色调：Teal（青绿色），辅以蓝灰色系
- 表面/背景使用低饱和度灰色
- 深色模式：近黑背景（`#121212`），表面色（`#1E1E1E`）
- `SharpShapes`：小圆角（2dp）、中圆角（4dp）
- `EasyMDTheme` 组合函数：包装 MaterialTheme，根据 `darkTheme` 参数切换配色

##### Type.kt

自定义 `Typography`，覆盖以下字体样式：

| 样式 | 字号 | 用途 |
|------|------|------|
| headlineLarge | 28sp | — |
| headlineMedium | 24sp | — |
| titleLarge | 22sp | — |
| titleMedium | 18sp | — |
| bodyLarge | 16sp | 正文，行高 24sp |
| bodyMedium | 14sp | — |
| labelMedium | 12sp | — |

---

## 编辑器详解

`WysiwygEditor` 是本项目的核心组件，采用**块级所见即所得**的设计。

### 块划分

编辑器通过 `parseBlocks()` 函数将 Markdown 正文按空行切分为独立的"块"：

```
输入文字通过空行分割为块。
空行是块的自然边界。

代码块作为整体——即使内部有空行也不拆分。
```

每个块通过 `detectBlockType()` 识别类型：

| 检测规则 | 块类型 |
|---------|--------|
| `#{1,6} 开头` | HEADING (H1~H6) |
| 默认 | PARAGRAPH |
| `- ` 或 `* ` 开头 | UNORDERED_LIST |
| `数字. 开头` | ORDERED_LIST |
| `- [ ] 开头` | TASK_LIST_TODO |
| `- [x] 开头` | TASK_LIST_DONE |
| `> 开头` | BLOCKQUOTE |
| ` ``` 开头` | CODE_BLOCK（围栏到闭合为一整块） |
| `---` / `***` / `___` | HORIZONTAL_RULE |

### 所见即所得渲染

所有块始终以 `BasicTextField` 呈现（不再区分"编辑/预览"模式），并按块类型应用不同的视觉样式：

| 块类型 | 文字样式 | 装饰 |
|--------|---------|------|
| H1 | 28sp Bold | — |
| H2 | 24sp Bold | — |
| H3 | 22sp Bold | — |
| H4 | 20sp Bold | — |
| H5~H6 | 18sp Bold | — |
| PARAGRAPH | 16sp 常规 | — |
| CODE_BLOCK | 14sp 等宽字体 | 灰色背景 + 圆角卡片 |
| BLOCKQUOTE | 16sp 斜体 | 左侧 3dp 竖线 |
| 列表类 | 16sp 常规 | — |

### 语法高亮转换

`MarkdownEditorVisualTransformation` 实现了 Compose 的 `VisualTransformation` 接口，在编辑过程中实时高亮 Markdown 语法：

- **块标记**：`#`、`- `、`> `、`1. ` 等前缀以强调色（secondary）显示
- **行内标记**：`**`、`*`、`~~`、反引号等语法包装符号以强调色显示
- **内容样式**：粗体标记之间的文字获得 `FontWeight.Bold`，斜体获得 `FontStyle.Italic`，删除线获得 `TextDecoration.LineThrough`
- **代码**：行内代码内容获得等宽字体 + 背景色
- **链接**：`[]()` 括号以强调色显示
- **优先级**：代码段内的语法标记不被二次解析

### 工具栏语法插入

点击底部工具栏按钮 → `ViewModel.insertMarkdownSyntax(syntax)`：

1. 读取当前 `TextFieldValue`（含全局光标位置）
2. 根据选中文本情况组合 `prefix + selectedText + suffix`
3. 在光标处插入，光标自动定位到 `prefix` 之后（有选中文本时定位到末尾）
4. 更新 `contentTextField`，触发编辑器重绘
5. `WysiwygEditor` 解析出光标所在块，同步块文本和光标位置

### 标题自动空格

`applyHeadingAutoSpace()` 在每次文本变更时检查：若某行出现 `#{1,6}后直接跟非空格字符`，自动在 `#` 号和内容之间插入空格，并调整光标位置。

### 焦点管理

- 每个块有独立的 `FocusRequester`
- 点击块时通过 `onFocusChanged` 获取焦点，通知 ViewModel 显示底部工具栏
- 点击底部空白区域通过 `FocusManager.clearFocus()` 取消焦点，隐藏工具栏
- 标题键盘"下一步"通过 `contentFocusTrigger` 计数器机制触发第一个块的焦点请求
- 块间焦点切换不产生中间"无焦点"状态，避免工具栏闪烁

---

## 数据格式

### 笔记文件结构

每篇笔记保存为一个独立的 `.md` 文件，文件头包含 YAML Front Matter：

```markdown
---
id: 550e8400-e29b-41d4-a716-446655440000
title: Kotlin 学习笔记
createdAt: 1714500000000
updatedAt: 1714586400000
tags: Kotlin,Android,编程
---

# Kotlin 基础

Kotlin 是一种在 **Java 虚拟机**上运行的静态类型编程语言。

## 变量声明

- `val` — 只读变量
- `var` — 可变变量

## 函数定义

```kotlin
fun sum(a: Int, b: Int): Int {
    return a + b
}
```

> Kotlin 让 Android 开发更加简洁和安全。
```

**YAML 头部字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID | 笔记唯一标识（也是文件名） |
| `title` | String | 笔记标题 |
| `createdAt` | Long | 创建时间（Unix 毫秒时间戳） |
| `updatedAt` | Long | 最后更新时间（Unix 毫秒时间戳） |
| `tags` | String | 标签列表（逗号分隔） |

正文部分为标准 Markdown 格式，可直接用任何文本编辑器打开。

### 附件索引

附件元数据以 JSON 数组形式存储在 SharedPreferences 键 `attachments_index` 中：

```json
[
  {
    "attachId": "uuid-1",
    "originalName": "photo.jpg",
    "storedName": "abc123def456.jpg",
    "mimeType": "image/jpeg",
    "size": 204800,
    "noteId": "note-uuid",
    "createdAt": 1714500000000,
    "checksum": "sha256hex..."
  }
]
```

### 笔记库配置

笔记库列表以 JSON 数组形式存储在 SharedPreferences 键 `storage_libraries` 中：

```json
[
  {
    "id": "library-uuid",
    "storageConfig": {
      "type": "WEBDAV",
      "webdavUrl": "https://dav.example.com",
      "webdavUsername": "user",
      "webdavPassword": "pass",
      "webdavPath": "/notes/"
    }
  }
]
```

---

## 构建配置

### 环境要求

| 工具 | 版本要求 |
|------|---------|
| Android Studio | Hedgehog (2023.1.1) 或更高 |
| JDK | 17 |
| Kotlin | 2.0.21 |
| Gradle | 8.11.1 |
| Android SDK | API 36 (编译) / API 26+ (运行) |
| Gradle Plugin | 8.7.3 |

### 依赖列表

| Group:Artifact | 版本 | 说明 |
|------|------|------|
| `androidx.core:core-ktx` | 1.12.0 | Android 核心 Kotlin 扩展 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.7.0 | 生命周期协程支持 |
| `androidx.activity:activity-compose` | 1.8.2 | Compose Activity 集成 |
| `androidx.compose:compose-bom` | 2024.12.01 | Compose 版本统一管理 |
| `androidx.compose.ui:ui` | (BOM) | Compose UI 基础 |
| `androidx.compose.ui:ui-graphics` | (BOM) | Compose 图形 |
| `androidx.compose.ui:ui-tooling-preview` | (BOM) | IDE 预览支持 |
| `androidx.compose.material3:material3` | (BOM) | Material Design 3 组件 |
| `androidx.compose.material:material-icons-extended` | (BOM) | 扩展 Material 图标 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.7.0 | ViewModel + Compose 集成 |
| `androidx.navigation:navigation-compose` | 2.7.7 | Compose 导航 |
| `org.commonmark:commonmark` | 0.21.0 | CommonMark Markdown 解析器 |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP 客户端（WebDAV / S3） |
| `org.jsoup:jsoup` | 1.17.2 | HTML 解析（WebDAV XML 响应） |
| `androidx.documentfile:documentfile` | 1.0.1 | Document URI 工具 |

**测试依赖**：

| Group:Artifact | 版本 | 说明 |
|------|------|------|
| `junit:junit` | 4.13.2 | 单元测试 |
| `androidx.test.ext:junit` | 1.1.5 | Android 测试 |
| `androidx.test.espresso:espresso-core` | 3.5.1 | UI 测试 |
| `androidx.compose.ui:ui-test-junit4` | (BOM) | Compose UI 测试 |
| `androidx.compose.ui:ui-tooling` | (BOM) | Debug Compose 工具 |
| `androidx.compose.ui:ui-test-manifest` | (BOM) | 测试清单 |

### 构建命令

```bash
# 克隆项目
git clone <repository-url>
cd easymd

# Android Studio 打开项目目录，等待 Gradle 同步后运行

# 命令行构建 Debug 版本
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 命令行构建 Release 版本
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk

# 安装到模拟器
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb -s emulator-5554 shell am start -n com.easymd.ywy/com.easymd.MainActivity
```

---

## 近期修复

### v0.1.1 — 安全与稳定性修复

| 类别 | 修复内容 | 涉及文件 |
|------|---------|---------|
| 🔴 并发安全 | `uuidToFileName` 改用 `ConcurrentHashMap`，消除多协程并发 CRUD 时的 `ConcurrentModificationException` | `NoteRepository.kt` |
| 🔴 映射失效 | `switchStorage()` 现在同时更新 `activeLibraryId` / `fileNamePrefs` / `attachmentManager`，切换笔记库后不再映射到旧库 | `NoteRepository.kt` |
| 🔴 附件泄漏 | `deleteNote()` 先逐条调用 `storage.deleteAttachment()` 清理远程附件，再清理本地附件索引 | `NoteRepository.kt` |
| 🔴 XXE 注入 | WebDAV XML 解析禁用 DOCTYPE 声明和外部实体，防止恶意服务器 SSRF/本地文件读取 | `WebDAVStorage.kt` |
| 🔴 签名断裂 | AWS SigV4 `canonicalHeaders` 按 header 名称 ASCII 排序，新增 header 后签名不再失效 | `S3Storage.kt` |
| 🟡 YAML 解析 | Front Matter `---` 闭合边界改用精确匹配，支持 `tags: [tag1, "tag2"]` 和 `tags: tag1,tag2` 两种格式 | `NoteRepository.kt` |
| 🟡 代码块兼容 | 结束符检测允许 fence 长度 ≥ 起始符（GFM 规范），` ```` ` 可闭合 `` ``` `` | `WysiwygEditor.kt` |
| 🟡 标题自动空格 | 从仅修复第一个匹配改为修复全文所有 `#标题` 行，光标偏移正确处理 | `WysiwygEditor.kt` |
| 🟡 S3 编码 | `encodeS3` 空格编码从 `+` 改为 `%20`，兼容非标准 S3 实现 | `S3Storage.kt` |
| 🟡 URL 构造 | 新增 `s3Host`/`s3PathPrefix` 属性，支持 endpoint 含路径前缀的场景 | `S3Storage.kt` |

---

## 开发路线图

- [x] 笔记 CRUD（创建、读取、更新、删除）
- [x] YAML Front Matter 文件格式
- [x] Material Design 3 + Compose 界面
- [x] MVVM 架构 + StateFlow 状态管理
- [x] CommonMark 规范解析与渲染
- [x] 块级 WYSIWYG 编辑器
- [x] Markdown 语法实时高亮
- [x] 标题自动空格
- [x] 语法工具栏（19 种快捷按钮）
- [x] 工具栏自定义
- [x] 多笔记库管理
- [x] 本地 / WebDAV / S3 三后端存储
- [x] 标签系统
- [x] 全文搜索
- [x] 附件管理（插入、去重、完整性检查、孤立清理）
- [x] 分享与导出
- [x] 深色模式（浅色 / 深色 / 跟随系统）
- [x] 平板适配（网格布局 + 可调列数）
- [x] 多选批量删除
- [ ] 自动同步（定时 / 手动触发）
- [ ] 冲突解决策略
- [ ] 笔记加密
- [ ] Markdown 预览模式（纯净排版视图）
- [ ] 撤消/重做（Undo/Redo）
- [ ] 滑动操作（左滑删除/归档）
- [ ] 笔记模板
- [ ] 笔记统计（字数、阅读时间）
- [ ] Widget 桌面小部件
- [ ] 单元测试覆盖

---

## 许可证

本项目仅供学习和参考使用。
