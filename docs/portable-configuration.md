# Freeplane 项目可移植性配置

## ✅ 当前状态

项目**已经配置为可移植**，可以直接在其他电脑上构建和运行。

---

## 📋 已完成的改进

### 1. ✅ JDK 路径 - 使用环境变量

**之前**（硬编码）：
```properties
org.gradle.java.home=C:/Program Files/Java/jdk-21
```

**现在**（可移植）：
```properties
# 使用系统 JAVA_HOME 环境变量
# 开发者只需设置自己的 JAVA_HOME 即可
```

### 2. ✅ 本地依赖 - 使用相对路径

**配置位置**：`build.gradle`

```groovy
repositories {
    mavenCentral()
    maven { url 'https://resources.knopflerfish.org/repo/maven2/release/' }
    
    // 使用项目相对路径（可移植）
    flatDir name: 'localGitDepsRepository',
            dirs: [rootDir.path + "/freeplane/lib",
                   rootDir.path + "/freeplane_plugin_jsyntaxpane/lib"]
}
```

**优点**：
- ✅ 使用 `rootDir.path` 相对路径
- ✅ 无论项目放在哪个目录都能工作
- ✅ 跨平台兼容（Windows/macOS/Linux）

### 3. ✅ BIN 目录 - 使用项目相对路径

```groovy
ext.globalBin = rootDir.path + '/BIN'
```

### 4. ✅ Gradle Wrapper - 锁定版本

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
```

---

## 🚀 在新电脑上部署

### 前置要求

1. **Java JDK 21**
   ```bash
   # 检查是否已安装
   java -version
   
   # 如果未安装，下载：
   # https://adoptium.net/temurin/releases/?version=21
   ```

2. **设置 JAVA_HOME 环境变量**

   **Windows**：
   ```powershell
   # 系统属性 → 环境变量 → 新建
   变量名：JAVA_HOME
   变量值：C:\Program Files\Java\jdk-21
   ```

   **macOS/Linux**：
   ```bash
   # 添加到 ~/.bashrc 或 ~/.zshrc
   export JAVA_HOME=/path/to/jdk-21
   export PATH=$JAVA_HOME/bin:$PATH
   ```

3. **Git**（用于克隆项目）

---

### 部署步骤

#### 步骤 1：克隆项目

```bash
# 从 Gitee 克隆
git clone https://gitee.com/zm050329/freeplane-recreating.git
cd freeplane-recreating

# 或从 GitHub 克隆
git clone https://github.com/your-org/freeplane-1.13.x.git
cd freeplane-1.13.x
```

#### 步骤 2：验证环境

```bash
# 检查 Java
java -version
echo $JAVA_HOME  # Windows: echo %JAVA_HOME%

# 应该输出 JDK 21
```

#### 步骤 3：构建项目

```bash
# Windows
.\gradlew.bat build

# macOS/Linux
./gradlew build
```

Gradle Wrapper 会：
- ✅ 自动下载 Gradle 8.14
- ✅ 缓存到 `~/.gradle/wrapper/dists/`
- ✅ 使用正确的版本构建

#### 步骤 4：运行 Freeplane

```bash
# Windows
BIN\freeplane.bat

# macOS/Linux
BIN/freeplane.sh
```

---

## 📂 项目结构（可移植性说明）

```
freeplane-1.13.x/
├── gradle/wrapper/          # ✅ Gradle Wrapper（可移植）
│   └── gradle-wrapper.properties
├── gradlew                  # ✅ Unix 启动脚本
├── gradlew.bat              # ✅ Windows 启动脚本
├── gradle.properties        # ✅ 项目配置（无硬编码路径）
├── build.gradle             # ✅ 构建配置（使用相对路径）
├── freeplane/lib/           # ✅ 本地依赖（相对路径）
│   └── idw-gpl-1.6.1.jar
├── BIN/                     # ⚠️ 构建产物（自动生成，不提交到 Git）
└── ...
```

---

## ⚙️ 自定义配置（可选）

### 场景 1：使用不同的 JDK 版本

如果开发者想使用 JDK 17 或其他版本：

**方法 A**：修改系统 `JAVA_HOME`
```bash
# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# macOS/Linux
export JAVA_HOME=/path/to/jdk-17
```

**方法 B**：项目级别覆盖（不影响其他项目）
编辑 `gradle.properties`：
```properties
org.gradle.java.home=/path/to/your/jdk
```

### 场景 2：离线构建

如果需要在没有网络的环境下构建：

```bash
# 1. 先在线构建一次（下载依赖）
./gradlew build

