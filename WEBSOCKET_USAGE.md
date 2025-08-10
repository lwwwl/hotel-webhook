# WebSocket连接使用说明

## 概述

WebSocket服务支持客服端和客人端两种连接方式，通过不同的参数来区分用户类型。

## 配置说明

WebSocket服务器地址通过Spring配置文件进行配置：

```properties
# application.properties
websocket.server.url=ws://localhost:7766
```

如果不配置，默认使用 `ws://localhost:7766`。

## 连接流程

### 1. 获取连接信息

#### 客服端连接
```http
POST /api/websocket/connect/agent
Content-Type: application/x-www-form-urlencoded

userId=12345
```

响应示例：
```json
{
    "success": true,
    "message": "客服端WebSocket连接信息获取成功",
         "wsUrl": "ws://localhost:7766/ws/notify?userId=12345&connectionId=MTIzNDU6YWdlbnQ6MjAyNC0wMS0wMVQxMjowMDowMA==",
    "wsToken": "MTIzNDU6YWdlbnQ6MjAyNC0wMS0wMVQxMjowMDowMA==",
    "userId": "12345",
    "userType": "agent"
}
```

#### 客人端连接
```http
POST /api/websocket/connect/guest
Content-Type: application/x-www-form-urlencoded

guestId=67890
```

响应示例：
```json
{
    "success": true,
    "message": "客人端WebSocket连接信息获取成功",
         "wsUrl": "ws://localhost:7766/ws/notify?guestId=67890&connectionId=Njc4OTA6Z3Vlc3Q6MjAyNC0wMS0wMVQxMjowMDowMA==",
    "wsToken": "Njc4OTA6Z3Vlc3Q6MjAyNC0wMS0wMVQxMjowMDowMA==",
    "userId": "67890",
    "userType": "guest"
}
```

### 2. 建立WebSocket连接

使用返回的`wsUrl`建立WebSocket连接：

#### 客服端
```javascript
const ws = new WebSocket('ws://localhost:7766/ws/notify?userId=12345&connectionId=MTIzNDU6YWdlbnQ6MjAyNC0wMS0wMVQxMjowMDowMA==');
```

#### 客人端
```javascript
const ws = new WebSocket('ws://localhost:7766/ws/notify?guestId=67890&connectionId=Njc4OTA6Z3Vlc3Q6MjAyNC0wMS0wMVQxMjowMDowMA==');
```

## 消息格式

### 心跳消息
客户端发送：
```
ping
```

服务端响应：
```
pong
```

### 通知消息
服务端推送的通知消息格式：
```json
{
    "type": "notification",
    "data": {
        "message": "新消息通知",
        "timestamp": "2024-01-01T12:00:00"
    }
}
```

## API接口

### 检查用户在线状态
```http
GET /api/websocket/status/{userId}
```

响应示例：
```json
{
    "success": true,
    "userId": "12345",
    "isGuestOnline": false,
    "isAgentOnline": true,
    "isOnline": true
}
```

### 获取在线统计
```http
GET /api/websocket/stats
```

响应示例：
```json
{
    "success": true,
    "onlineGuestCount": 10,
    "onlineAgentCount": 5,
    "totalConnectionCount": 15
}
```

## 前端集成示例

### React Hook示例

#### 客人端集成（hotel-management-guest-web）

客人端在验证成功后自动建立WebSocket连接，支持以下功能：

1. **自动连接管理**：验证成功后自动建立WebSocket连接
2. **连接状态显示**：在聊天页面显示WebSocket连接状态
3. **自动重连**：连接断开后自动尝试重连
4. **心跳保活**：30秒发送一次心跳包
5. **全局状态管理**：通过React Context在整个应用中共享连接状态

**主要文件结构：**
```
src/
├── api/
│   └── websocket.ts          # WebSocket API接口
├── hooks/
│   └── useWebSocket.ts       # WebSocket管理Hook
├── contexts/
│   └── WebSocketContext.tsx  # 全局WebSocket上下文
├── pages/
│   ├── VerifyPage.tsx        # 验证页面（建立连接）
│   └── ChatPage.tsx          # 聊天页面（显示状态）
└── App.tsx                   # 应用入口（提供上下文）
```

**使用流程：**
1. 用户在验证页面输入验证码
2. 验证成功后，自动调用WebSocket API获取连接信息
3. 建立WebSocket连接并存储到全局上下文
4. 跳转到聊天页面，显示连接状态
5. 所有页面都可以通过`useWebSocketContext()`访问WebSocket状态

**环境配置：**
```bash
# .env 文件
VITE_WEBSOCKET_API_URL=http://localhost:7766
```

#### 通用React Hook示例
```typescript
import { useState, useEffect, useRef } from 'react';

interface WebSocketConnection {
    wsUrl: string;
    wsToken: string;
    userId: string;
    userType: string;
}

export const useWebSocket = (userId: string, userType: 'agent' | 'guest') => {
    const [isConnected, setIsConnected] = useState(false);
    const [messages, setMessages] = useState<any[]>([]);
    const wsRef = useRef<WebSocket | null>(null);

    const connect = async () => {
        try {
            // 获取连接信息
            const response = await fetch(`/api/websocket/connect/${userType}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: userType === 'agent' 
                    ? `userId=${userId}`
                    : `guestId=${userId}`
            });

            const data: WebSocketConnection = await response.json();
            
            if (data.success) {
                // 建立WebSocket连接
                const ws = new WebSocket(data.wsUrl);
                wsRef.current = ws;

                ws.onopen = () => {
                    setIsConnected(true);
                    console.log('WebSocket连接已建立');
                };

                ws.onmessage = (event) => {
                    const message = JSON.parse(event.data);
                    setMessages(prev => [...prev, message]);
                };

                ws.onclose = () => {
                    setIsConnected(false);
                    console.log('WebSocket连接已关闭');
                };

                ws.onerror = (error) => {
                    console.error('WebSocket错误:', error);
                };
            }
        } catch (error) {
            console.error('获取WebSocket连接信息失败:', error);
        }
    };

    const sendMessage = (message: string) => {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            wsRef.current.send(message);
        }
    };

    const disconnect = () => {
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
        }
    };

    useEffect(() => {
        connect();
        return () => disconnect();
    }, [userId, userType]);

    return {
        isConnected,
        messages,
        sendMessage,
        disconnect
    };
};
```

## 注意事项

1. **连接参数**：客服端使用`userId`参数，客人端使用`guestId`参数
2. **连接标识**：每个连接都会生成唯一的连接标识，用于会话管理
3. **心跳机制**：客户端应定期发送`ping`消息保持连接活跃
4. **错误处理**：连接断开时应自动重连
5. **多端登录**：同一用户可以在多个设备上同时登录
