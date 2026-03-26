# Link Server

这是 `Link` 的最小可用服务端，当前负责 5 类能力：

1. 系统生成邀请码和配对码
2. 用 `invite_key + pair_code` 解锁并签发 `session_token`
3. 查询当前配对状态
4. 同步聊天消息
5. 上传和读取双方的 Moments usage 快照

## 运行方式

```bash
cd server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8080
```

服务启动后：

- 健康检查：`GET /health`
- 本地数据库文件：`server/link.db`

## 最小联调流程

### 1. 生成一对情侣的邀请信息

```bash
curl -X POST http://127.0.0.1:8080/v1/internal/invites/create
```

返回示例：

```json
{
  "invite_key": "abc123...",
  "pair_code": "7K9M",
  "expires_in_minutes": 60,
  "remaining_slots": 2
}
```

同一组 `invite_key + pair_code` 可以给两个人分别登录，最多 2 人。

### 2. 第一个人登录

```bash
curl -X POST http://127.0.0.1:8080/v1/auth/invite/unlock   -H 'Content-Type: application/json'   -d '{
    "invite_key": "你的邀请码",
    "pair_code": "你的配对码",
    "nickname": "snoz",
    "device_public_key": "debug-device-public-key-0001"
  }'
```

### 3. 第二个人登录

```bash
curl -X POST http://127.0.0.1:8080/v1/auth/invite/unlock   -H 'Content-Type: application/json'   -d '{
    "invite_key": "同一个邀请码",
    "pair_code": "同一个配对码",
    "nickname": "girlfriend",
    "device_public_key": "debug-device-public-key-0002"
  }'
```

返回里会有：

- `session_token`
- `pair_id`
- `pair_code`
- `display_name`
- `member_count`

后续接口都要带：

```text
Authorization: Bearer <session_token>
```

### 4. 查询配对状态

```bash
curl http://127.0.0.1:8080/v1/pair/status/pair_7k9m   -H 'Authorization: Bearer <session_token>'
```

### 5. 同步消息

```bash
curl -X POST http://127.0.0.1:8080/v1/messages/sync   -H 'Authorization: Bearer <session_token>'   -H 'Content-Type: application/json'   -d '{
    "pair_id": "pair_7k9m",
    "outgoing_messages": [
      {
        "local_id": "msg-001",
        "body": "hello",
        "sent_at_epoch_millis": 1710000000000
      }
    ]
  }'
```

### 6. 上传与读取 usage

上传：

```bash
curl -X POST http://127.0.0.1:8080/v1/usage/upload   -H 'Authorization: Bearer <session_token>'   -H 'Content-Type: application/json'   -d '{
    "pair_id": "pair_7k9m",
    "captured_at_epoch_millis": 1710000000000,
    "events": [
      {
        "app_name": "抖音",
        "package_name": "com.ss.android.ugc.aweme",
        "time_label": "22:18",
        "duration_label": "14m"
      }
    ]
  }'
```

读取对方最近一次快照：

```bash
curl http://127.0.0.1:8080/v1/usage/latest/pair_7k9m   -H 'Authorization: Bearer <session_token>'
```

## 当前取舍

- 使用 `sqlite3`，适合单机或轻量 VPS 原型
- 邀请码默认 60 分钟有效，最多 2 次使用
- 聊天同步当前返回整段会话，不做增量 cursor
- usage 当前返回对方最近一次快照，不做历史分页
- 还没有接 HTTPS、刷新 token、设备吊销和管理员后台
