# Kotlin Multiplatform 库发布到 Maven Central 完整指南

本文档记录了将一个 Compose Multiplatform 库从代码编写完成到成功发布至 Maven Central 的完整流程，包括所有遇到的问题和解决方案。

## 目录

- [前置条件](#前置条件)
- [第一步：配置 Gradle 发布插件](#第一步配置-gradle-发布插件)
- [第二步：生成 GPG 签名密钥](#第二步生成-gpg-签名密钥)
- [第三步：注册 Maven Central 并获取 Token](#第三步注册-maven-central-并获取-token)
- [第四步：创建 GitHub Actions 发布工作流](#第四步创建-github-actions-发布工作流)
- [第五步：配置 GitHub Secrets](#第五步配置-github-secrets)
- [第六步：创建 Release 触发发布](#第六步创建-release-触发发布)
- [遇到的问题与解决方案](#遇到的问题与解决方案)
- [发布成功后的验证](#发布成功后的验证)
- [后续版本发布流程](#后续版本发布流程)

---

## 前置条件

- 一个已推送到 GitHub 的 Kotlin Multiplatform 项目
- 项目中有独立的库模块（本例为 `infinite-canvas`）
- 本地安装了 Git（Git for Windows 自带 GPG 工具）
- 安装了 GitHub CLI（`gh`），用于命令行操作 GitHub

### 安装 GitHub CLI

Windows 用户可以从 GitHub 下载安装包：

```
https://github.com/cli/cli/releases/latest
```

下载 `gh_*_windows_amd64.msi`，双击安装。安装后在终端中登录：

```bash
gh auth login
```

按提示选择 GitHub.com → HTTPS → 浏览器登录。

---

## 第一步：配置 Gradle 发布插件

### 1.1 添加 vanniktech maven-publish 插件

这是 Kotlin 官方推荐的发布插件，自动处理签名、上传、staging 等流程。

在 `gradle/libs.versions.toml` 中添加版本和插件声明：

```toml
[versions]
# ... 其他版本 ...
mavenPublish = "0.30.0"

[plugins]
# ... 其他插件 ...
mavenPublish = { id = "com.vanniktech.maven.publish", version.ref = "mavenPublish" }
```

在根目录 `build.gradle.kts` 中声明插件（不应用）：

```kotlin
plugins {
    // ... 其他插件 ...
    alias(libs.plugins.mavenPublish) apply false
}
```

### 1.2 配置库模块的 build.gradle.kts

以下是完整的库模块配置示例：

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)  // 替代手动的 `maven-publish`
}

// 条件编译：iOS/macOS 目标只能在 macOS 上构建
val isMacOs = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    // iOS/macOS 目标仅在 macOS 上注册
    // JitPack (Linux) 和 Windows 上会跳过这些目标
    if (isMacOs) {
        iosArm64()
        iosSimulatorArm64()
        macosArm64()
        macosX64()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.androidx.lifecycle.viewmodelCompose)
        }
    }
}

// Maven Central 发布配置
mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    // 坐标：groupId, artifactId, version
    coordinates("io.github.xingray", "compose-infinite-canvas", "0.1.2")

    pom {
        name.set("Compose Infinite Canvas")
        description.set("An infinite canvas component for Compose Multiplatform")
        url.set("https://github.com/XingRay/compose-infinite-canvas")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("XingRay")
                name.set("XingRay")
                url.set("https://github.com/XingRay")
            }
        }

        scm {
            url.set("https://github.com/XingRay/compose-infinite-canvas")
            connection.set("scm:git:git://github.com/XingRay/compose-infinite-canvas.git")
            developerConnection.set("scm:git:ssh://git@github.com/XingRay/compose-infinite-canvas.git")
        }
    }
}

android {
    namespace = "io.github.xingray.compose.infinitecanvas"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

**关键说明：**

- `coordinates()` 中的 `groupId` 必须与你在 Maven Central 验证的命名空间一致
- `SonatypeHost.CENTRAL_PORTAL` 是 2024 年之后的新发布方式，旧的 OSSRH 已于 2025 年 6 月关闭
- `signAllPublications()` 要求配置 GPG 签名（后续步骤）
- POM 信息（name, description, license, scm 等）是 Maven Central 的强制要求，缺少会被拒绝

### 1.3 启用 Compose 实验性目标

如果你的库包含 macOS 目标，需要在 `gradle.properties` 中添加：

```properties
org.jetbrains.compose.experimental.macos.enabled=true
```

> **踩坑记录：** 不添加此配置，在 macOS 上构建时会报错：
> `ERROR: Compose targets '[macos]' are experimental and may have bugs!`

---

## 第二步：生成 GPG 签名密钥

Maven Central 要求所有发布的产物必须经过 GPG 签名。

### 2.1 检查 GPG 是否可用

Windows 用户如果安装了 Git for Windows，GPG 已经自带：

```bash
gpg --version
# 输出类似：gpg (GnuPG) 2.4.7-unknown
```

### 2.2 生成密钥对

在终端（Git Bash）中运行：

```bash
gpg --full-generate-key
```

按提示依次输入：

| 提示 | 选择 |
|------|------|
| 密钥类型 | `1`（RSA and RSA） |
| 密钥长度 | `4096` |
| 有效期 | `0`（永不过期），确认 `y` |
| 姓名 | 你的名字或 GitHub 用户名 |
| 邮箱 | 你的邮箱地址 |
| 注释 | 留空，直接回车 |
| 确认 | 输入 `O` |
| 密码 | 设置一个密码，**务必记住** |

### 2.3 查看密钥信息

```bash
gpg --list-secret-keys --keyid-format=long
```

输出示例：

```
sec   rsa4096/EA86658BBE618039 2026-02-28 [SC]
      F9DC690F711B2D707C6E499FEA86658BBE618039
uid                 [ultimate] XingRay <leixing1012@gmail.com>
ssb   rsa4096/B8ECAAC4350E5E73 2026-02-28 [E]
```

从中提取：
- **完整 Key ID**：`F9DC690F711B2D707C6E499FEA86658BBE618039`
- **短 Key ID**（最后 8 位）：`BE618039` — 这个要配置到 GitHub Secrets

### 2.4 上传公钥到密钥服务器

Maven Central 验证签名时需要从公钥服务器获取你的公钥：

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys F9DC690F711B2D707C6E499FEA86658BBE618039
```

### 2.5 导出私钥

CI 环境需要使用私钥进行签名，导出为 ASCII 格式：

```bash
gpg --export-secret-keys --armor YOUR_KEY_ID > gpg-private-key.txt
```

运行时会弹出密码输入框，输入生成密钥时设置的密码。

导出的文件内容类似：

```
-----BEGIN PGP PRIVATE KEY BLOCK-----

lQdGBGmjW8sBEAC+vcb9QG56g/4OghFDNOV0VJIFCp7P2m/79YkX9gknYWWPXRU7
... (很多行 Base64 编码内容) ...
=szfK
-----END PGP PRIVATE KEY BLOCK-----
```

> **安全提醒：** 私钥文件用完后立即删除，不要提交到 Git 仓库！

---

## 第三步：注册 Maven Central 并获取 Token

### 3.1 注册账号

1. 打开 https://central.sonatype.com
2. 使用 GitHub 账号登录

### 3.2 验证命名空间

1. 进入 Namespaces 页面
2. 添加命名空间，例如 `io.github.xingray`
3. GitHub 类型的命名空间验证方式：系统会要求你在 GitHub 上创建一个特定名称的临时仓库（如 `OSSRH-12345`）
4. 创建该仓库后，回到 Maven Central 点击验证
5. 验证通过后可以删除临时仓库

### 3.3 生成 User Token

1. 登录 https://central.sonatype.com
2. 点击右上角头像 → "View Account"
3. 找到 "Generate User Token" 按钮并点击
4. 保存生成的 **username** 和 **password**

> **注意：** Token 只显示一次，务必立即保存。如果丢失，可以重新生成（旧的会失效）。

---

## 第四步：创建 GitHub Actions 发布工作流

### 4.1 创建工作流文件

创建 `.github/workflows/publish.yml`：

```yaml
name: Publish to Maven Central

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Publish all targets to Maven Central
        run: ./gradlew :infinite-canvas:publishAllPublicationsToMavenCentralRepository --no-configuration-cache
        env:
          ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
          ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
          ORG_GRADLE_PROJECT_signingInMemoryKeyId: ${{ secrets.SIGNING_IN_MEMORY_KEY_ID }}
          ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.SIGNING_IN_MEMORY_KEY }}
          ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.SIGNING_IN_MEMORY_KEY_PASSWORD }}
