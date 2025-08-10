# Hotel Webhook Service

这是一个基于Spring Boot的酒店客服咨询系统WebSocket和Webhook服务，用于管理客人和客服的实时通信。

## 系统架构

### 核心组件

1. **WebSocket连接管理**
   - `WebSocketSessionManager`: 管理用户WebSocket连接的生命周期
   - `NotifyWebSocketHandler`: 处理WebSocket连接和消息
   - 支持多端登录和连接管理

2. **Chatwoot Webhook处理**
   - `ChatwootWebhookProcessor`: 解析和处理Chatwoot webhook事件
   - `NotificationService`: 通知服务，向相关用户推送实时通知
   - 支持多种事件类型：消息创建、会话创建、会话更新等

3. **数据模型**
   - `UserSession`: 用户会话信息
   - `ChatwootEvent`: Chatwoot事件模型
   - `NotificationMessage`: 通知消息模型

### 系统流程

1. **用户连接**
   - 客人或客服通过WebSocket连接到 `/ws/notify?userId=xxx&userType=guest|agent`
   - 系统记录用户会话信息到内存中

2. **消息处理**
   - Chatwoot发送webhook到 `/chatwoot-webhook/callback`
   - 系统解析事件并创建通知消息
   - 通过WebSocket向相关用户推送通知

3. **实时通知**
   - 客户端收到通知后，拉取最新消息或会话列表
   - 支持心跳检测和连接状态管理

## API接口

### WebSocket接口

```
GET /ws/notify?userId={userId}&userType={userType}
```

参数说明：
- `userId`: 用户ID（必填）
- `userType`: 用户类型，guest（客人）或agent（客服）

### HTTP接口

#### Webhook回调
```
POST /chatwoot-webhook/callback
Content-Type: application/json

{
  "event": "message_created",
  "message": {
    "id": "123",
    "content": "Hello",
    "sender_id": "456",
    "sender_type": "user"
  },
  "conversation": {
    "id": "789",
    "inbox_id": "1"
  }
}
```

#### 健康检查
```
GET /chatwoot-webhook/health
```

## 配置说明

### 应用配置

- 端口：默认8080
- WebSocket路径：/ws/notify
- Webhook路径：/chatwoot-webhook/callback

### 心跳配置

- 心跳间隔：5分钟
- 连接超时：5分钟
- 统计日志：1分钟

## 部署说明

### 环境要求

- Java 21+
- Maven 3.6+
- Spring Boot 3.5.4

### 启动命令

```bash
# 编译
mvn clean package

# 运行
java -jar target/hotel-webhook-0.0.1-SNAPSHOT.jar
```

### Docker部署

```bash
# 构建镜像
docker build -t hotel-webhook .

# 运行容器
docker run -p 8080:8080 hotel-webhook
```

## 开发说明

### 项目结构

```
src/main/java/com/example/hotelwebhook/
├── config/                 # 配置类
│   ├── AppConfig.java     # 应用配置
│   └── WebSocketConfig.java # WebSocket配置
├── controller/            # 控制器
│   └── WebhookController.java # Webhook控制器
├── model/                # 数据模型
│   ├── UserSession.java  # 用户会话模型
│   ├── ChatwootEvent.java # Chatwoot事件模型
│   └── NotificationMessage.java # 通知消息模型
├── service/              # 服务层
│   ├── WebSocketSessionManager.java # WebSocket会话管理
│   ├── ChatwootWebhookProcessor.java # Webhook处理器
│   ├── NotificationService.java # 通知服务
│   └── WebSocketHeartbeatTask.java # 心跳任务
└── websocket/           # WebSocket处理
    └── NotifyWebSocketHandler.java # WebSocket处理器
```

### 扩展功能

1. **会话参与者管理**
   - 根据conversationId获取会话参与者列表
   - 支持群聊和私聊

2. **消息持久化**
   - 集成数据库存储消息历史
   - 支持消息搜索和统计

3. **用户状态管理**
   - 在线状态显示
   - 输入状态提示

4. **安全认证**
   - JWT token验证
   - 用户权限控制

## 监控和日志

### 日志级别

- INFO: 连接建立、断开、事件处理
- DEBUG: 详细的消息处理过程
- ERROR: 错误和异常信息

### 监控指标

- 在线用户数量
- 总连接数
- 消息处理统计
- 系统性能指标

## 故障排除

### 常见问题

1. **WebSocket连接失败**
   - 检查URL参数是否正确
   - 确认网络连接正常

2. **Webhook回调失败**
   - 检查请求格式是否正确
   - 查看服务器日志

3. **消息推送失败**
   - 确认用户在线状态
   - 检查WebSocket连接状态

### 调试方法

1. 启用DEBUG日志级别
2. 使用WebSocket测试工具
3. 监控系统资源使用情况
