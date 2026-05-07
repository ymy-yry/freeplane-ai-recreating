# 🤖 Freeplane AI Plugin

**Powerful AI for Freeplane mind maps** — supports OpenRouter, Gemini, Ollama, ERNIE, DashScope and more. Let AI read, edit, and build mind map nodes directly.

![AI Chat Panel](docs/screenshots/ai-chat-panel.png)
*AI Chat Panel - Intelligent Mind Map Interaction*

---

## ✨ Features

### 💬 AI Chat Panel
- **Right-tab integration**: seamlessly embedded in Freeplane UI
- **Multi-turn conversation**: context memory up to 65,536 tokens
- **Tool Call visualization**: clearly shows AI tool invocations
- **Font scaling**: adjustable chat font size (50%–200%)
- **Token counter**: real-time token usage display

### 🧠 AI Smart Tool Calls (14 Tools)

AI can directly manipulate mind maps with the following tools:

| 类别 | 方法 | 说明 |
|------|------|------|
| **Read nodes** | `readNodesWithDescendants` | Read a node and all its descendants |
| **Fetch for editing** | `fetchNodesForEditing` | Get editable content fields (text, details, notes, attributes, etc.) |
| **Create nodes** | `createNodes` | Batch-create nodes with hierarchical structure |
| **Edit nodes** | `edit` | Unified edit API — text/details/notes/attributes/tags/icons/styles/hyperlinks/connectors |
| **Move nodes** | `moveNodes` | Move nodes to a new parent |
| **Delete nodes** | `deleteNodes` | Delete specified nodes |
| **Search nodes** | `searchNodes` | Search nodes by content, with regex support |
| **Select node** | `selectSingleNode` | Select and scroll to a specific node |
| **Get selection** | `getSelectedMapAndNodeIdentifiers` | Get currently selected map and node identifiers |
| **Create summary** | `createSummary` | Create a summary node aggregating child content |
| **Move into summary** | `moveNodesIntoSummary` | Move nodes into a summary node |
| **List icons** | `listAvailableIcons` | List application-level available icons |
| **List styles** | `listMapStyles` | List styles defined in the target map |
| **Edit connectors** | `editConnectors` | Edit connectors between nodes (source/target/labels) |

### 🌐 多 AI 提供商支持

| 提供商 | 示例模型 | 配置键 | 说明 |
|--------|---------|--------|------|
| **OpenRouter** | GPT-5, Claude Sonnet 4.6, Gemini 2.5 Pro | `ai_openrouter_key` | Multi-model aggregator |
| **Google Gemini** | gemini-3-pro, gemini-2.5-flash | `ai_gemini_key` | Google's latest AI models |
| **Ollama** | llama3, mistral, qwen | `ai_ollama_service_address` | Local deployment, privacy-first |
| **ERNIE** | ernie-4.5-turbo, deepseek-v3 | `ai_ernie_key` | Baidu — Chinese-optimized |
| **DashScope** | qwen-max, qwen-plus | `ai_dashscope_key` | Alibaba Cloud AI service |

### 🔌 MCP 服务器（模型上下文协议）
- **Port**: 6298 (configurable)
- **Token auth**: secure access control
- **External integration**: callable from Claude Desktop, Cursor, etc.
- **Remote control**: operate Freeplane mind maps via API

### 🎨 三种交互模式

#### 1️⃣ **Chat Mode**
- **Use case**: discuss mind map content, get suggestions
- **Features**:
  - Natural-language dialogue
  - AI answers do not modify the map directly
  - Ideal for brainstorming and planning
- **How to enable**: select "Chat" in the AI panel top-right

#### 2️⃣ **Build Mode**
- **Use case**: let AI create and edit mind maps automatically
- **Features**:
  - AI can directly manipulate nodes
  - Auto-generate mind map structures
  - Batch create, edit, and organize nodes
