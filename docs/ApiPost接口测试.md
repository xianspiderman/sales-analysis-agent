# ApiPost 接口测试

## 1. 测试准备

启动服务后，在 ApiPost 中建立环境：

| 变量 | 值 |
|---|---|
| `baseUrl` | `http://localhost:8087` |
| `token` | 登录成功后填写响应中的 Token |
| `localPassword` | `LocalSales@2026` |

统一请求头：

```text
Content-Type: application/json
Authorization: {{token}}
```

登录接口和“未登录 401”测试不携带 `Authorization`。

## 2. 本地初始化账号与 BCrypt

`data.sql` 为本地初始化账号写入 `LocalSales@2026` 对应的 BCrypt 摘要。该固定口令仅用于本地初始化数据；登录请求传入原始口令，数据库只保存摘要。

登录请求：

```json
{
  "loginName": "sales_director",
  "password": "{{localPassword}}"
}
```

## 3. 登录测试

### 3.1 正确登录

```bash
curl --location 'http://localhost:8087/auth/login' \
--header 'Content-Type: application/json' \
--data '{
  "loginName": "sales_director",
  "password": "LocalSales@2026"
}'
```

预期：HTTP 200，并返回 `token`、`username`、`role`、`regionId` 和 `repId`。

本地初始化账号：

| 角色 | 登录账号 |
|---|---|
| 销售总监 | `sales_director` |
| 华东主管 | `east_manager` |
| 华东销售员 | `east_zhangwei` |

### 3.2 错误密码

```bash
curl --location 'http://localhost:8087/auth/login' \
--header 'Content-Type: application/json' \
--data '{
  "loginName": "sales_director",
  "password": "WRONG_PASSWORD"
}'
```

预期：HTTP 401，错误码为 `INVALID_CREDENTIALS`。

### 3.3 不存在的账号

```bash
curl --location 'http://localhost:8087/auth/login' \
--header 'Content-Type: application/json' \
--data '{
  "loginName": "not_exists",
  "password": "WRONG_PASSWORD"
}'
```

预期：与错误密码相同，统一返回 HTTP 401 和 `INVALID_CREDENTIALS`。

### 3.4 退出登录

```bash
curl --location --request POST 'http://localhost:8087/auth/logout' \
--header 'Authorization: YOUR_TOKEN'
```

## 4. 未登录异常

```bash
curl --location 'http://localhost:8087/test/tool/region-ranking' \
--header 'Content-Type: application/json' \
--data '{
  "startDate": "2025-01-01",
  "endDate": "2027-12-31"
}'
```

预期：HTTP 401，错误码为 `NOT_LOGIN`。

## 5. 数据权限测试

### 5.1 销售员排名

分别使用总监、华东主管和华东销售员 Token 执行同一请求：

```bash
curl --location 'http://localhost:8087/test/tool/top-reps' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "startDate": "2025-01-01",
  "endDate": "2027-12-31",
  "regionName": null,
  "topN": 20
}'
```

预期：

- 总监可以看到多个大区的销售员。
- 华东主管只能看到华东区销售员。
- 华东销售员只能看到本人。

### 5.2 越权大区

使用华东主管 Token 查询华南区：

```bash
curl --location 'http://localhost:8087/test/tool/query-orders' \
--header 'Authorization: YOUR_EAST_MANAGER_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "startDate": "2025-01-01",
  "endDate": "2027-12-31",
  "regionName": "华南区",
  "reqName": null,
  "limit": 10
}'
```

预期：返回“无权查询其他大区数据”。`reqName` 是当前测试 Controller 使用的销售员姓名字段。

## 6. 参数校验测试

### 6.1 开始日期晚于结束日期

```bash
curl --location 'http://localhost:8087/test/tool/query-orders' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "startDate": "2027-12-31",
  "endDate": "2025-01-01",
  "regionName": null,
  "reqName": null,
  "limit": 10
}'
```

预期：返回“开始日期不得晚于结束日期”。

### 6.2 非法 TopN

```bash
curl --location 'http://localhost:8087/test/tool/top-reps' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "startDate": "2025-01-01",
  "endDate": "2027-12-31",
  "regionName": null,
  "topN": 100
}'
```

预期：返回 TopN 范围错误。

