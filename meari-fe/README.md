# 🏷️ 네이밍 규칙 (Naming Convention)

본 프로젝트는 **일관성 있는 코드 스타일 유지**와 **협업 효율 향상**을 위해 아래와 같은 네이밍 규칙을 따릅니다.

---

### 1. 📁 폴더 & 파일 네이밍

* **kebab-case** 사용
* 모두 소문자
* 의미가 명확한 단어 사용

```text
features/
shared/
parking-history/
```

❌ `BadExample/`, `bad_example/`
✅ `bad-example/`

---

### 2. 🧩 React 컴포넌트

#### 컴포넌트 파일

* **PascalCase**
* 하나의 파일에 하나의 컴포넌트

```text
OrderList.jsx
ReservationCard.jsx
ParkingHistoryItem.jsx
```

#### 컴포넌트 내부 함수

* **camelCase**

```jsx
const handleSubmit = () => {}
const renderItem = () => {}
```

---

### 3. 📄 Page 컴포넌트 (`pages/`)

* URL 단위 컴포넌트
* **PascalCase**

```text
Main.jsx
Login.jsx
Reservation.jsx
OrderDetail.jsx
```

---

### 4. 🎣 Custom Hook

* 반드시 `use`로 시작
* **camelCase**

```text
useAuth.js
useOrderList.js
useParkingHistory.js
```

```js
export const useOrderList = () => {}
```

---

### 5. 🧠 상태 관리 (Zustand / Redux)

#### Store 파일

* feature 기준 분리
* `.store.js` 접미사 사용

```text
auth.store.js
location.store.js
cart.store.js
```

#### Store 내부 함수

* camelCase
* **동사 + 명사** 형태 권장

```js
setUser()
clearUser()
updateLocation()
addToCart()
```

---

### 6. 🌐 API 관련 네이밍

#### API 파일

* feature 기준
* `.api.js` 접미사 사용

```text
auth.api.js
order.api.js
parking.api.js
```

#### API 함수

* HTTP 동작 + 리소스 명

```js
getOrders()
createOrder()
updateReservation()
deleteCartItem()
```

---

### 7. 🛠️ 유틸 함수 (`utils/`)

* **camelCase**
* 동작 중심 네이밍

```text
formatDate.js
calculateTotalPrice.js
parseLocation.js
```

---

### 8. 🎨 스타일 파일

* **kebab-case**
* 컴포넌트와 1:1 매칭 권장

```text
login-form.module.scss
header.module.css
```

---

### 9. 🔤 변수 & 함수 공통 규칙

* **camelCase**
* Boolean 값은 접두사 사용

```js
isLoggedIn
hasPermission
canSubmit
```

---

### ✅ 네이밍 규칙 요약

| 구분    | 규칙                      |
| ----- | ----------------------- |
| 폴더    | kebab-case              |
| 컴포넌트  | PascalCase              |
| 페이지   | PascalCase              |
| 커스텀 훅 | use + camelCase         |
| store | camelCase + `.store.js` |
| api   | camelCase + `.api.js`   |
| utils | camelCase               |

---

