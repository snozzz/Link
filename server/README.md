# Link Server

这是 `Link` 的最小服务端骨架，用来承接三类同步能力：

1. 邀请码校验与会话签发
2. 配对状态查询
3. 聊天消息与 usage 时间线同步

## 运行方式

```bash
cd server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

## 当前状态

- 使用内存存储，只适合本地开发
- 已有邀请码生成、解锁、配对状态、消息同步、usage 上传接口
- 下一步会补数据库、真正的设备密钥校验和 token 签发