### 6.3 非法统计维度

```bash
curl --location 'http://localhost:8087/test/tool/bar-chart' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "dimension": "unknown",
  "startDate": "2025-01-01",
  "endDate": "2027-12-31",
  "title": "销售统计"
}'
```

预期：返回统计维度错误。

## 7. Redis 缓存测试

连续执行两次完全相同的请求：

```bash
curl --location 'http://localhost:8087/test/tool/month-over-month' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "currentStart": "2026-06-01",
  "currentEnd": "2026-06-30",
  "prevStart": "2026-05-01",
  "prevEnd": "2026-05-31",
  "regionName": null
}'
```

预期：

1. 第一次日志出现“销售额缓存未命中”。
2. 第二次日志出现“销售额缓存命中”。
3. 总监、主管、销售员分别使用 `COMPANY`、`REGION`、`REP` 范围 Key。

## 8. LangChain4j 同步入口

```bash
curl --location 'http://localhost:8087/agent/chat' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "sessionId": "apipost-sync-001",
  "message": "查询2025年1月1日至2027年12月31日销售员业绩前10名"
}'
```

预期：HTTP 200，一次性返回完整回答。主管和销售员的结果不得超出自己的数据范围。

服务端会把客户端 `sessionId` 转换成 `userId:sessionId` 后再读写 MySQL 对话记忆。可以让主管和销售员故意使用相同的 `sessionId` 分别提问：两者应各自开始独立会话，销售员的回答中不能出现主管上一轮的措辞或数据。

## 9. LangChain4j SSE 入口

```bash
curl --no-buffer --location 'http://localhost:8087/agent/chat/stream' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Accept: text/event-stream' \
--header 'Content-Type: application/json' \
--data '{
  "sessionId": "apipost-stream-001",
  "message": "查询2025年1月1日至2027年12月31日销售员业绩前10名"
}'
```

预期持续收到：

```text
event:token
data:...

event:done
data:[DONE]
```

使用同一个主管 Token 测试同步与 SSE，两条链路的数据范围应一致。

## 10. 统一入口与三种模式

统一入口支持 `LANGCHAIN4J`、`AGENTSCOPE_SINGLE` 和 `AGENTSCOPE_TEAM`。请求省略 `mode` 时使用 `sales-agent.routing.default-mode`；`allow-request-override=true` 时可以在请求中指定模式。

### 10.1 统一同步问答

```bash
curl --location 'http://localhost:8087/analysis/chat' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "sessionId": "routing-sync-001",
  "message": "统计今年各大区销售额并生成柱状图",
  "mode": "AGENTSCOPE_TEAM"
}'
```

响应结构：

```json
{
  "sessionId": "routing-sync-001",
  "reply": "...",
  "route": {
    "requestedMode": "AGENTSCOPE_TEAM",
    "actualMode": "AGENTSCOPE_TEAM",
    "fallback": false
  },
  "execution": {
    "requestId": "...",
    "rootAgent": "sales-team-supervisor",
    "status": "success"
  }
}
```

`execution` 在两种 AgentScope 模式下返回请求级执行摘要，在 `LANGCHAIN4J` 模式下为 `null`。摘要还包含总耗时、专家调用、推理轮次、模型调用、工具调用和 Token 用量。

分别把 `mode` 改为以下值，可以从同一入口验证三种执行模式：

| mode | 执行链路 |
|---|---|
| `LANGCHAIN4J` | LangChain4j AiServices |
| `AGENTSCOPE_SINGLE` | AgentScope 单 ReActAgent |
| `AGENTSCOPE_TEAM` | Supervisor 调度三类专家 |

### 10.2 统一 SSE 问答

```bash
curl --no-buffer --location 'http://localhost:8087/analysis/chat/stream' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Accept: text/event-stream' \
--header 'Content-Type: application/json' \
--data '{
  "sessionId": "routing-stream-001",
  "message": "生成近6个月销售趋势折线图",
  "mode": "AGENTSCOPE_SINGLE"
}'
```

首个事件为 `route`：

```text
event:route
data:{"requestedMode":"AGENTSCOPE_SINGLE","actualMode":"AGENTSCOPE_SINGLE","fallback":false}
```