```

**关键说明：**

- `runs-on: macos-latest` — 使用 macOS runner，因为 iOS/macOS 目标只能在 macOS 上编译。公开仓库免费使用
- `chmod +x gradlew` — Windows 开发者提交的 `gradlew` 文件可能没有执行权限，必须加这一步
- `--no-configuration-cache` — 避免 CI 环境中的配置缓存问题
- 所有敏感信息通过 `secrets` 传入，不会暴露在日志中

### 4.2 为什么选择 macOS runner？

| Runner | 价格（公开仓库） | 可编译目标 |
|--------|:---:|---|
| ubuntu-latest | 免费 | JVM, Android, JS, WASM |
| macos-latest | 免费 | JVM, Android, JS, WASM, **iOS, macOS** |
| windows-latest | 免费 | JVM, Android, JS, WASM |

macOS runner 是唯一能编译所有目标的选择。对于公开仓库完全免费。

---

## 第五步：配置 GitHub Secrets

打开仓库设置页面：

```
https://github.com/你的用户名/你的仓库/settings/secrets/actions
```

点击 "New repository secret"，依次添加 5 个 Secret：

| Secret 名称 | 值的来源 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Maven Central User Token 的 username |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central User Token 的 password |
| `SIGNING_IN_MEMORY_KEY_ID` | GPG 密钥 ID 的最后 8 位（如 `BE618039`） |
| `SIGNING_IN_MEMORY_KEY` | `gpg-private-key.txt` 的完整内容（包括 BEGIN/END 行） |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | 生成 GPG 密钥时设置的密码 |

添加完成后，删除本地的 `gpg-private-key.txt` 文件：

```bash
rm gpg-private-key.txt
```

---

## 第六步：创建 Release 触发发布

### 6.1 确保代码已推送

```bash
git add -A
git commit -m "配置 Maven Central 发布"
git push
```

### 6.2 创建 Tag

```bash
git tag 0.1.2
git push origin 0.1.2
```

### 6.3 创建 Release

**方式一：GitHub 网页**

1. 打开 `https://github.com/你的用户名/你的仓库/releases/new`
2. 选择刚创建的 tag
3. 填写 Release title
4. 点击 "Publish release"