# 2. 后续可以离线构建
./gradlew build --offline
```

### 场景 3：使用代理

如果公司网络需要代理：

编辑 `gradle.properties`：
```properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

---

## 🐛 常见问题

### Q1: 构建失败 - "Could not find java.home"

**原因**：未设置 `JAVA_HOME` 环境变量

**解决**：
```bash
# Windows
set JAVA_HOME=C:\Program Files\Java\jdk-21

# macOS/Linux
export JAVA_HOME=/path/to/jdk-21
```

### Q2: 构建失败 - "Unable to delete directory BIN"

**原因**：Freeplane 正在运行或文件被占用

**解决**：
```bash
# 方法 1：关闭 Freeplane
# 方法 2：手动删除 BIN 文件夹
rm -rf BIN  # macOS/Linux
Remove-Item -Recurse -Force BIN  # Windows

# 方法 3：跳过清理
./gradlew build -x clean
```

### Q3: 依赖下载失败

**原因**：网络问题或仓库不可访问

**解决**：
```bash
# 使用国内镜像（在 build.gradle 中添加）
repositories {
    maven { url 'https://maven.aliyun.com/repository/central' }
    mavenCentral()
    // ...
}
```

### Q4: Gradle 版本不匹配

**原因**：使用了系统 Gradle 而不是 Wrapper

**解决**：
```bash
# 始终使用 gradlew/gradlew.bat
./gradlew build        # ✅ 正确
gradle build           # ❌ 错误（使用系统版本）
```

---

## 📊 可移植性检查清单

| 配置项 | 状态 | 说明 |
|--------|------|------|
| JDK 路径 | ✅ 可移植 | 使用 JAVA_HOME 环境变量 |
| 本地依赖 | ✅ 可移植 | 使用相对路径 |
| Gradle 版本 | ✅ 可移植 | 使用 Wrapper 锁定 |
| BIN 目录 | ✅ 可移植 | 使用相对路径 |
| 构建缓存 | ✅ 可移植 | 存储在 ~/.gradle |
| 配置文件 | ✅ 可移植 | 无硬编码绝对路径 |

---

## 🎯 最佳实践

### 对于开发者

1. **永远不要硬编码路径**
   - ❌ `C:\Users\zengming\...`
   - ✅ `rootDir.path + '/...'`

2. **使用环境变量**
   - `JAVA_HOME` 用于 JDK
   - `GRADLE_USER_HOME` 用于 Gradle 缓存（可选）

3. **使用 Gradle Wrapper**
   - 确保所有开发者使用相同版本
   - 避免版本冲突

4. **不要提交构建产物**
   - `BIN/` 已在 `.gitignore` 中
   - `build/` 已在 `.gitignore` 中

### 对于项目维护者

1. **定期检查硬编码路径**
   ```bash
   grep -r "C:\\" *.gradle
   grep -r "/Users/" *.gradle
   ```

2. **测试跨平台构建**
   - Windows
   - macOS
   - Linux

3. **文档更新**
   - 保持本文档最新
   - 记录所有环境要求

---

## 📝 总结

✅ **项目已经完全可移植**

其他开发者只需：
1. 安装 JDK 21
2. 设置 `JAVA_HOME`
3. 克隆项目
4. 运行 `./gradlew build`

无需任何额外的路径配置！