AgentScope 模式随后可以输出 `agent_start`、`model_start`、`model_end`、`token`、`tool_start`、`tool_end`、`summary` 和 `done`。LangChain4j 模式通过统一入口输出 `token`、`tool_start`、`tool_end` 和 `done`。错误统一使用 `error` 事件。

当主模式在首个 `token` 或 `tool_start` 之前抛出异常，并且路由配置启用 fallback 时，会先输出 `fallback` 事件，再执行配置的降级模式。`fallback` 事件中的 `requestedMode` 与 `actualMode` 可用于确认模式切换结果。

### 10.3 清理指定模式会话

```bash
curl --location --request DELETE \
'http://localhost:8087/analysis/session/routing-sync-001?mode=AGENTSCOPE_TEAM' \
--header 'Authorization: YOUR_TOKEN'
```

## 11. AgentScope 专用入口

### 11.1 单 ReActAgent 同步问答

```bash
curl --location 'http://localhost:8087/agentscope/chat' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "sessionId": "agentscope-single-001",
  "message": "查询近6个月月度销售趋势"
}'
```

响应包含 `sessionId`、`reply` 和 `execution`。单 ReActAgent 可以调用完整的 12 个销售工具。

### 11.2 Supervisor 多 Agent 同步问答

```bash
curl --location 'http://localhost:8087/agentscope/team/chat' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Content-Type: application/json' \
--data '{
  "sessionId": "agentscope-team-001",
  "message": "统计今年各大区销售额，生成柱状图并检查异常"
}'
```

Supervisor 根据目标委派数据分析、图表生成和异常诊断专家。三类专家分别使用 8、3、1 个职责范围内的工具，结果由 Supervisor 汇总。

### 11.3 AgentScope SSE

单 ReActAgent：

```bash
curl --no-buffer --location 'http://localhost:8087/agentscope/chat/stream' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Accept: text/event-stream' \
--header 'Content-Type: application/json' \
--data '{
  "sessionId": "agentscope-single-stream-001",
  "message": "查询近6个月月度销售趋势"
}'
```

Supervisor：

```bash
curl --no-buffer --location 'http://localhost:8087/agentscope/team/chat/stream' \
--header 'Authorization: YOUR_TOKEN' \
--header 'Accept: text/event-stream' \
--header 'Content-Type: application/json' \
--data '{
  "sessionId": "agentscope-team-stream-001",
  "message": "生成近6个月销售趋势折线图并检查异常"
}'
```

流结束前的 `summary` 事件返回本次调用的执行摘要，最后一个业务完成事件为 `done`。

### 11.4 AgentScope 会话隔离与清理

AgentScope StateStore 使用登录用户 ID 与客户端 `sessionId` 隔离会话；Supervisor 使用 `team:` 命名空间，与同名的单 ReActAgent 会话隔离。

```bash
curl --location --request DELETE \
'http://localhost:8087/agentscope/session/agentscope-single-001' \
--header 'Authorization: YOUR_TOKEN'

curl --location --request DELETE \
'http://localhost:8087/agentscope/team/session/agentscope-team-001' \
--header 'Authorization: YOUR_TOKEN'
```

## 12. Tool 测试接口

| 方法 | 地址 | 用途 |
|---|---|---|
| POST | `/test/tool/query-orders` | 查询订单明细 |
| POST | `/test/tool/top-reps` | 销售员排名 |
| POST | `/test/tool/region-ranking` | 大区排名 |
| POST | `/test/tool/top-products` | 产品排名 |
| POST | `/test/tool/month-over-month` | 环比分析 |
| POST | `/test/tool/year-over-year` | 同比分析 |
| POST | `/test/tool/monthly-trend` | 月度趋势 |
| POST | `/test/tool/line-chart` | 折线图数据 |
| POST | `/test/tool/bar-chart` | 柱状图数据 |
| POST | `/test/tool/pie-chart` | 饼图数据 |
| POST | `/test/tool/detect-anomalies` | 异常检测 |

## 13. 运行结果截图

- 正确登录响应。
- 总监、主管、销售员三种排名结果对比。
- 主管越权请求被拒绝。
- 同步 Agent 回答。
- SSE 的 token 与 done 事件。
- Redis 未命中和命中日志。
- 异常检测结果。
