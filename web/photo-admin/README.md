# Photo Admin

本地预览：

```bash
python3 -m http.server 4173 -d web/photo-admin
```

浏览器打开：

```text
http://127.0.0.1:4173
```

使用方式：

1. 先点 `加载 Demo` 看页面效果。
2. 要看真实服务器图片时，填写：
   - `serverUrl`
   - `pair_id`
   - `session_token`
3. 再点 `连接服务器`。

后端依赖的接口：
- `GET /v1/photos/list/{pair_id}`
- `GET /v1/photos/file/{photo_id}`
- `GET /v1/photos/summary/{pair_id}`
