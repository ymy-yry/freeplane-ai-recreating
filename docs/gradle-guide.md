# Freeplane 项目 Gradle 构建指南

> 本文档参考 [Gradle 官方文档](https://docs.gradle.org/current/userguide/userguide.html) 编写，专门针对 Freeplane 项目的构建配置和使用方式。

---

## 目录

- [1. 快速开始](#1-快速开始)
- [2. 项目结构](#2-项目结构)
- [3. 构建基础](#3-构建基础)
- [4. 依赖管理](#4-依赖管理)
- [5. 多项目构建](#5-多项目构建)
- [6. 插件系统](#6-插件系统)
- [7. 构建任务](#7-构建任务)
- [8. 性能优化](#8-性能优化)
- [9. 常见问题](#9-常见问题)
- [10. 最佳实践](#10-最佳实践)

---

## 1. 快速开始

### 1.1 环境要求

- **Gradle 版本**: `8.14`（锁定版本）
- **Java 版本**: 
  - 核心模块：Java 8
  - AI 插件：Java 17
  - 推荐运行时：Java 21（Zulu JDK）

### 1.2 安装与验证

**检查 Gradle 版本**：

```bash
# 使用 Gradle Wrapper（推荐）
./gradlew --version

# 输出示例：
# Gradle 8.14
# Build time:   2024-xx-xx
# Revision:     xxx
# JVM:          21.0.5 (Azul Systems, Inc. 21.0.5+1-LTS)
# OS:           Windows 11 10.0 amd64
```

**验证 Java 环境**：

```bash
java -version
javac -version
```

### 1.3 首次构建

```bash
# Windows PowerShell
.\gradlew.bat build

# Linux/macOS
./gradlew build
```

首次构建会自动下载 Gradle 8.14 和所有依赖项，可能需要几分钟时间。

---

## 2. 项目结构

### 2.1 多模块架构

Freeplane 采用 Gradle 多项目构建模式，包含 15 个子模块：

```
freeplane-1.13.x/
├── build.gradle              # 根构建脚本
├── settings.gradle           # 模块声明
├── gradle.properties         # Gradle 属性配置
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties  # Wrapper 配置
│
├── freeplane/                # 核心应用模块
├── freeplane_api/            # 公共 API 接口
├── freeplane_framework/      # 框架基础
├── freeplane_mac/            # macOS 适配
│
├── freeplane_plugin_ai/           # AI 插件 ⭐
├── freeplane_plugin_script/       # 脚本插件
├── freeplane_plugin_markdown/     # Markdown 支持
├── freeplane_plugin_latex/        # LaTeX 公式
├── freeplane_plugin_formula/      # 公式编辑器
├── freeplane_plugin_svg/          # SVG 支持
├── freeplane_plugin_jsyntaxpane/  # 代码高亮
├── freeplane_plugin_codeexplorer/ # 代码浏览器
├── freeplane_plugin_bugreport/    # 错误报告
└── freeplane_plugin_openmaps/     # 开放地图
```

### 2.2 模块依赖关系

```
┌─────────────────────────────────────┐
│     插件模块 (Plugin Layer)          │
│  freeplane_plugin_*                 │
│  - 依赖 freeplane                    │
│  - 依赖 freeplane_api                │
│  - 可选依赖其他插件                   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│     核心模块 (Core Layer)            │
│  freeplane                          │
│  - 依赖 freeplane_api                │
│  - 依赖 freeplane_framework          │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│     API 层 (API Layer)               │
│  freeplane_api                      │
│  - 无内部依赖                        │
│  - 定义公共接口                      │
└─────────────────────────────────────┘
```

### 2.3 输出目录

| 目录 | 用途 | 说明 |
|------|------|------|
| `BIN/` | 运行时文件 | 包含启动脚本、JAR、插件 |
| `BUILD/` | 构建产物 | 编译输出的 JAR 文件 |
| `DIST/` | 分发包 | Windows/Mac/Linux 安装包 |
| `.gradle/` | 缓存 | Gradle 缓存和守护进程 |

---

## 3. 构建基础

### 3.1 核心概念

#### Project（项目）

在 Gradle 中，每个构建由一个或多个 **Project** 组成。在 Freeplane 中：

- **根项目**: `freeplane_root`
- **子项目**: 15 个模块（如 `freeplane`、`freeplane_plugin_ai`）

#### Task（任务）

**Task** 是构建的基本工作单元。常见的任务包括：

```bash
# 编译 Java 源代码
.\gradlew.bat compileJava

# 运行测试
.\gradlew.bat test

# 打包 JAR
.\gradlew.bat jar

# 完整构建（编译 + 测试 + 打包）
.\gradlew.bat build

# 清理构建产物
.\gradlew.bat clean
```

#### Configuration（配置）

**Configuration** 用于管理依赖。Freeplane 定义了以下配置：

```groovy
configurations {
    lib           // 插件运行时依赖
    maclib        // macOS 专用依赖
    api.extendsFrom(lib)    // api 继承 lib
    api.extendsFrom(maclib) // api 继承 maclib
}
```

### 3.2 构建生命周期

Gradle 构建分为三个阶段：

1. **Initialization（初始化）**: 解析 `settings.gradle`，确定哪些项目参与构建
2. **Configuration（配置）**: 执行所有 `build.gradle`，构建任务图
3. **Execution（执行）**: 按照依赖关系执行任务

**示例**：

```bash
# 查看构建生命周期详情
.\gradlew.bat build --info

# 查看任务执行顺序
.\gradlew.bat build --dry-run
```

### 3.3 常用构建命令

#### 编译相关

```bash
# 编译所有模块
.\gradlew.bat compileJava

# 编译特定模块
.\gradlew.bat :freeplane:compileJava
.\gradlew.bat :freeplane_plugin_ai:compileJava

# 编译并跳过测试
.\gradlew.bat build -x test
```

#### 测试相关

```bash
# 运行所有测试
.\gradlew.bat test

# 运行特定模块测试
.\gradlew.bat :freeplane:test

# 详细测试日志（显示失败原因）
.\gradlew.bat test -PTestLoggingFull

# 运行单个测试类
.\gradlew.bat :freeplane_plugin_ai:test --tests "ChatRequestFlowTest"
```

#### 打包相关

```bash
# 打包所有 JAR
.\gradlew.bat jar

# 构建分发包
.\gradlew.bat dist           # 所有平台
.\gradlew.bat win.dist       # Windows 安装包
.\gradlew.bat mac.dist       # macOS DMG
.\gradlew.bat linux-packages # Linux DEB
```

### 3.4 构建输出示例

```
> Task :freeplane_plugin_ai:compileJava
注: 某些输入文件使用或覆盖了已过时的 API。
注: 有关详细信息，请使用 -Xlint:deprecation 重新编译。

> Task :freeplane_plugin_ai:test
ChatRequestFlowTest > modelResponseModeShowsLatestUsage PASSED

> Task :freeplane_plugin_ai:pluginJar
> Task :freeplane_plugin_ai:jar
> Task :freeplane_plugin_ai:copyOSGiJars
> Task :freeplane_plugin_ai:copyOSGiManifest
> Task :freeplane_plugin_ai:copyOSGiConfig
> Task :freeplane_plugin_ai:build

BUILD SUCCESSFUL in 45s
23 actionable tasks: 23 executed
```

---

## 4. 依赖管理

### 4.1 仓库配置

Freeplane 配置了多个依赖仓库：

```groovy
repositories {
    // Maven 中央仓库
    mavenCentral()
    
    // OSGi Knopflerfish 框架仓库
    maven { 
        url 'https://resources.knopflerfish.org/repo/maven2/release/' 
    }
    
    // 本地 Gradle 缓存
    maven { 
        url "${project.gradle.gradleUserHomeDir}/local-artifacts" 
    }
    
    // 本地 JAR 目录（flat directory）
    flatDir name: 'localGitDepsRepository',
            dirs: [
                rootDir.path + "/freeplane/lib",
                rootDir.path + "/freeplane_plugin_jsyntaxpane/lib"
            ]
}
```

**仓库搜索顺序**：按声明顺序查找，找到即停止。

### 4.2 依赖声明

#### 标准依赖配置

```groovy
dependencies {
    // 编译和运行时都需要（API 暴露）
    api 'org.example:library:1.0.0'
    
    // 仅内部使用（不暴露给使用者）
    implementation 'org.example:library:1.0.0'
    
    // 仅编译时需要
    compileOnly 'org.example:library:1.0.0'
    
    // 仅运行时需要
    runtimeOnly 'org.example:library:1.0.0'
    
    // 测试依赖
    testImplementation 'junit:junit:4.13.2'
}
```

#### Freeplane 自定义依赖

```groovy
dependencies {
    // 项目模块依赖
    implementation project(':freeplane')
    implementation project(':freeplane_plugin_markdown')
    
    // 插件依赖库（自定义配置）
    lib 'dev.langchain4j:langchain4j:1.11.0'
    lib 'dev.langchain4j:langchain4j-open-ai:1.11.0'
}
```

### 4.3 依赖版本管理

#### 版本声明位置

```groovy
// build.gradle - 根项目
ext {
    langchain4jVersion = '1.11.0'
    junitVersion = '4.13.2'
}

// 子项目使用
dependencies {
    implementation "dev.langchain4j:langchain4j:$langchain4jVersion"
}
```

#### 查看依赖树

```bash
# 查看特定模块依赖树
.\gradlew.bat :freeplane_plugin_ai:dependencies

# 查看依赖配置
.\gradlew.bat :freeplane_plugin_ai:dependencies --configuration lib

# 输出示例：
# lib
# +--- dev.langchain4j:langchain4j:1.11.0
# |    +--- dev.langchain4j:langchain4j-core:1.11.0
# |    \--- org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3
# +--- dev.langchain4j:langchain4j-open-ai:1.11.0
# \--- dev.langchain4j:langchain4j-ollama:1.11.0
```

### 4.4 依赖检查

```bash
# 检查依赖更新
.\gradlew.bat :freeplane_plugin_ai:dependencyUpdates

# 安全漏洞扫描
.\gradlew.bat dependencyCheckAnalyze

# 检查重复依赖
.\gradlew.bat :freeplane_plugin_ai:dependencies --scan
```

---

## 5. 多项目构建

### 5.1 项目声明

`settings.gradle` 定义所有子项目：

```groovy
rootProject.name = 'freeplane_root'

include 'freeplane',
        'freeplane_api',
        'freeplane_framework',
        'freeplane_mac',
        'freeplane_ant',
        'freeplane_debughelper',
        'JOrtho_0.4_freeplane',
        'freeplane_plugin_bugreport',
        'freeplane_plugin_formula',
        'freeplane_plugin_latex',
        'freeplane_plugin_markdown',
        'freeplane_plugin_script',
        'freeplane_plugin_svg',
        'freeplane_plugin_jsyntaxpane',
        'freeplane_plugin_codeexplorer',
        'freeplane_plugin_ai'
```

### 5.2 配置作用域

#### allprojects（所有项目）

应用于根项目和所有子项目：

```groovy
allprojects {
    repositories {
        mavenCentral()
    }
    
    ext.globalBin = rootDir.path + '/BIN'
}
```

#### subprojects（子项目）

仅应用于子项目（排除根项目）：

```groovy
subprojects {
    apply plugin: 'java-library'
    apply plugin: 'eclipse'
    apply plugin: 'idea'

    java {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType(JavaCompile) {
        options.encoding = "UTF-8"
    }
}
```

#### configure（特定项目）

选择性配置特定项目：

```groovy
// 配置所有 OSGi 项目
configure(subprojects.findAll { 
    it.name =~ /plugin/ || it.name =~ /^freeplane$/ 
}) {
    apply plugin: 'biz.aQute.bnd.builder'
    
    dependencies {
        implementation 'org.knopflerfish.kf6:framework:8.0.11'
    }
}
```

### 5.3 跨项目依赖

```groovy
// freeplane_plugin_ai/build.gradle
dependencies {
    // 依赖其他模块
    implementation project(':freeplane')
    implementation project(':freeplane_plugin_markdown')
    
    // 外部依赖
    lib 'dev.langchain4j:langchain4j:1.11.0'
}
```

**构建顺序**：Gradle 自动解析依赖顺序，先构建被依赖的项目。

### 5.4 版本管理

所有子项目共享同一版本（定义在根项目）：

```groovy
allprojects {
    Properties props = new Properties()
    props.load(new FileInputStream(
        rootDir.path + '/freeplane/src/viewer/resources/version.properties'
    ))
    
    ext.majorVersion = props['freeplane_version']
    ext.versionStatus = props['freeplane_version_status']
    
    version = ext.majorVersion  // 所有模块统一版本
}
```

---

## 6. 插件系统

### 6.1 Gradle 插件

#### 核心插件

```groovy
plugins {
    id 'java-library'                    // Java 库支持
    id 'eclipse'                         // Eclipse IDE 集成
    id 'idea'                            // IntelliJ IDEA 集成
    id 'biz.aQute.bnd.builder'           // OSGi Bundle 构建
}
```

#### 社区插件

```groovy
plugins {
    id 'com.netflix.nebula.ospackage' version '11.11.2'  // Linux 打包
    id 'org.owasp.dependencycheck' version '12.1.2'       // 安全扫描
    id 'com.github.ben-manes.versions' version '0.52.0'   // 版本检查
}
```

### 6.2 OSGi 插件配置

Freeplane 使用 **Knopflerfish OSGi 框架**，插件需配置为 OSGi Bundle：

```groovy
apply plugin: 'biz.aQute.bnd.builder'

ext {
    pluginid = 'org.freeplane.plugin.ai'
    bundleActivator = 'org.freeplane.plugin.ai.Activator'
    bundleImports = 'io.github.gitbucket.markedj.*'
    bundleExports = 'org.freeplane.plugin.api.*'
}

jar {
    bundle {
        bnd([
            '-savemanifest': 'build/manifest/MANIFEST.MF',
            'Bundle-SymbolicName': pluginid,
            'Export-Package': bundleExports,
            'Bundle-Vendor': 'Freeplane Project',
            'Import-Package': bundleImports,
            'Bundle-Activator': bundleActivator,
            'Bundle-ClassPath': '., lib/plugin-1.13.x.jar'
        ])
    }
}
```

**生成的 MANIFEST.MF 示例**：

```
Manifest-Version: 1.0
Bundle-SymbolicName: org.freeplane.plugin.ai
Bundle-Name: Freeplane AI Plugin
Bundle-Version: 1.13.x
Bundle-Activator: org.freeplane.plugin.ai.Activator
Bundle-ClassPath: .,lib/plugin-1.13.x.jar
Export-Package: org.freeplane.plugin.api
Import-Package: io.github.gitbucket.markedj
Require-Bundle: org.freeplane.core
```

### 6.3 插件打包流程

```
1. 编译 Java 源代码
   ↓
2. 创建 plugin.jar（非 OSGi）
   ↓
3. 创建 OSGi Bundle JAR（包含 MANIFEST.MF）
   ↓
4. 复制到 BIN/plugins/org.freeplane.plugin.ai/
   ├── lib/
   │   ├── plugin-1.13.x.jar
   │   └── langchain4j-1.11.0.jar
   └── META-INF/
       └── MANIFEST.MF
```

### 6.4 AI 插件特殊配置

#### Java 17 编译

```groovy
java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.withType(JavaCompile) {
    options.compilerArgs << '-parameters'  // 保留参数名（LangChain4j 需要）
}
```

#### Bootstrap 模式（兼容 Java 8 OSGi）

```groovy
sourceSets {
    bootstrap {
        java {
            srcDir 'src/bootstrap/java'  // Java 8 引导代码
        }
        compileClasspath += sourceSets.main.compileClasspath
    }
}

// Bootstrap 编译为 Java 8
tasks.named('compileBootstrapJava', JavaCompile) {
    options.release = 8
}

// 主代码编译为 Java 17
tasks.named('compileJava', JavaCompile) {
    options.release = 17
}

// JAR 包含两个源码集
tasks.named('jar', Jar) {
    from(sourceSets.bootstrap.output)
}
```

**设计原因**：

- OSGi 框架需要 Java 8 的 `BundleActivator`
- AI 功能需要 Java 17 + LangChain4j
- Bootstrap 模式实现向后兼容

#### 字节码验证

```groovy
task verifyAiPluginBytecodeLevels {
    dependsOn tasks.named('classes')
    doLast {
        // 验证 Bootstrap 为 Java 8 (major version 52)
        verifyClassMajorVersion(
            sourceSets.bootstrap.output.classesDirs,
            'org/freeplane/plugin/ai/bootstrap/Java8BootstrapActivator.class',
            52
        )
        
        // 验证主代码为 Java 17 (major version 61)
        verifyClassMajorVersion(
            sourceSets.main.output.classesDirs,
            'org/freeplane/plugin/ai/Activator.class',
            61
        )
    }
}

check.dependsOn verifyAiPluginBytecodeLevels
```

---

## 7. 构建任务

### 7.1 任务类型

#### 内置任务

| 任务 | 描述 | 示例 |
|------|------|------|
| `compileJava` | 编译 Java 源代码 | `.\gradlew.bat compileJava` |
| `processResources` | 复制资源文件 | `.\gradlew.bat processResources` |
| `classes` | 编译代码 + 处理资源 | `.\gradlew.bat classes` |
| `jar` | 打包 JAR 文件 | `.\gradlew.bat jar` |
| `test` | 运行单元测试 | `.\gradlew.bat test` |
| `build` | 完整构建 | `.\gradlew.bat build` |
| `clean` | 清理构建产物 | `.\gradlew.bat clean` |

#### 自定义任务

```groovy
// 简单任务
task hello {
    doLast {
        println 'Hello, Freeplane!'
    }
}

// 带依赖的任务
task buildAndCopy {
    dependsOn 'build'
    doLast {
        copy {
            from 'build/libs'
            into 'BIN'
        }
    }
}

// 类型化任务
task copyOSGiJars(type: Copy) {
    from ("$buildDir/libs")
    from (configurations.lib)
    into(globalBin + '/plugins/org.freeplane.plugin.ai/lib/')
}
```

### 7.2 任务依赖

```groovy
// 声明依赖关系
jar.dependsOn cleanBUILD
build.dependsOn copyOSGiJars
build.dependsOn copyOSGiManifest
build.dependsOn copyOSGiConfig

// 执行顺序控制
dist.mustRunAfter clean
createGitTag.mustRunAfter dist
```

**查看任务依赖图**：

```bash
.\gradlew.bat :freeplane_plugin_ai:taskTree build
```

### 7.3 任务分组

```groovy
task verifyAiPluginManifest {
    group = 'verification'
    description = 'Verifies the AI plugin OSGi manifest'
    
    dependsOn tasks.named('jar')
    doLast {
        // 验证逻辑
    }
}
```

**查看所有任务**：

```bash
.\gradlew.bat tasks
.\gradlew.bat tasks --all  # 包含隐藏任务
```

### 7.4 分发任务

```groovy
// dist.gradle
task dist {}

dist {
    dependsOn binZip
    dependsOn srcTarGz
    dependsOn srcpureTarGz
    dependsOn windowsPortableInstaller
    dependsOn gitinfoDist
    dependsOn historyDist
    dependsOn freeplaneDeb
}

# 执行分发
.\gradlew.bat dist
```

**分发输出**：

```
DIST/
├── freeplane-1.13.x-windows-setup.exe
├── freeplane-1.13.x.zip
├── freeplane-1.13.x.tar.gz
├── freeplane-1.13.x-src.zip
└── freeplane_1.13.x_amd64.deb
```

---

## 8. 性能优化

### 8.1 Gradle 属性配置

`gradle.properties`：

```properties
# JVM 内存配置
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError

# 并行构建（多核加速）
org.gradle.parallel=true

# 构建缓存（复用构建结果）
org.gradle.caching=true

# Daemon 守护进程（保持 JVM 运行）
org.gradle.daemon=true
org.gradle.daemon.idletimeout=10800000  # 3 小时
```

### 8.2 并行构建

**原理**：同时构建无依赖关系的模块。

```bash
# 启用并行构建（已配置）
.\gradlew.bat build --parallel

# 指定并行工作线程数
.\gradlew.bat build --parallel=4
```

**效果对比**：

```
# 串行构建
BUILD SUCCESSFUL in 2m 30s

# 并行构建（8 核）
BUILD SUCCESSFUL in 1m 15s  # 速度提升 50%
```

### 8.3 构建缓存

**原理**：缓存任务输出，避免重复构建。

```bash
# 启用缓存（已配置）
.\gradlew.bat build

# 清理缓存
.\gradlew.bat clean

# 查看缓存命中率
.\gradlew.bat build --info | grep "cache"
```

**缓存位置**：

```
.gradle/
└── caches/
    ├── 8.14/
    │   ├── executionHistory/  # 任务执行历史
    │   └── transforms/        # 依赖转换缓存
    └── build-cache-1/         # 构建缓存
```

### 8.4 增量编译

Gradle 自动启用增量编译：

```groovy
tasks.withType(JavaCompile) {
    options.encoding = "UTF-8"
    // 增量编译自动启用
}
```

**效果**：

```
# 首次构建
> Task :freeplane_plugin_ai:compileJava
Compiling with JDK Java compiler API.

# 修改一个文件后
> Task :freeplane_plugin_ai:compileJava
Incremental compilation of 1 classes
```

### 8.5 性能分析

```bash
# 构建性能分析
.\gradlew.bat build --profile

# 生成报告
open build/reports/profile/profile-20240101T120000.html
```

**报告内容**：

- 任务执行时间分布
- 依赖解析耗时
- 配置阶段耗时

---

## 9. 常见问题

### 9.1 构建失败排查

#### 问题：无法确定 Java 版本

```
FAILURE: Build failed with an exception.

* What went wrong:
Could not determine java version from '21.0.5'.
```

**解决方案**：

```bash
# 1. 检查 JAVA_HOME
echo $env:JAVA_HOME  # PowerShell
echo %JAVA_HOME%     # CMD

# 2. 设置 JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"

# 3. 验证
java -version
.\gradlew.bat --version
```

#### 问题：构建缓存损坏

```
FAILURE: Build failed with an exception.

* What went wrong:
Build cache is corrupted.
```

**解决方案**：

```bash
# 1. 停止 Gradle Daemon
.\gradlew.bat --stop

# 2. 删除缓存
Remove-Item -Recurse -Force .gradle/caches

# 3. 重新构建
.\gradlew.bat build
```

#### 问题：文件被占用无法清理

```
> Task :cleanBUILD FAILED
Unable to delete directory 'C:\...\BIN'
```

**解决方案**：

```bash
# 1. 关闭 Freeplane 程序
# 2. 关闭占用文件的程序
# 3. 重试清理
.\gradlew.bat clean
```

#### 问题：Daemon 不兼容

```
An incompatibility was found between Gradle 8.14 and the Java 17 daemon.
```

**解决方案**：

```bash
# 停止所有 Daemon
.\gradlew.bat --stop

# 使用正确 JDK 重启
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"
.\gradlew.bat build
```

### 9.2 依赖问题

#### 问题：依赖下载失败

```
Could not resolve dev.langchain4j:langchain4j:1.11.0.
```

**解决方案**：

```bash
# 1. 检查网络连接
ping repo1.maven.org

# 2. 配置代理（如需要）
# gradle.properties
systemProp.http.proxyHost=proxy.example.com
systemProp.http.proxyPort=8080

# 3. 刷新依赖
.\gradlew.bat build --refresh-dependencies
```

#### 问题：依赖冲突

```
Conflict between dev.langchain4j:langchain4j-core:1.11.0 and 1.10.0
```

**解决方案**：

```bash
# 1. 查看依赖树
.\gradlew.bat :freeplane_plugin_ai:dependencies

# 2. 排除冲突依赖
dependencies {
    implementation('org.example:library:1.0.0') {
        exclude group: 'dev.langchain4j', module: 'langchain4j-core'
    }
}

# 3. 强制统一版本
configurations.all {
    resolutionStrategy {
        force 'dev.langchain4j:langchain4j-core:1.11.0'
    }
}
```

### 9.3 OSGi 问题

#### 问题：Bundle 激活失败

```
org.osgi.framework.BundleException: Unable to resolve org.freeplane.plugin.ai
```

**解决方案**：

```bash
# 1. 检查 MANIFEST.MF
cat BIN/plugins/org.freeplane.plugin.ai/META-INF/MANIFEST.MF

# 2. 验证依赖是否完整
.\gradlew.bat :freeplane_plugin_ai:dependencies --configuration lib

# 3. 重新构建插件
.\gradlew.bat :freeplane_plugin_ai:clean
.\gradlew.bat :freeplane_plugin_ai:build
```

#### 问题：字节码版本不匹配

```
java.lang.UnsupportedClassVersionError: 
org/freeplane/plugin/ai/Activator has been compiled by a more recent 
version of the Java Runtime (class file version 61.0)
```

**解决方案**：

```bash
# 验证字节码版本
.\gradlew.bat :freeplane_plugin_ai:verifyAiPluginBytecodeLevels

# 确保 bootstrap 代码为 Java 8
# 确保主代码为 Java 17
```

---

## 10. 最佳实践

### 10.1 构建命令使用

✅ **推荐做法**：

```bash
# 使用 Wrapper（确保版本一致）
.\gradlew.bat build

# 只构建需要的模块
.\gradlew.bat :freeplane_plugin_ai:build

# 快速构建（跳过测试）
.\gradlew.bat build -x test

# 查看详细错误
.\gradlew.bat test -PTestLoggingFull
```

❌ **避免做法**：

```bash
# 不要使用系统 Gradle（版本可能不一致）
gradle build

# 不要频繁全量构建（使用增量编译）
.\gradlew.bat clean build  # 除非必要

# 不要在构建时运行程序（文件占用）
# 先关闭 Freeplane 再构建
```

### 10.2 依赖管理

✅ **推荐做法**：

```groovy
// 使用明确版本号
lib 'dev.langchain4j:langchain4j:1.11.0'

// 定期检查更新
.\gradlew.bat dependencyUpdates

// 扫描安全漏洞
.\gradlew.bat dependencyCheckAnalyze
```

❌ **避免做法**：

```groovy
// 不要使用动态版本
lib 'dev.langchain4j:langchain4j:1.+'  // ❌

// 不要忽略安全警告
// 定期修复漏洞依赖
```

### 10.3 多模块开发

✅ **推荐做法**：

```bash
# 1. 只编译修改的模块
.\gradlew.bat :freeplane_plugin_ai:compileJava

# 2. 测试相关模块
.\gradlew.bat :freeplane_plugin_ai:test

# 3. 验证集成
.\gradlew.bat :freeplane_plugin_ai:build
```

❌ **避免做法**：

```bash
# 不要每次都全量构建（浪费时间）
.\gradlew.bat clean build  # 除非有重大变更
```

### 10.4 版本控制

✅ **提交清单**：

- ✅ `build.gradle`
- ✅ `settings.gradle`
- ✅ `gradle.properties`
- ✅ `gradle/wrapper/gradle-wrapper.properties`
- ✅ `gradle/wrapper/gradle-wrapper.jar`

❌ **不要提交**：

- ❌ `.gradle/` 目录
- ❌ `BIN/` 目录（构建产物）
- ❌ `BUILD/` 目录（构建产物）
- ❌ `.idea/` 或 `.settings/`（IDE 配置）

### 10.5 CI/CD 集成

**GitHub Actions 示例**：

```yaml
name: Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'zulu'
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run tests
      run: ./gradlew test -PTestLoggingFull
    
    - name: Upload artifacts
      uses: actions/upload-artifact@v3
      with:
        name: distribution
        path: DIST/
```

### 10.6 性能优化建议

1. **保持 Daemon 运行**：避免重复启动 JVM
2. **使用并行构建**：充分利用多核 CPU
3. **启用构建缓存**：复用历史构建结果
4. **增量编译**：只编译修改的文件
5. **定期清理缓存**：避免缓存膨胀

```bash
# 检查 Daemon 状态
.\gradlew.bat --status

# 查看缓存大小
Get-ChildItem .gradle/caches -Recurse | Measure-Object -Property Length -Sum
```

---

## 附录

### A. Gradle Wrapper 文件说明

```
gradle/
└── wrapper/
    ├── gradle-wrapper.jar         # Wrapper 可执行文件
    └── gradle-wrapper.properties  # Wrapper 配置

# gradle-wrapper.properties 内容
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### B. 常用环境变量

| 变量 | 说明 | 示例 |
|------|------|------|
| `JAVA_HOME` | JDK 路径 | `C:\Program Files\Zulu\zulu-21` |
| `GRADLE_HOME` | Gradle 安装路径（使用 Wrapper 时不需要） | `C:\gradle\gradle-8.14` |
| `GRADLE_USER_HOME` | Gradle 缓存路径 | `C:\Users\username\.gradle` |
| `GRADLE_OPTS` | Gradle JVM 参数 | `-Xmx2048m` |

### C. 版本对照表

| Gradle 版本 | 最低 Java 版本 | 最高 Java 版本 |
|-------------|---------------|---------------|
| 8.14 | 8 | 21 |
| 8.0 | 8 | 19 |
| 7.0 | 7 | 17 |

| Java 版本 | Class 文件主版本号 |
|-----------|-------------------|
| Java 8 | 52 |
| Java 11 | 55 |
| Java 17 | 61 |
| Java 21 | 65 |

### D. 参考资源

- **Gradle 官方文档**: https://docs.gradle.org/current/userguide/userguide.html
- **Gradle DSL 参考**: https://docs.gradle.org/current/dsl/
- **Knopflerfish OSGi**: https://www.knopflerfish.org/
- **Bnd 工具**: https://bnd.bndtools.org/
- **LangChain4j 文档**: https://docs.langchain4j.dev/

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2024-01-01 | 初始版本，基于 Freeplane 1.13.x |

---

**文档维护**：本文档随项目更新而更新，如有疑问请查阅 [Gradle 官方文档](https://docs.gradle.org/) 或提交 Issue。
