# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Key Components

### Hybrid Storage Architecture

inventory-service uses a hybrid storage pattern for high-throughput stock management:

- **Primary Storage (Redis)**: All real-time stock operations (reserve, confirm, cancel, add, decrease)
- **Secondary Storage (MariaDB)**: Periodic snapshots for durability and recovery

Redis keys:
- `inventory:stock:{skuId}` - Total stock quantity
- `inventory:reserved:{skuId}` - Reserved stock quantity
- Available stock = total - reserved

### Stock State Management

Stock uses a two-phase reservation model:

| State | Description |
|-------|-------------|
| `total` | Current available stock in Redis |
| `reserved` | Stock held for pending orders |
| `available` | Calculated as `total - reserved` |

**Stock Lifecycle**:
1. **Initialize**: SKU created -> set total, reserved=0
2. **Reserve**: Order placed -> decrease total, increase reserved
3. **Confirm**: Payment confirmed -> decrease reserved (stock consumed)
4. **Cancel**: Order cancelled -> increase total, decrease reserved (stock returned)

### Redis Lua Scripts

All scripts located in `src/main/resources/redis/script/`:

| Script | Keys | Operation | Return Codes |
|--------|------|-----------|--------------|
| `reserve_stock.lua` | stock, reserved | Decrease stock, increase reserved | -1: not found, -2: insufficient |
| `confirm_stock.lua` | reserved | Decrease reserved | -1: not found, -2: invalid state |
| `cancel_stock.lua` | stock, reserved | Increase stock, decrease reserved | -1: not found, -2: invalid state |
| `add_stock.lua` | stock | Increase stock | -1: not found |
| `decrease_stock.lua` | stock | Decrease stock directly | -1: not found, -2: insufficient |

### Kafka Integration

**Consumed Events** (from other services):

| Event | Source | Action |
|-------|--------|--------|
| `ProductSkuCreatedEvent` | catalog-service | Initialize stock for new SKU |
| `OrderPlacedEvent` | order-service | Reserve stock for order items |
| `OrderConfirmedEvent` | order-service | Confirm reserved stock |
| `OrderCancelledEvent` | order-service | Release reserved stock back to available |

**Published Events** (to other services):

| Event | Trigger | Purpose |
|-------|---------|---------|
| `StockReservedEvent` | Reserve success | Notify order-service reservation complete |
| `StockReservationFailedEvent` | Reserve failure | Notify insufficient stock |
| `StockConfirmedEvent` | Confirm success | Notify stock consumption complete |
| `StockConfirmFailedEvent` | Confirm failure | Notify confirmation error |

### Snapshot Mechanism

`InventorySnapshotScheduler` runs every 60 seconds:
1. Query all stock data from Redis
2. Upsert to MariaDB `inventory` table
3. Provides durability for Redis data recovery

### Database Schema

```sql
CREATE TABLE inventory (
    sku_id         VARCHAR(50) NOT NULL PRIMARY KEY,
    total_stock    INT NOT NULL DEFAULT 0,
    reserved_stock INT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT ck_inventory_stock_non_negative
        CHECK (total_stock >= 0 AND reserved_stock >= 0 AND total_stock >= reserved_stock)
);
```

### API Endpoints

**Query**:
- `GET /api/inventories/{skuId}` - Get stock by SKU ID
- `POST /api/inventories/bulk` - Bulk query by SKU IDs

**Stock Management**:
- `POST /api/inventories/{skuId}/increase` - Add stock
- `POST /api/inventories/increase` - Bulk add stock
- `POST /api/inventories/{skuId}/decrease` - Reduce stock directly
- `POST /api/inventories/decrease` - Bulk reduce stock

## Dependencies

Requires GitHub Packages authentication for common modules:
- `common-core` - Shared exceptions, CloudEvent wrapper, API response
- `common-security` - JWT validation, security configuration

Set `GH_USER` and `GH_TOKEN` environment variables or configure in `~/.gradle/gradle.properties`:
```properties
gpr.user=your-github-username
gpr.token=your-github-token
```
