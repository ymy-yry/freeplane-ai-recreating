# Freeplane 项目 Git 多分支协作指南

## 一、代码仓库与分支规范

### 1.1 仓库地址

**Gitee 仓库**：https://gitee.com/zm050329/freeplane-recreating

请全体成员尽快 clone 最新代码，并熟悉项目结构和已有代码：

```bash
git clone https://gitee.com/zm050329/freeplane-recreating.git
cd freeplane-recreating
```

### 1.2 分支管理规则

| 分支名称 | 用途 | 负责人 | 说明 |
|---------|------|--------|------|
| `master` | 主分支（稳定版） | 全员 | **禁止直接修改**，仅通过 PR 合并 |
| `feature/a` | 成员A：REST API 桥接层 | 成员A | 后端开发 |
| `feature/b` | 成员B：思维导图主视图 | 成员B | 前端开发 |
| `feature/c` | 成员C：AI 功能面板 | 成员C | 前端开发 |
| `feature/d` | 成员D：多模型切换与 AI 工具 | 成员D | 后端开发 |

**创建个人分支**：

```bash
# 成员A示例
git checkout -b feature/a

# 成员B示例
git checkout -b feature/b

# 成员C示例
git checkout -b feature/c

# 成员D示例
git checkout -b feature/d
```

**重要原则**：
- ✅ 所有修改都在自己的分支上进行
- ✅ 开发完成后通过 Pull Request（PR）合并到 `master` 分支
- ❌ **不要直接在 `master` 分支上修改代码**

---

## 二、提交与合并流程

### 2.1 日常开发流程

```bash
# 1. 每次开发前，先拉取最新代码
git checkout master
git pull origin master

# 2. 切换回自己的工作分支
git checkout feature/a  # 替换为你的分支名

# 3. 将 master 的最新代码合并到自己的工作分支
git merge master

# 4. 开发、提交（小步提交，commit 信息要清晰规范）
git add .
git commit -m "A: 完成 RestApiServer.java"

# 5. 推送到远程
git push origin feature/a
```

### 2.2 Commit 信息规范

**格式**：`<负责人>: <简要描述>`

| 负责人 | 示例 |
|--------|------|
| 成员A | `A: 完成 RestApiServer.java` |
| 成员B | `B: 实现 MindMapCanvas.vue 基础布局` |
| 成员C | `C: 完成 AiChatPanel.vue 流式输出` |
| 成员D | `D: 新增 DashScopeChatModelProvider` |

**要求**：
- ✅ 简明扼要，说明做了什么
- ✅ 使用中文描述
- ❌ 不要写 `update`、`fix` 等模糊信息

### 2.3 合并前检查清单

在创建 Pull Request 之前，必须确保：

- [ ] 代码能正常编译通过
- [ ] 后端接口通过 Apifox 测试
- [ ] 前端页面能正常显示和交互
- [ ] 没有提交不必要的文件（编译产物、IDE 配置）
- [ ] Commit 信息清晰规范

### 2.4 创建 Pull Request 合并到 master

1. 推送功能分支到远程：`git push origin feature/a`
2. 访问仓库：https://gitee.com/zm050329/freeplane-recreating
3. 点击 "Pull Request" → "新建 Pull Request"
4. 源分支：`feature/a`，目标分支：`master`
5. 填写标题和描述，说明本次改动的内容
6. 等待其他成员审查，审查通过后点击"合并"

---

## 三、协作与沟通要求

### 3.1 关联模块提前沟通

如果两位同学负责的内容存在关联，请**主动提前沟通**，约定好接口格式、数据结构和交互方式，避免重复劳动或冲突。

**典型协作场景**：

| 关联成员 | 协作内容 | 沟通要点 |
|---------|---------|---------|
| 成员A ↔ 成员B | REST API 接口联调 | JSON 数据结构、字段命名、错误处理 |
| 成员B ↔ 成员C | 右键菜单触发 AI 功能 | 事件传递方式、状态同步机制 |
| 成员A ↔ 成员D | AI 工具接口接入 | stub 替换时机、参数格式 |
| 成员C ↔ 成员D | AI 模型列表动态加载 | 模型选择器与后端配置同步 |

