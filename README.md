# ✈ 空运代理智能管理系统 (AirFreight Agent System)

基于空运代理15步完整工作流 + 财务客户对接 + 国外航空/海关/银行API对接的端到端智能管理系统。

## 系统架构

```
air-freight-agent-system/
├── backend/                  # Spring Boot 3 后端
│   ├── src/main/java/com/airfreight/
│   │   ├── AirFreightApplication.java   # 启动入口
│   │   ├── config/                      # 安全、Swagger配置
│   │   ├── controller/                  # REST API控制器
│   │   ├── entity/                      # JPA实体类
│   │   ├── repository/                  # 数据访问层
│   │   ├── service/                     # 业务逻辑层
│   │   ├── api/                         # 国外对接接口
│   │   └── dto/                         # 数据传输对象
│   └── src/main/resources/
├── frontend/                 # Vue 3 前端
│   └── src/
│       ├── views/            # 页面组件
│       ├── api/              # API接口封装
│       └── router/           # 路由配置
└── 文档/                      # 设计文档
```

## 15步空运代理工作流

| 阶段 | 步骤 | 系统模块 |
|------|------|----------|
| 前期准备 | 1. 需求确认与询价 | 询价模块 |
| 前期准备 | 2. 货物合规校验 | 询价模块 |
| 前期准备 | 3. 单证初备 | 运单模块 |
| 核心操作 | 4. 订舱申请与确认 | 订舱模块 |
| 核心操作 | 5. 货物集中与预处理 | 货物模块 |
| 核心操作 | 6. 单证审核与提单确认 | 订舱模块 |
| 核心操作 | 7. 出口报关 | 报关模块 |
| 核心操作 | 8. 货物入仓与装机 | 订舱模块 |
| 核心操作 | 9. 航班运输与在途跟踪 | 跟踪模块 |
| 收尾交付 | 10. 目的港到货通知 | 通知模块 |
| 收尾交付 | 11. 换单 | 提货模块 |
| 收尾交付 | 12. 目的港清关 | 报关模块 |
| 收尾交付 | 13. 缴纳税费 | 财务模块 |
| 收尾交付 | 14. 提货 | 提货模块 |
| 收尾交付 | 15. 末端派送 | 派送模块 |

## 国外对接

- **IATA航空API**：订舱、跟踪、AWB电子化
- **海关电子申报**：中国e-Customs、欧盟AIS、美国ACE
- **SWIFT国际支付**：ISO 20022标准电汇

## 技术栈

- **后端**：Spring Boot 3 + JPA + H2(开发)/MySQL(生产) + Redis + JWT
- **前端**：Vue 3 + Vite + Element Plus + Pinia + Axios
- **文档**：Swagger/OpenAPI 3
- **构建**：Maven + npm

## 快速启动

### 后端
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
后端启动在 http://localhost:8080
Swagger文档: http://localhost:8080/swagger-ui.html
H2控制台: http://localhost:8080/h2-console

### 前端
```bash
cd frontend
npm install
npm run dev
```
前端启动在 http://localhost:5173

## API接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 计算体积重 | POST | /api/shipments/calculate-volume-weight | 长×宽×高/6000 |
| 获取报价 | POST | /api/shipments/quotation | 生成完整报价 |
| 创建运单 | POST | /api/shipments/create-from-quotation | 确认报价后创建 |
| 查询运单 | GET | /api/shipments | 列表查询 |
| 提交订舱 | POST | /api/bookings | 提交订舱申请 |
| 确认舱位 | PUT | /api/bookings/{id}/confirm | 航司确认 |
| 生成账单 | POST | /api/financials/invoice/{shipmentId} | 费用账单 |
| SWIFT支付 | POST | /api/financials/{id}/swift-payment | 国际支付 |

## 开发者

- GitHub: [rain-fly](https://github.com/rain-fly)
- Email: diaoyufei@hotmail.com