**方式二：GitHub CLI**

```bash
gh release create 0.1.2 --title "0.1.2" --notes "Release description"
```

### 6.4 查看构建状态

**方式一：GitHub 网页**

打开 `https://github.com/你的用户名/你的仓库/actions`

**方式二：GitHub CLI**

```bash
# 查看最近的 workflow 运行
gh run list

# 查看具体运行的详情
gh run view <run-id>

# 查看失败日志
gh run view <run-id> --log-failed
```

---

## 遇到的问题与解决方案

### 问题 1：JitPack 发布后消费方 variant 匹配失败

**错误信息：**

```
Could not resolve com.github.XingRay.compose-infinite-canvas:infinite-canvas-android:0.1.1
No matching variant of ... was found.
The consumer was configured to find ... attribute 'org.jetbrains.kotlin.platform.type' with value 'jvm' but:
  - Variant 'releaseApiElements-published' ... attribute 'org.jetbrains.kotlin.platform.type' with value 'androidJvm'
```

**原因：** JitPack 在 Linux 上构建，iOS 目标编译失败导致 `publishToMavenLocal` 整体失败或产出不完整的 Gradle Module Metadata。消费方解析依赖时无法正确匹配 JVM variant。

**解决方案：** 从库模块移除 iOS 目标（JitPack 无法构建），或改用 Maven Central 发布（使用 macOS runner 构建全平台）。