### 3.2 联调约定流程

1. **定义接口文档**：后端负责人提供接口路径、请求参数、响应格式
2. **确认数据结构**：前后端共同确认 JSON 字段名、类型、是否必须
3. **Mock 数据测试**：前端先用模拟数据开发，不阻塞进度
4. **实际接口联调**：后端完成后，前端切换为真实 API 调用
5. **验证测试**：双方共同验证功能是否正常

### 3.3 遇到问题及时沟通

遇到技术难题或进度问题，**及时在小组群里说明**：

**反馈格式**：
```
【问题反馈】
负责人：成员B
问题描述：MindMapCanvas.vue 渲染大图时卡顿
影响范围：节点数量超过 200 时帧率下降
已尝试方案：使用 dagre 默认配置、关闭动画
需要帮助：寻求布局优化建议
```

### 3.4 每日进度同步

建议在每天工作结束前，在小组群同步：
- ✅ 今天完成了什么
- 🔄 明天计划做什么
- ⚠️ 是否有阻塞问题

---

## 四、冲突处理

### 4.1 何时会出现冲突

- 多人修改了同一个文件的同一行
- 一个人修改了文件，另一个人删除了该文件
- 合并分支时，两个分支对同一段代码做了不同修改

### 4.2 解决冲突步骤

```bash
# 1. 尝试合并 master 的最新代码
git pull origin master

# 2. 如果出现冲突，Git 会提示
# CONFLICT (content): Merge conflict in NodeRestController.java

# 3. 打开冲突文件，找到冲突标记
<<<<<<< HEAD
当前分支的代码
=======
要合并的分支的代码
>>>>>>> master

# 4. 手动编辑文件，保留需要的代码，删除冲突标记

# 5. 标记冲突已解决
git add NodeRestController.java

# 6. 完成合并
git commit -m "merge: 解决与 master 分支的冲突"
```

### 4.3 避免冲突的最佳实践

1. **频繁同步**：每天至少 `git pull` 两次（开始工作前、提交前）
2. **小步提交**：每次提交只做一个小改动，降低冲突概率
3. **沟通分工**：团队成员提前约定谁修改哪些文件
4. **使用独立分支**：不要直接在 `master` 上开发

---

## 五、注意事项

### 5.1 禁止操作

❌ **不要在 master 分支上直接提交**
```bash
# 错误示例
git checkout master
git commit -m "修改了代码"  # 应该在 feature 分支开发
```

❌ **不要提交敏感信息**
- API Key、密码、Token 等应放在 `.gitignore` 排除的文件中
- Freeplane 项目使用 `secrets.xml` 单独管理密钥

❌ **不要提交编译产物**
```
*.class
*.jar
BIN/
DIST/
.idea/
```

### 5.2 换行符问题

Windows 用户可能遇到 LF/CRLF 警告，统一配置：

```bash
# 提交时转LF，检出时转CRLF（Windows推荐）
git config core.autocrlf true

# 或者彻底关闭自动转换（开源项目推荐）
git config core.autocrlf false
```

---

## 六、实用技巧

### 6.1 查看状态

```bash
# 查看当前分支
git branch

# 查看远程分支
git branch -r

# 查看当前状态
git status

# 查看提交历史
git log --oneline
```

### 6.2 撤销操作

```bash
# 撤销工作区的修改（未add）
git checkout -- NodeRestController.java

# 取消暂存（已add但未commit）
git reset HEAD NodeRestController.java

# 撤销最后一次提交（保留修改）
git reset --soft HEAD~1
```

### 6.3 临时保存工作进度

```bash
# 保存当前未提交的修改
git stash

# 查看已保存的进度
git stash list

# 恢复最近一次保存
git stash pop
```


