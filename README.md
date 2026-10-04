# Dsers — 多平台订单同步中台

把电商平台的订单自动流转到供应商发货，再把物流单号回写平台。订单状态全程可追踪。

目前对接 Shopify。

## 背景

跨境卖家在平台卖货，实际由供应商代发，中间的流程基本靠人工：

逐个导出订单 → 逐个下单给供应商 → 拿回单号再逐个回填平台

订单量一上来，这三步就是纯体力活，还容易出错 —— 漏单、重复下单、状态对不上。

这个项目把它自动化。真正要解决的不是「怎么调接口」，而是**数据在平台、中台、供应商三方之间怎么保持一致**。

## 闭环

订单状态机：`PENDING` → `ORDERED` → `SHIPPED` → `SYNCED`

| 步 | 动作 | 接口 | 走完变成 |
|---|---|---|---|
| 1 | 从 Shopify 拉取订单 | `POST /orders/pull` | PENDING |
| 2 | 下单给供应商 | `POST /orders/{id}/place` | ORDERED |
| 3 | 查供应商发货情况，取回运单号 | `POST /orders/{id}/refresh-shipping` | SHIPPED |
| 4 | 回写运单号到 Shopify | `POST /orders/{id}/push-tracking` | SYNCED |

另有 `GET /orders/list`、`GET /orders/{id}` 两个查询接口，以及 `POST /orders/sync` 单条同步。

## 三个设计点

### 1. 字段三分区

平台数据同步回来时要覆盖本地记录，但 `status` 是我们自己的业务状态 ——
一起覆盖的话，刚推进到「已下单」的订单会被平台数据打回「待处理」。

按「谁说了算」把字段划成三区：

| 区 | 内容 | 谁能写 |
|---|---|---|
| A 区 | 平台镜像（订单号、金额、收货地址……） | 同步逻辑 |
| B 区 | 业务字段（`status`、`tracking_number`、`supplier_order_id`……） | 只有业务代码 |
| C 区 | 系统字段（`id`、`create_time`、`update_time`） | 数据库自己 |

B 区不是一个「记得别碰」的约定，而是**在 SQL 层面没有写入入口**：
覆盖平台数据的 `updateZoneA` 里根本不出现这些列。

### 2. 两层幂等

| 层 | 风险 | 手段 |
|---|---|---|
| 对外 | 下单请求超时重试 → 供应商重复下单 | 请求里带 `clientOrderNo`（本地订单 ID），对方表上有唯一索引 |
| 对内 | 拉取接口被重复触发 | `(platform, platform_order_id)` 唯一索引 + 先查后插，重复拉取只刷新平台数据 |

幂等是失败补偿的前提 —— 没有它，超时之后连重试都不敢。

### 3. 回写的两步 API，和为什么它不加事务

Shopify 把订单拆成三层：

```
order（买家下的单）
  └── fulfillment_order（发货单，按发货地点拆分）
        └── fulfillment（实际发出的包裹，带运单号）
```

回写运单号时必须指明是哪张发货单，所以要分两步：先查发货单 ID，再提交履约。

**这一步刻意没有加 `@Transactional`。** 回写中间要发两次 HTTP，
而本地事务只能回滚数据库、回滚不了已经发出去的请求 ——
如果 Shopify 那边已经发货成功、本地却回滚了，反而会造出
「平台已发货、我们假装什么都没发生」的假一致。

## 技术栈

Java 17 · Spring Boot 3.3 · MyBatis-Plus 3.5 · MySQL 8 · RestTemplate

## 本地运行

1. 建库建表 —— 建表 SQL 见 [docs/数据库设计.md](docs/数据库设计.md)
2. 复制配置模板并填上自己的值：

   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```

   需要填：数据库连接、Shopify 店铺地址、Admin API access token

3. 启动：

   ```bash
   mvn spring-boot:run
   ```

供应商侧由一个独立的模拟服务提供（`dsers-supplier`），它实现了
下单、发货、查询物流三个接口，用来把整条链路跑通。

## 目录结构

```
src/main/java/com/example/dsers/
├── Controller/     HTTP 接口
├── service/        业务逻辑（OrderService、OrderSyncService）
├── client/         外部系统调用（ShopifyClient、SupplierClient）
├── converter/      DTO → 实体
├── mapper/         MyBatis-Plus Mapper，含四个手写 SQL
├── entity/         数据库实体
├── dto/            外部接口的数据结构
├── vo/             返回给前端的数据结构
├── enums/          订单状态、同步结果
└── common/         统一响应、异常处理
```

## 文档

| 文档 | 内容 |
|---|---|
| [设计文档](docs/设计文档.md) | 系统设计、异常分支、对接踩坑记录 |
| [数据库设计](docs/数据库设计.md) | 表结构与建表 SQL、字段来源说明 |
| [开发计划](docs/开发计划.md) | 阶段划分与实际完成情况 |

## 已知限制

这些是明确知道、但当前没有做的：

- **多履约单未支持** —— 一个订单拆成多张发货单时（多仓、多供应商），
  当前只取第一张。单仓单供应商场景下成立，多仓会停在 `partially_fulfilled`
- **拉取没有翻页** —— Shopify 单次最多返回 250 条，超过的部分没有取
- **失败补偿未实现** —— `sync_failed` 字段已预留，
  但回写失败目前是直接抛异常，还没有状态机兜底和主动对账

补偿的顺序是刻意这样排的：先把主干链路（成功了怎么办）跑通，
确认要为哪几种失败做补偿，再接补偿逻辑。