- **How to enable**: select "Build" in the AI panel top-right
- **Example prompt**:
  ```
  "Create a mind map about Machine Learning with Supervised, Unsupervised, and Reinforcement Learning branches"
  ```

#### 3️⃣ **Auto Mode**
- **Use case**: AI intelligently decides when to chat and when to act
- **Features**:
  - AI picks the best interaction style
  - Switches mode based on user intent
  - Smartest and most efficient
- **How to enable**: select "Auto" in the AI panel top-right

---

## 🏗️ Technical Architecture

### Backend
- **Language**: Java 17 (with Java 8 bootstrap compatibility)
- **AI Framework**: LangChain4j 1.10.0
- **Plugin System**: OSGi (Knopflerfish 8.0.11)
- **Transport**: REST API (port 6299) + MCP (port 6298)

### Frontend (In Development)
- **Tech Stack**: Vue 3 + TypeScript + Pinia + Vue Flow
- **Features**: Responsive design, real-time canvas sync, node operations
- **AI Integration**: Three-mode switch, model selection, intelligent routing

### Key Components
```
freeplane_plugin_ai/
├── chat/           # AI 聊天子系统
├── tools/          # 14 个 AI 工具实现
├── mcpserver/      # 模型上下文协议服务器
├── restapi/        # REST API 服务器
├── edits/          # AI 编辑追踪与标记
└── buffer/         # 智能缓冲层
```

---

## 🚀 Core Technical Improvements

### 1️⃣ Cycle Detection Algorithm: From 5 DFS Passes to Snake-Digest Three-Color Marking

**Problem**: AI-generated mind map JSON may contain circular references, which cause JVM stack overflow when recursively creating nodes.

**Solution**: "Snake-Digest" principle + Three-Color Marking method

```
JSON string
    ↓ Jackson parse
JsonNode tree ("egg", complete object tree)
    ↓ traverseToGraph() DFS node-by-node extraction
GraphData adjacency list ("egg liquid", lightweight graph structure)
    ↓ single statsDFS()
Cycle detection + depth check + statistics (all complete)
```

**Performance Improvement**:
- 85 nodes: 271 µs (↓ 66%)
- 400 nodes: 390 µs (↓ 87%)
- 1111 nodes: 1057 µs (↓ 89%)
- Algorithm complexity: optimized from O(n²) to O(n)

### 2️⃣ Factory Method Pattern: Eliminated 118-Line instanceof Chain

**Improvement**: Use factory pattern instead of lengthy instanceof conditional checks

| Dimension | Before | After | Improvement |
|-----------|--------|-------|-------------|
| Lines of code | 118 lines | 1 line call | ↓ 99% |
| Open-Closed Principle | ❌ Violated | ✅ Compliant | Essential improvement |
| Testability | ❌ Cannot test independently | ✅ Unit testable | Coverage improved |

### 3️⃣ Observer Pattern: Tool Execution Lifecycle Monitoring

**Features**:
- Parameter pre-validation before tool execution
- Performance metric collection after execution
- Unified exception handling on execution failure

**Implementation**:
- Java 16+ sealed interfaces + records
- Decorator pattern wrapping executor
- Support for multiple observers listening in parallel

### 4️⃣ Intelligent Buffer Layer: Natural Language Routing & Plugin Architecture

**Features**:
- Requirement Analyzer: understand user intent
- Prompt Optimizer: generate high-quality prompts
- Model Router: select optimal model based on task
- Result Optimizer: post-process AI output

### 5️⃣ Streaming Output: SSE Token-by-Token Push

**Advantages**:
- Real-time display of AI output
- Typewriter effect improves user experience
- Branch summary supports segment-by-segment push

### 6️⃣ Degradation Strategy: Fatal vs Non-Fatal Error Classification

**Decision Tree**:
```
Validate AI response
    ↓
Valid? ──YES──▶ Use directly
    │
   NO
    ↓
Has cycle/parse error? ──YES──▶ Force degradation (use sample JSON)
    │
   NO
    ↓
Other warnings ──▶ Log and continue with original response
```