### 问题 2：GitHub Actions workflow 未触发

**现象：** 创建了 Release，但 Actions 页面没有任何运行记录。

**原因：** Release 是基于旧的 tag 创建的，而那个 tag 对应的 commit 还没有 `.github/workflows/publish.yml` 文件。GitHub Actions 只会在 Release 对应的 commit 上查找 workflow 文件。

**解决方案：** 删除旧的 Release 和 tag，在包含 workflow 文件的最新 commit 上重新创建：

```bash
# 删除旧 Release
gh release delete 0.1.2 --yes

# 删除远程和本地 tag
git push origin --delete 0.1.2
git tag -d 0.1.2

# 在最新 commit 上重新创建 tag
git tag 0.1.2
git push origin 0.1.2

# 重新创建 Release
gh release create 0.1.2 --title "0.1.2" --notes "Release notes"
```

### 问题 3：gradlew 没有执行权限

**错误信息：**

```
/Users/runner/work/_temp/xxx.sh: line 1: ./gradlew: Permission denied
```

**原因：** Windows 上开发并提交的 `gradlew` 文件没有 Unix 执行权限。macOS runner 上运行时被拒绝。

**解决方案：** 在 workflow 中添加 `chmod` 步骤：

```yaml
- name: Grant execute permission for gradlew
  run: chmod +x gradlew
```

### 问题 4：Compose macOS 目标是实验性的

**错误信息：**

```
ERROR: Compose targets '[macos]' are experimental and may have bugs!
But, if you still want to use them, add to gradle.properties:
org.jetbrains.compose.experimental.macos.enabled=true
```

**原因：** Compose Multiplatform 的 macOS 目标尚处于实验阶段，需要显式启用。

**解决方案：** 在 `gradle.properties` 中添加：

```properties
org.jetbrains.compose.experimental.macos.enabled=true
```

---

## 发布成功后的验证

### 检查 Maven Central

发布成功后约 10-30 分钟，可以在以下地址搜索到你的库：

```
https://central.sonatype.com/search?q=io.github.xingray
```

### 消费方使用

其他开发者在 `build.gradle.kts` 中添加依赖即可使用：

```kotlin
implementation("io.github.xingray:compose-infinite-canvas:0.1.2")
```

Maven Central 是 Gradle 的默认仓库，无需额外配置仓库地址。

---

## 后续版本发布流程

完成首次配置后，后续发布新版本只需 3 步：

### 1. 更新版本号

修改库模块 `build.gradle.kts` 中的版本号：

```kotlin
coordinates("io.github.xingray", "compose-infinite-canvas", "0.2.0")
```

### 2. 提交并创建 Tag

```bash
git add -A
git commit -m "发布 0.2.0"
git push
git tag 0.2.0
git push origin 0.2.0
```

### 3. 创建 Release

```bash
gh release create 0.2.0 --title "0.2.0" --notes "What's new in this version"
```

GitHub Actions 会自动触发构建和发布。

---

## 附录：各平台目标的构建环境要求

| 目标平台 | Linux | macOS | Windows |
|----------|:-----:|:-----:|:-------:|
| JVM | ✅ | ✅ | ✅ |
| Android | ✅ | ✅ | ✅ |
| JS (Browser) | ✅ | ✅ | ✅ |
| WASM (Browser) | ✅ | ✅ | ✅ |
| iOS (Arm64) | ❌ | ✅ | ❌ |
| iOS (Simulator) | ❌ | ✅ | ❌ |
| macOS (Arm64) | ❌ | ✅ | ❌ |
| macOS (X64) | ❌ | ✅ | ❌ |

这就是为什么 GitHub Actions 需要使用 `macos-latest` runner 来构建全平台产物。
