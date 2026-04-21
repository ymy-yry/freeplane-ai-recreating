# 后端部署与测试报告（王彦博）

## 基本信息

- 日期：第四周
- 分支：`origin/d`
- 工作区：`d:\kaiyuan\freeplane-recreating\_codex_worktrees\d`
- 测试目标：验证 `d` 分支后端是否可编译、可启动、可提供 REST API，并补充本地 mock 测试与首轮性能基线

## 工作概述

本次工作围绕 `d` 分支后端展开，主要完成了以下内容：

1. 搭建并验证 `d` 分支本地构建环境
2. 编译并运行后端相关模块
3. 补充 REST API 本地 mock 集成测试
4. 启动真实 Freeplane 实例并执行 live 冒烟测试
5. 修复 AI 接口在未配置 provider 时的空指针/错误返回问题
6. 编写 `k6` 性能测试脚本并执行首轮轻量性能基线

## 环境配置

### Java 与 Gradle

- 使用 JDK：`C:\Program Files\Java\jdk-21`
- 使用 Gradle：`C:\Users\19067\.gradle\wrapper\dists\gradle-9.2.0-bin\11i5gvueggl8a5cioxuftxrik\gradle-9.2.0\bin\gradle.bat`

### 关键说明

- 系统默认 Java 24 无法正常启动当前 Freeplane，因为启动参数中包含 `-Djava.security.manager=allow`，而 Java 24 已不再支持启用 Security Manager。
- 因此，实际运行与测试时必须显式切换到 JDK 21。

## 构建与部署过程

### 1. 编译验证

已成功执行：

```powershell
gradle :freeplane_plugin_ai:compileJava --no-daemon
```

结果：

- `freeplane_plugin_ai` 编译通过
- 说明 `d` 分支已补齐此前缺失的 `icon` 相关源码，具备后端基础可测性

### 2. 插件测试验证

已成功执行：

```powershell
gradle :freeplane_plugin_ai:test --no-daemon -PTestLoggingFull
```

结果：

- `freeplane_plugin_ai` 全量测试通过

### 3. 运行产物生成

为获得可运行界面与真实 REST 服务，执行了跳过测试的构建：

```powershell
gradle build -x test --no-daemon
```

结果：

- 成功生成 `BIN` 目录及启动脚本
- 成功生成 `freeplane.bat`

### 4. 插件装载问题处理

首次启动时发现 AI 插件目录中仅有 `lib`，缺少：

- `META-INF/MANIFEST.MF`

导致 OSGi 未正确装载 AI 插件，`6299` 端口未开启。

后续通过执行以下任务补齐插件元数据：

```powershell
gradle :freeplane_plugin_ai:copyOSGiManifest --no-daemon
gradle :freeplane_plugin_ai:pluginJar :freeplane_plugin_ai:copyOSGiJars :freeplane_plugin_ai:copyOSGiManifest --no-daemon
```

补齐后再次启动，日志确认：

- AI 插件已 `Installed`
- AI 插件已 `Started`
- `RestApiServer: started on port 6299`

## 新增与修改内容

### 1. 新增 REST API mock 集成测试

新增文件：

- `freeplane_plugin_ai/src/test/java/org/freeplane/plugin/ai/restapi/RestApiRouterIntegrationTest.java`

该测试会在本地随机端口启动临时 `HttpServer`，并通过 mock 的 `MapModelProvider` 与 `AIChatPanel` 对 REST 层进行验证，不依赖真实前端或真实 AI provider。

### 2. 修复 AI 接口未配置 provider 时的异常处理

修改文件：

- `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/service/impl/DefaultAgentService.java`
- `freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/service/impl/DefaultChatService.java`

修复内容：

- 当 AI provider 未配置时，不再继续调用空的 `agentService`
- 将原本的内部异常/空指针行为统一替换为友好错误提示：

```json
{
  "error": "AI service not initialized. Please configure AI provider in preferences."
}
```

### 3. provider 识别逻辑补全

原逻辑仅判断：

- `openrouter`
- `gemini`
- `ollama`

