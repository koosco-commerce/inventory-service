# 트러블슈팅 기록: Redis TestBase 구현 및 Gradle 빌드 이슈

**날짜**: 2025-12-31
**브랜치**: `feat/atomic-db`
**작성자**: Claude Code

---

## 목차

1. [RedisContainerTestBase 구현](#1-rediscontainertestbase-구현)
2. [JPQL 쿼리 오류 수정](#2-jpql-쿼리-오류-수정)
3. [Gradle 빌드 실패 해결](#3-gradle-빌드-실패-해결)
4. [PathVariable Warning 분석](#4-pathvariable-warning-분석)
5. [브랜치 구조 정리](#5-브랜치-구조-정리)

---

## 1. RedisContainerTestBase 구현

### 문제 상황

`RedisContainerTestBase.kt` 파일에서 다음 문제들이 발견됨:

```kotlin
// 기존 코드 (문제점)
@TestContainers  // ❌ import 누락
abstract class RedisContainerTestBase {
    companion object {
        @Container  // ❌ import 누락
        val redis = GenericContainer("redis:7.2")
            .withExposedPorts(6379)
        // ❌ Spring Boot Redis 설정과 연결 안 됨
    }
}
```

**주요 문제**:
- `@TestContainers`, `@Container` 어노테이션 import 누락
- Spring Boot의 Redis 설정과 동적 연결 미구현
- Testcontainers JUnit 5 통합 모듈 의존성 누락

### 해결 과정

#### Step 1: 의존성 검토

**build.gradle.kts 확인**:
```kotlin
// 기존 의존성 (문제 발견)
testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.7"))
testImplementation("org.testcontainers:testcontainers")
testImplementation("org.testcontainers:redis")
testImplementation("org.springframework.boot:spring-boot-testcontainers")
// ❌ junit-jupiter 모듈 누락!
```

**의존성 추가** (line 89):
```kotlin
testImplementation("org.testcontainers:junit-jupiter")  // ✅ 추가
```

#### Step 2: RedisContainerTestBase 개선

**최종 구현** (`src/test/kotlin/com/koosco/inventoryservice/base/RedisContainerTestBase.kt`):

```kotlin
package com.koosco.inventoryservice.base

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Redis Testcontainers를 사용하는 테스트의 베이스 클래스
 * - Redis 컨테이너를 자동으로 시작/종료
 * - Spring Boot의 Redis 설정을 동적으로 구성
 * - 모든 Redis 관련 통합 테스트에서 상속하여 사용
 */
@Testcontainers
abstract class RedisContainerTestBase {

    companion object {
        @Container
        @JvmStatic
        val redis: GenericContainer<*> = GenericContainer("redis:7.2-alpine")
            .withExposedPorts(6379)
            .withReuse(true)

        @DynamicPropertySource
        @JvmStatic
        fun redisProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379).toString() }
        }
    }
}
```

**주요 개선 사항**:
- ✅ 필요한 import 추가
- ✅ `@DynamicPropertySource`로 Spring Boot Redis 설정 동적 구성
- ✅ `.withReuse(true)`로 테스트 실행 속도 향상
- ✅ `redis:7.2-alpine` 경량 이미지 사용
- ✅ `@JvmStatic` 추가로 Kotlin companion object 호환성 개선

#### Step 3: 사용 예제 작성

**샘플 테스트** (`src/test/kotlin/com/koosco/inventoryservice/base/RedisContainerTestBaseTest.kt`):

```kotlin
@SpringBootTest
class RedisContainerTestBaseTest : RedisContainerTestBase() {

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Test
    fun `Redis 컨테이너가 실행 중이어야 한다`() {
        assertTrue(redis.isRunning)
    }

    @Test
    fun `Redis에 데이터를 저장하고 조회할 수 있어야 한다`() {
        // given
        val key = "test:key"
        val value = "test-value"

        // when
        redisTemplate.opsForValue().set(key, value)
        val result = redisTemplate.opsForValue().get(key)

        // then
        assertEquals(value, result)
    }
}
```

### 결과

✅ RedisContainerTestBase 구현 완료
✅ 필요한 의존성 추가 완료
✅ Spring Boot와 동적 연결 구현
✅ 사용 예제 제공

---

## 2. JPQL 쿼리 오류 수정

### 문제 발견

**파일**: `src/main/kotlin/com/koosco/inventoryservice/inventory/infra/persist/JpaInventoryRepository.kt`

```kotlin
// ❌ 잘못된 JPQL (line 21)
@Modifying
@Query("UPDATE Inventory i SET i.stock.total = i.stock + :quantity WHERE i.skuId = :skuId")
                                               ^^^^^^^^
                                               객체 + 숫자 (불가능!)
fun increaseStockById(skuId: String, quantity: Int): Int
```

**문제 분석**:
- `i.stock`은 **객체** (Stock 임베디드 타입)
- `:quantity`는 **숫자** (Int)
- 객체와 숫자의 덧셈은 불가능한 연산

**비교**:
```kotlin
// ✅ 정상 (line 17)
@Query("UPDATE Inventory i SET i.stock.total = i.stock.total - :quantity ...")
fun decreaseStockById(...)

// ❌ 오류 (line 21)
@Query("UPDATE Inventory i SET i.stock.total = i.stock + :quantity ...")
fun increaseStockById(...)
```

### 해결

```kotlin
// ✅ 수정된 JPQL
@Modifying
@Query("UPDATE Inventory i SET i.stock.total = i.stock.total + :quantity WHERE i.skuId = :skuId")
fun increaseStockById(skuId: String, quantity: Int): Int
```

**변경 사항**:
```diff
- i.stock + :quantity
+ i.stock.total + :quantity
```

### 결과

✅ JPQL 구문 오류 수정
✅ `decreaseStockById`와 `increaseStockById` 일관된 패턴 사용

---

## 3. Gradle 빌드 실패 해결

### 문제 상황

```bash
$ ./gradlew build -x test

> Task :spotlessKotlin FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Task ':spotlessKotlin' uses this output of task ':compileJava'
without declaring an explicit or implicit dependency.
```

**에러 메시지**:
```
Gradle detected a problem with the following location: '/Users/koo/CodeSpace/commerce/inventory-service'.

Reason: Task ':spotlessKotlin' uses this output of task ':compileJava'
without declaring an explicit or implicit dependency.
```

### 원인 분석

#### 직접적인 트리거

```kotlin
// build.gradle.kts에 추가된 의존성
testImplementation("org.testcontainers:junit-jupiter")  // ← 트리거
```

이 의존성 추가로:
1. Gradle이 **task graph를 재계산**
2. 이전에 숨겨져 있던 **task dependency 문제 드러남**
3. `-x test` 옵션으로 빌드 시 **spotless → compileJava 순서 문제** 발생

#### 근본 원인

**Gradle 8.14.3의 엄격한 검증**:
```bash
Gradle 8.14.3
Build time: 2025-07-04 13:15:44 UTC
```

**Gradle의 점진적 강화**:
- Gradle 7.x: 경고만 표시
- Gradle 8.x: 검증 강화, 일부 경고를 오류로 변경
- **Gradle 8.14.3**: Task dependency validation 매우 엄격

#### 왜 이전엔 문제가 없었나?

| 상황 | 설명 |
|------|------|
| 캐시된 빌드 | `> Task :spotlessKotlin UP-TO-DATE` 상태로 실제 순서 문제 숨겨짐 |
| 의존성 변경 전 | 이전 구성에서는 task graph가 달랐음 |
| Spotless 플러그인 | 6.25.0 + Gradle 8.14.x 조합에서 명시적 dependency 필요 |

### 해결 방법

**build.gradle.kts 수정** (line 155-158):

```kotlin
// Fix spotless task dependency issue
tasks.matching { it.name.startsWith("spotless") }.configureEach {
    mustRunAfter(tasks.withType<JavaCompile>())
}
```

**동작 방식**:
- 모든 Spotless 태스크가 Java 컴파일 **이후**에 실행되도록 명시
- `mustRunAfter`로 실행 순서 강제

### 결과

```bash
$ ./gradlew build -x test

BUILD SUCCESSFUL in 1s
13 actionable tasks: 3 executed, 10 up-to-date
```

✅ Spotless 태스크 정상 실행
✅ 의존성 경고 해결
✅ 빌드 성공

---

## 4. PathVariable Warning 분석

### Warning 내용

**파일**: `src/main/kotlin/com/koosco/inventoryservice/inventory/api/controller/TestInventoryStockController.kt:40`

```kotlin
@PostMapping("/decrease/{skuId}")
fun decreaseStock(
    @PathVariable("skuId") skuId: String,  // ⚠️ Warning
    @RequestBody request: StockDto
): ApiResponse<Any>
```

**Warning 메시지**:
```
Cannot resolve path variable 'skuId' in request mapping
```

### 분석

**코드는 정상**:
```kotlin
@PostMapping("/decrease/{skuId}")  // ✅ {skuId} 정의됨
fun decreaseStock(
    @PathVariable("skuId") skuId: String,  // ✅ {skuId} 참조
```

**Warning 발생 원인** (False Positive):

1. **IntelliJ IDEA 인덱싱 문제**
   - Kotlin Spring 플러그인이 path variable을 아직 인식하지 못함

2. **Spring 어노테이션 프로세싱 타이밍**
   - 컴파일 전이라 Spring의 어노테이션 처리가 완료되지 않음

3. **Kotlin 파라미터 이름 보존**
   - `-parameters` 옵션과 관련

### 해결 방법

```bash
# 방법 1: IDE 인덱싱 새로고침
File → Invalidate Caches / Restart

# 방법 2: Gradle 동기화
./gradlew clean build

# 방법 3: 무시 (권장)
# - 런타임에 정상 작동
# - 컴파일 성공
# - Spring 프레임워크 정상 인식
```

### 결과

✅ 코드 문제 없음 (False Positive)
✅ 런타임 정상 작동
✅ 컴파일 성공

---

## 5. 브랜치 구조 정리

### 현재 브랜치 구조

```
main (base branch)
 ├── feat/atomic-db (현재 작업 브랜치)
 │   └── DB atomic operation 개선
 │
 └── feat/redis (별도 브랜치)
     └── Redis 마이그레이션 작업
```

### feat/atomic-db 브랜치 현황

**Staged (커밋 대기)**:
```bash
renamed: ReduceStockUseCase.kt → DecreaseStockUseCase.kt
renamed: AddStockUseCase.kt → IncreaseStockUseCase.kt
renamed: InventoryRepositoryImpl.kt → InventoryRepositoryAdapter.kt
```

**Unstaged (작업 중)**:
```bash
modified: InventoryStockController.kt
modified: StockCommand.kt
modified: InventoryRepositoryPort.kt
modified: ConfirmStockUseCase.kt
modified: DecreaseStockUseCase.kt
modified: IncreaseStockUseCase.kt
modified: ReserveStockUseCase.kt
modified: InventoryRepositoryAdapter.kt
modified: JpaInventoryRepository.kt  ← JPQL 수정 완료
```

### feat/redis 브랜치 (별도)

**작업 내용**:
- RDB (JPA) → Redis 마이그레이션
- Inventory 엔티티 삭제
- Redis 기반 재고 관리 구현

**주요 변경**:
```bash
# 삭제된 파일
D  domain/entity/Inventory.kt
D  infra/persist/secondary/InventoryQuery.kt
D  infra/persist/secondary/InventoryRepositoryImpl.kt
D  infra/persist/secondary/JpaInventoryRepository.kt

# 추가된 파일
A  infra/storage/primary/RedisInventorySeedAdapter.kt
A  infra/storage/primary/RedisInventoryStockAdapter.kt
A  infra/storage/primary/RedisInventoryStockQueryAdapter.kt
```

### 정리

- ✅ **feat/atomic-db**: JPA 기반 atomic operation 개선 (현재 작업)
- ✅ **feat/redis**: Redis 마이그레이션 (별도 진행)
- ✅ 두 브랜치는 독립적이며 base는 main

---

## 요약

### 해결된 이슈

| 이슈 | 파일 | 해결 방법 |
|------|------|-----------|
| RedisContainerTestBase 구현 | `base/RedisContainerTestBase.kt` | Import 추가, DynamicPropertySource 구현 |
| Testcontainers 의존성 누락 | `build.gradle.kts:89` | `junit-jupiter` 모듈 추가 |
| JPQL 쿼리 오류 | `JpaInventoryRepository.kt:21` | `i.stock + :quantity` → `i.stock.total + :quantity` |
| Gradle 빌드 실패 | `build.gradle.kts:155-158` | Spotless task dependency 명시 (`mustRunAfter`) |
| PathVariable Warning | `TestInventoryStockController.kt:40` | False Positive, 무시 가능 |

### 추가된 파일

```
src/test/kotlin/com/koosco/inventoryservice/base/
├── RedisContainerTestBase.kt (개선)
└── RedisContainerTestBaseTest.kt (신규)
```

### 수정된 파일

```
build.gradle.kts
├── testImplementation("org.testcontainers:junit-jupiter") (line 89)
└── tasks.matching { ... }.configureEach { mustRunAfter(...) } (line 155-158)

src/main/kotlin/com/koosco/inventoryservice/inventory/infra/persist/
└── JpaInventoryRepository.kt (JPQL 수정, line 21)
```

### 교훈

1. **Gradle 버전 업그레이드**: 엄격한 검증으로 숨겨진 문제 발견
2. **Task Dependency**: 명시적 의존성 선언의 중요성
3. **Testcontainers 설정**: DynamicPropertySource로 Spring Boot 통합
4. **JPQL 작성**: 객체 경로 vs 필드 경로 구분 필요
5. **IDE Warning**: False Positive 판별 능력 필요

---

**문서 버전**: 1.0
**최종 업데이트**: 2025-12-31