---

## 📦 Installation

### Option 1: Pre-packaged Zip File (Recommended)

1. Download the latest release: [Zip package download link](https://github.com/ymy-yry/freeplane-ai-recreating/releases/latest)
2. Extract to Freeplane's `plugins` directory
3. Restart Freeplane

### Option 2: Manual JAR Install

1. Download the JAR file: [freeplane_plugin_ai-1.13.3.jar](https://github.com/ymy-yry/freeplane-ai-recreating/releases/latest)
2. Copy the JAR into Freeplane's `plugins` directory:
   ```
   Windows: %APPDATA%\Freeplane\plugins\
   macOS:   ~/Library/Application Support/Freeplane/plugins/
   Linux:   ~/.freeplane/plugins/
   ```
3. Restart Freeplane → **Tools → Add-ons** to verify

### Option 3: Development Deployment

```bash
# 克隆仓库
git clone https://github.com/ymy-yry/freeplane-ai-recreating.git
cd freeplane-ai-recreating

# 构建插件 JAR
cd freeplane_plugin_ai
gradle jar

# 生成完整的 .addon.mm 包（推荐）
gradle packageAddonMM

# 输出位置：
# build/outputs/org.freeplane.plugin.ai.addon.mm

# 部署到 BIN 目录（开发测试）
gradle deployToBin

# 启动 Freeplane
../BIN/freeplane.bat  # Windows
../BIN/freeplane.sh   # Linux/macOS
```

### 🎯 Build Commands

```bash
# 1. Compile the plugin
gradle :freeplane_plugin_ai:compileJava

# 2. Package the JAR
gradle :freeplane_plugin_ai:jar

# 3. Generate .addon.mm add-on package (with embedded binary data)
gradle :freeplane_plugin_ai:packageAddonMM

# 4. Deploy to Freeplane BIN directory
gradle :freeplane_plugin_ai:deployToBin

# 5. Run unit tests
gradle :freeplane_plugin_ai:test

# 6. Full build (compile + test + package)
gradle build
```

### 📦 Build Artifacts

| File | Size | Purpose |
|------|------|---------|
| `freeplane_plugin_ai-1.13.3.jar` | ~0.6 MB | JAR file — place manually in plugins directory |
| `org.freeplane.plugin.ai.addon.mm` | ~0.8 MB | **Recommended** — full add-on package, double-click to install |

> **💡 Tip**: the `.addon.mm` file contains all necessary binary data and can be installed directly in Freeplane with no extra steps.

---

## ⚙️ Configure API Keys

### Option A: Via UI

1. Open Freeplane
2. **Tools → Preferences → AI**
3. Fill in the API key for your chosen provider

### Option B: Via Config File

Edit `secrets.properties` in your Freeplane user directory:

```properties
# OpenRouter (recommended — multi-model)
ai_openrouter_key=sk-or-v1-xxxxx

# Google Gemini
ai_gemini_key=AIza-xxxxx

# ERNIE (Baidu)
ai_ernie_key=bce-v3/ALTAK-xxxxx

# DashScope (Alibaba Cloud)
ai_dashscope_key=sk-xxxxx

# Ollama (local deployment, optional)
ai_ollama_service_address=http://localhost:11434
```

### Get an API Key

| Provider | URL | Free Tier |
|----------|-----|-----------|
| OpenRouter | https://openrouter.ai/keys | $1 credit on signup |
| Google Gemini | https://aistudio.google.com/app/apikey | Free |
| ERNIE | https://qianfan.cloud.baidu.com/ | New-user credit |
| DashScope | https://dashscope.console.aliyun.com/ | Free tier |
| Ollama | https://ollama.ai/ | Completely free (local) |

---

## 🚀 Usage Examples

### Example 1: Create a Mind Map (Build Mode)

**User input**:
```
Create a mind map about "Python Programming" with:
- Basics (variables, data types, control flow)
- Functions & Modules
- Object-Oriented Programming
- Popular Libraries (NumPy, Pandas, Requests)
```

**AI auto-executes**:
1. Calls `createNodes` to create the root node "Python Programming"
2. Uses hierarchical parameters to create 4 first-level branches
3. Adds child nodes to each branch
4. Auto-expands all nodes

### Example 2: Edit Node Content (Build Mode)

**User input**:
```
Select the "Machine Learning" node and expand it with 3 child nodes
```

**AI operations**:
1. Calls `searchNodes` to locate the "Machine Learning" node
2. Calls `createNodes` to create 3 children:
   - Supervised Learning
   - Unsupervised Learning
   - Reinforcement Learning
3. Adds a short description to each child

### Example 3: Consultation (Chat Mode)

**User input**:
```
My mind map is about "Web Development" — what important topics am I missing?
```

**AI answer**:
```
Based on the current map, consider adding:

1. Frontend Frameworks (React, Vue, Angular)
2. Backend Tech (Node.js, Django, Spring)
3. Databases (MySQL, MongoDB, Redis)
4. DevOps (Docker, CI/CD, cloud deployment)
5. Security best practices

Want me to add these for you?
```

### Example 4: Remote Call via MCP

**Claude Desktop config**:
```json
{
  "mcpServers": {
    "freeplane": {
      "command": "curl",
      "args": ["http://localhost:6298/mcp"],
      "env": {
        "Authorization": "Bearer YOUR_TOKEN"
      }
    }
  }
}
```

**Claude conversation**:
```
Open the "Project Plan" mind map in Freeplane,
and add a "UI Design Review" child node under "Phase 2"
```

---

## 🔧 Advanced Configuration

### Sampling Parameters

> ⚠️ **Note**: some config keys below must be added manually to Freeplane's preferences or config file. Advanced Agent features are still in development.

```properties
# Temperature (creativity vs. determinism, 0.0–1.0)
temperature=0.2

# Top-P (sampling diversity, 0.0–1.0)
top_p=0.9

# Presence penalty (encourage new topics, -2.0 to 2.0)
presence_penalty=0.0

# Frequency penalty (reduce repetition, -2.0 to 2.0)
frequency_penalty=0.0
```

### Display Settings

```properties
# Show Tool Call history
ai_chat_shows_tool_calls=true

# Font scaling percentage
ai_chat_font_scaling=100

# Token counter display mode
ai_chat_token_counter_mode=visible

# Auto-unfold parents when creating nodes
ai_unfolds_parents_on_create=true

# AI edit state-icon visibility
ai_edits_state_icon_visible=true
```

---

## 📊 Performance

### Large Mind Maps
- **Virtual scrolling**: render only visible nodes
- **Incremental updates**: refresh only changed parts
- **Lazy loading**: load collapsed nodes on demand
- **Memory management**: auto-reclaim unused caches

### AI Response Acceleration
- **Streaming output**: display AI content in real time
- **Concurrent requests**: call multiple tools in parallel
- **Caching**: reuse identical AI responses
- **Token optimization**: smart context trimming to save tokens

---

## 🐛 Troubleshooting

### Issue 1: AI Panel Does Not Appear

**Cause**: Java version below 17

**Solution**:
```bash
# Check Java version
java -version

# Java 17 or later is required
# Download: https://adoptium.net/
```

### Issue 2: API Call Fails

**Checklist**:
- [ ] API Key is correctly configured
- [ ] Network connection is working
- [ ] Provider account has credit
- [ ] Firewall is not blocking requests

**Log location**:
```
Windows: %APPDATA%\Freeplane\freeplane.log
macOS:   ~/Library/Application Support/Freeplane/freeplane.log
Linux:   ~/.freeplane/freeplane.log
```

### Issue 3: Ollama Connection Fails

**Solution**:
```bash
# 1. Start Ollama
ollama serve

# 2. Pull a model
ollama pull llama3

# 3. Test connectivity
curl http://localhost:11434/api/tags

# 4. Configure Freeplane
ai_ollama_service_address=http://localhost:11434
```

### Issue 4: MCP Server Won't Start

**Cause**: port 6298 is in use

**Solution**:
```properties
# Change MCP port
ai_mcp_server_port=6299
```

---

## 📚 Developer Documentation

### Project Structure

```
freeplane_plugin_ai/
├── src/main/java/org/freeplane/plugin/ai/
│   ├── Activator.java                      # OSGi plugin entry point
│   ├── chat/                               # AI chat subsystem
│   │   ├── AIChatPanel.java               # Chat panel UI
│   │   ├── AIProviderConfiguration.java   # Provider config
│   │   └── AIModelSelection.java          # Model selection
│   ├── service/                            # AI service layer
│   │   ├── AIServiceFactory.java          # Service factory
│   │   ├── OpenRouterAIService.java       # OpenRouter impl
│   │   ├── GeminiAIService.java           # Gemini impl
│   │   └── OllamaAIService.java           # Ollama impl
│   ├── tools/                              # AI tool set (14 tools)
│   │   ├── AIToolSet.java                 # Tool set main class
│   │   ├── create/                        # Node creation tools
│   │   ├── edit/                          # Node editing tools
│   │   ├── move/                          # Node moving tools
│   │   ├── delete/                        # Node deletion tools
│   │   ├── search/                        # Node search tools
│   │   ├── read/                          # Node read tools
│   │   └── selection/                     # Node selection tools
│   ├── mcpserver/                          # MCP server
│   │   └── ModelContextProtocolServer.java
│   ├── restapi/                            # REST API
│   │   └── RestApiServer.java
│   └── edits/                              # AI edit tracking
│       ├── AIEdits.java
│       └── AiEditsStateIconProvider.java
├── frontend/                               # Web frontend (Vue 3)
│   ├── src/
│   │   ├── components/                    # Vue components
│   │   ├── views/                         # Page views
│   │   └── stores/                        # Pinia state management
│   └── package.json
└── build.gradle                            # Gradle build config
```

### Adding a New AI Provider

1. Create a service class:
```java
public class NewProviderAIService implements IAIService {
    @Override
    public ChatResponse chat(ChatRequest request) {
        // implement AI call logic
    }
}
```

2. Register in the factory:
```java
// Add to AIServiceFactory.java
case "newprovider":
    return new NewProviderAIService(configuration);
```

3. Add config keys:
```properties
ai_newprovider_key=
ai_newprovider_service_address=
ai_newprovider_model_list=
```

---

## 🤝 Contributing

### Filing Issues
- Describe the problem
- Provide reproduction steps
- Attach log files
- Include environment info (OS, Java version, Freeplane version)

### Pull Requests
1. Fork this repo
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit: `git commit -m 'Add amazing feature'`
4. Push: `git push origin feature/amazing-feature`
5. Open a PR

### Code Style
- **Java**: follow Google Java Style
- **Frontend**: follow Vue Style Guide
- **Commit messages**: use imperative mood (e.g. "Fix bug" not "Fixed bug")
- **Tests**: new features must include unit tests

---

## 📄 License

This project is licensed under **GNU General Public License v2.0**

- Consistent with the Freeplane main project
- Free to use, modify, and distribute
- Modified code must remain open source

See the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgements

- **Freeplane Team**: the powerful mind mapping platform
- **LangChain4j**: excellent Java AI integration framework
- **OpenRouter / Google / Baidu / Alibaba**: AI model services
- **Community contributors**: all Issue reporters and PR authors

---

## 📞 Contact

- **GitHub Issues**: https://github.com/ymy-yry/freeplane-ai-recreating/issues
- **Email**: [238966298g@gmail.com](mailto:238966298g@gmail.com)
- **Discussions**: https://github.com/ymy-yry/freeplane-ai-recreating/discussions

---

**Empower your mind maps with AI!** 🚀