现已补充：

- `dashscope`
- `ernie`

避免用户配置了国内模型 key 后仍被错误识别为“未配置 AI 服务”。

### 4. 新增 k6 压测脚本

新增文件：

- `perf/k6-rest-baseline.js`
- `perf/README.md`

脚本覆盖接口：

- `GET /api/map/current`
- `POST /api/nodes/search`
- `POST /api/nodes/create`

## 测试内容与结果

## 一、后端基础测试

### 1. 编译测试

结果：通过

- `freeplane_plugin_ai` 编译成功
- 后端代码可进入后续运行与测试阶段

### 2. 插件测试

结果：通过

- `freeplane_plugin_ai:test` 全量通过
- 当前测试结果文件共 67 个 XML 报告

## 二、REST API mock 测试

### 测试方式

通过本地临时 `HttpServer` + mock 依赖验证 REST 路由、状态码、参数校验和 CORS。

### 覆盖场景

共 10 项，全部通过：

1. 无地图时 `GET /api/map/current` 返回 `404`
2. `OPTIONS /api/ai/chat/message` 返回 `204` 且带 CORS 头
3. `POST /api/ai/chat/message` 缺少 `message` 返回 `400`
4. `POST /api/ai/build/expand-node` 缺少 `nodeId` 返回 `400`
5. `POST /api/nodes/create` 缺少必填参数返回 `400`
6. 未知路由返回 `404`
7. 未配置 provider 时 `POST /api/ai/chat/message` 返回友好错误
8. 未配置 provider 时 `POST /api/ai/build/expand-node` 返回友好错误
9. 未配置 provider 时 `POST /api/ai/build/summarize` 返回友好错误
10. 未配置 provider 时 `POST /api/ai/build/tag` 返回友好错误

### 结果

- `RestApiRouterIntegrationTest` 通过
- AI 接口现在在无 provider 场景下返回一致、可理解的错误信息

## 三、真实后端冒烟测试

### 测试方式

启动真实 Freeplane 图形界面，确认插件装载后通过 `http://127.0.0.1:6299` 访问后端接口。

### Live 测试结果

#### 1. 地图接口

- `GET /api/map/current`：通过，状态 `200`
- `POST /api/maps/create`：通过，状态 `201`
- `GET /api/maps`：通过，状态 `200`

#### 2. 节点接口

- `POST /api/nodes/create`：通过，状态 `200`
- `GET /api/nodes/{id}`：通过，状态 `200`
- `POST /api/nodes/edit`：通过，状态 `200`
- `POST /api/nodes/toggle-fold`：通过，状态 `200`
- `POST /api/nodes/search`：通过，状态 `200`
- `POST /api/nodes/delete`：通过，状态 `200`

#### 3. AI 接口

在未配置 provider 的情况下：

- `GET /api/ai/chat/models`：通过，状态 `200`
  - 返回空模型列表 `[]`
- `POST /api/ai/chat/message`：状态 `500`
  - 返回友好错误提示：AI service not initialized
- `POST /api/ai/build/expand-node`：状态 `500`
  - 返回友好错误提示：AI service not initialized
- `POST /api/ai/build/summarize`：状态 `500`
  - 返回友好错误提示：AI service not initialized
- `POST /api/ai/build/tag`：状态 `500`
  - 返回友好错误提示：AI service not initialized

### Live 测试结论

- 地图与节点基础后端功能已跑通
- AI 插件框架与 REST 接口已加载成功
- AI 功能依赖 provider 配置后才能真正执行
- AI 接口在 provider 未配置时的错误处理已被修复为友好返回

## 四、性能基线测试

### 1. k6 脚本与安装

已编写 `k6` 压测脚本：

- `perf/k6-rest-baseline.js`

并使用 `winget` 成功安装：

- `GrafanaLabs.k6` 版本 `1.7.1`
- 可执行路径：`C:\Program Files\k6\k6.exe`

### 2. 正式 k6 基线测试

已使用 `k6` 对以下接口执行首轮正式基线测试：

- `GET /api/map/current`
- `POST /api/nodes/search`
- `POST /api/nodes/create`

测试脚本采用 3 个场景：

- `map_current_reads`：5 个 VU，持续 30s
- `node_search_reads`：3 个 VU，持续 30s
- `node_create_writes`：1 个 VU，持续 20s

### 3. k6 测试结果

#### 场景级结果

| 接口 | 场景压力 | 请求数 | 成功率 | 平均耗时 | P95 |
| --- | --- | ---: | ---: | ---: | ---: |
| `/api/map/current` | 5 VU / 30s | 150 | 100% | 0.93 ms | 1.63 ms |
| `/api/nodes/search` | 3 VU / 30s | 90 | 100% | 1.19 ms | 2.19 ms |
| `/api/nodes/create` | 1 VU / 20s | 20 | 100% | 9.13 ms | 16.61 ms |

#### 全局结果

- 总请求数：261
- 总检查项：521
- 检查通过率：100%
- 总体平均耗时：1.76 ms
- 总体 P95：6.32 ms
- 总体失败率：0%

#### 阈值结果

全部通过：

- `map/current` P95 `< 500ms`
- `nodes/search` P95 `< 800ms`
- `nodes/create` P95 `< 1000ms`
- 各场景失败率 `< 1%`

### 4. 轻量本地替代基线

除正式 `k6` 结果外，还使用 PowerShell + `HttpClient` 补充执行了一版轻量本地测量，作为吞吐量参考值：

| 接口 | 请求数 | 成功率 | 平均耗时 | P95 | 吞吐量 |
| --- | ---: | ---: | ---: | ---: | ---: |
| `/api/map/current` | 50 | 100% | 0.55 ms | 0.93 ms | 858.80 QPS |
| `/api/nodes/search` | 50 | 100% | 0.68 ms | 0.92 ms | 1147.78 QPS |
| `/api/nodes/create` | 20 | 100% | 2.22 ms | 3.58 ms | 393.41 TPS |

### 基线结论

- 当前在单机本地环境下，基础地图和节点接口延迟很低
- 正式 `k6` 基线与本地轻量测量结果一致表明：地图读取与节点搜索性能表现稳定
- `nodes/create` 写操作明显高于读接口，但在当前压力下仍保持低延迟和 100% 成功率
- 本轮 `k6` 为低压基线测试，后续可继续提高 VU、持续时间和混合写读比例做进一步压测

## 当前后端状态总结

### 已完成

- `d` 分支后端可编译
- AI 插件可加载
- REST 服务可启动
- 地图与节点 CRUD 基础链路可用
- 后端 mock 集成测试已补齐
- AI 接口未配置 provider 时的异常处理已修复
- 首轮性能基线已产出

### 当前仍存在的限制

- 当前未配置真实 AI provider，AI 功能尚未进入“真实可用”状态
- 若后续需要验证 AI 对话、节点展开、摘要、标签等真实能力，必须在偏好设置中配置至少一种 provider
- 本机未安装 `k6`，正式性能压测尚未执行

## 后续建议

1. 在 Freeplane 偏好设置中配置至少一个 AI provider
2. 对以下 AI 接口做真实功能测试：
   - `/api/ai/chat/message`
   - `/api/ai/build/expand-node`
   - `/api/ai/build/summarize`
   - `/api/ai/build/tag`
3. 安装 `k6` 后执行正式压测：

```powershell
k6 run .\perf\k6-rest-baseline.js
```

4. 继续扩展性能测试覆盖：
   - `/api/maps`
   - `/api/nodes/edit`
   - `/api/nodes/delete`
5. 按 OWASP REST 安全建议继续补输入校验、错误处理和接口健壮性测试

## 参考测试方法

- Grafana k6 Thresholds  
  https://grafana.com/docs/k6/latest/using-k6/thresholds/

- Apache JMeter Web Test Plan  
  https://jmeter.apache.org/usermanual/build-web-test-plan

- OWASP REST Security Cheat Sheet  
  https://cheatsheetseries.owasp.org/cheatsheets/REST_Security_Cheat_Sheet.html
