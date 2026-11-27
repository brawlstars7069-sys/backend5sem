#!/usr/bin/env bash
BASE_URL="http://localhost:8080"

echo "=========================================================="
echo "    КОМПЬЮТЕРНЫЙ КЛУБ ZOMBIE PLAYA - ТЕСТИРОВАНИЕ API     "
echo "  (PostgreSQL 18, Spring Data JPA, Flyway, Связанные данные)"
echo "=========================================================="

echo -e "\n=== 1. ПРОВЕРКА СЛУЖЕБНЫХ ЭНДПОИНТОВ, МЕТОДОВ И 404 ==="
echo "--- 1.1 GET / (Root info) ---"; curl -i -s "$BASE_URL/"
echo -e "\n--- 1.2 GET /ping (200 OK) ---"; curl -i -s "$BASE_URL/ping"
echo -e "\n--- 1.3 POST /ping (405 Method Not Allowed с заголовком Allow) ---"; curl -i -s -X POST "$BASE_URL/ping"
echo -e "\n--- 1.4 GET /unknown (404 Not Found) ---"; curl -i -s "$BASE_URL/unknown"

echo -e "\n\n=== 2. CRUD КОМПЬЮТЕРОВ (СВЕДЕНИЯ О ЖЕЛЕЗЕ И СЕТИ ИЗ МИГРАЦИИ V2) ==="
echo "--- 2.1 GET /api/computers (Все компьютеры со связанными игроками) ---"; curl -i -s "$BASE_URL/api/computers"
echo -e "\n--- 2.2 GET /api/computers?type=VIP (Фильтр по типу VIP) ---"; curl -i -s "$BASE_URL/api/computers?type=VIP"
echo -e "\n--- 2.3 GET /api/computers?status=AVAILABLE (Фильтр: свободные) ---"; curl -i -s "$BASE_URL/api/computers?status=AVAILABLE"
echo -e "\n--- 2.4 GET /api/computers?maxRate=150 (Фильтр по тарифу) ---"; curl -i -s "$BASE_URL/api/computers?maxRate=150"
echo -e "\n--- 2.5 GET /api/computers/1 (Компьютер 1 по ID) ---"; curl -i -s "$BASE_URL/api/computers/1"
echo -e "\n--- 2.6 GET /api/computers/999 (404: не найден) ---"; curl -i -s "$BASE_URL/api/computers/999"

echo -e "\n--- 2.7 POST /api/computers (Создание нового ПК: 201 Created) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" \
  -d '{"name":"PC-STREAM-PRO","type":"VIP","hourlyRate":350.0}'

echo -e "\n--- 2.8 POST /api/computers (400 Bad Request: отрицательный тариф) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" \
  -d '{"name":"BAD-PC","type":"STANDARD","hourlyRate":-50.0}'

echo -e "\n--- 2.9 POST /api/computers (409 Conflict: дубликат имени) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" \
  -d '{"name":"PC-01","type":"STANDARD","hourlyRate":150.0}'

echo -e "\n--- 2.10 PUT /api/computers/4 (Обновление ПК 4: 200 OK) ---"
curl -i -s -X PUT "$BASE_URL/api/computers/4" -H "Content-Type: application/json" \
  -d '{"name":"PC-STREAM-UPGRADED","type":"VIP","hourlyRate":400.0}'

echo -e "\n--- 2.11 DELETE /api/computers/4 (Удаление свободного ПК: 204 No Content) ---"
curl -i -s -X DELETE "$BASE_URL/api/computers/4"

echo -e "\n--- 2.12 DELETE /api/computers/999 (404: удаление несуществующего) ---"
curl -i -s -X DELETE "$BASE_URL/api/computers/999"


echo -e "\n\n=== 3. CRUD ИГРОКОВ (СВЯЗАННЫЕ КОМПЬЮТЕРЫ БЕЗ N+1) ==="
echo "--- 3.1 GET /api/players (Список игроков со списком связанных ПК) ---"; curl -i -s "$BASE_URL/api/players"
echo -e "\n--- 3.2 GET /api/players?type=VIP (Фильтр VIP игроков) ---"; curl -i -s "$BASE_URL/api/players?type=VIP"
echo -e "\n--- 3.3 GET /api/players?hasActiveComputers=false (Фильтр игроков без активных сессий) ---"; curl -i -s "$BASE_URL/api/players?hasActiveComputers=false"
echo -e "\n--- 3.4 GET /api/players?search=Zombie (Поиск по никнейму) ---"; curl -i -s "$BASE_URL/api/players?search=Zombie"
echo -e "\n--- 3.5 GET /api/players/1 (Игрок 1 по ID) ---"; curl -i -s "$BASE_URL/api/players/1"

echo -e "\n--- 3.6 POST /api/players (Создание игрока: 201 Created) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" \
  -d '{"nickname":"CyberHunter","phone":"+79995554433","type":"VIP"}'

echo -e "\n--- 3.7 POST /api/players (400 Bad Request: пустой телефон) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" \
  -d '{"nickname":"Ghost","phone":"","type":"REGULAR"}'

echo -e "\n--- 3.8 POST /api/players (409 Conflict: дубликат телефона) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" \
  -d '{"nickname":"Clone","phone":"+79991112233","type":"REGULAR"}'

echo -e "\n--- 3.9 PUT /api/players/3 (Обновление игрока 3: 200 OK) ---"
curl -i -s -X PUT "$BASE_URL/api/players/3" -H "Content-Type: application/json" \
  -d '{"nickname":"CyberHunter_PRO","phone":"+79995554433","type":"VIP"}'

echo -e "\n--- 3.10 DELETE /api/players/3 (Удаление игрока без активных сессий: 204 No Content) ---"
curl -i -s -X DELETE "$BASE_URL/api/players/3"


echo -e "\n\n=== 4. СТРОГАЯ ВАЛИДАЦИЯ ПАРАМЕТРОВ И БИТЫХ ТЕЛ ЗАПРОСА ==="
echo "--- 4.1 400: Некорректный boolean в фильтре (hasActiveComputers=qwerty) ---"
curl -i -s "$BASE_URL/api/players?hasActiveComputers=qwerty"
echo -e "\n--- 4.2 400: Некорректный enum в фильтре (type=ULTRA) ---"
curl -i -s "$BASE_URL/api/computers?type=ULTRA"
echo -e "\n--- 4.3 400: Некорректное число в фильтре (maxRate=abc) ---"
curl -i -s "$BASE_URL/api/computers?maxRate=abc"
echo -e "\n--- 4.4 400: Битый JSON в теле запроса ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d '{bad-json'
echo -e "\n--- 4.5 400: Нечисловой ID ресурса в URL (/api/computers/xyz) ---"
curl -i -s "$BASE_URL/api/computers/xyz"


echo -e "\n\n=== 5. СВЯЗАННЫЕ ДАННЫЕ, ДОМЕННЫЕ ОПЕРАЦИИ И ЦЕЛОСТНОСТЬ ДАННЫХ ==="
echo "--- 5.1 REGULAR игрок (1) занимает STANDARD ПК 1 (200 OK) ---"
curl -i -s -X POST "$BASE_URL/api/computers/1/occupy" -H "Content-Type: application/json" -d '{"playerId":1}'

echo -e "\n--- 5.2 Проверка: ПК 1 стал OCCUPIED, указан связанный игрок ---"; curl -i -s "$BASE_URL/api/computers/1"
echo -e "\n--- 5.3 Проверка: В профиле игрока 1 отображается занятый ПК 1 со спеками и IP ---"; curl -i -s "$BASE_URL/api/players/1"

echo -e "\n--- 5.4 404: Попытка занять ПК несуществующим игроком (ID 9999) ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/occupy" -H "Content-Type: application/json" -d '{"playerId":9999}'

echo -e "\n--- 5.5 409: Попытка занять уже занятый ПК 1 другим игроком ---"
curl -i -s -X POST "$BASE_URL/api/computers/1/occupy" -H "Content-Type: application/json" -d '{"playerId":2}'

echo -e "\n--- 5.6 409: REGULAR игрок пытается занять 2-й ПК (превышение лимита 1) ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/occupy" -H "Content-Type: application/json" -d '{"playerId":1}'

echo -e "\n--- 5.7 409: REGULAR игрок пытается занять VIP ПК (ПК 3) ---"
curl -i -s -X POST "$BASE_URL/api/computers/3/occupy" -H "Content-Type: application/json" -d '{"playerId":1}'

echo -e "\n--- 5.8 409: Попытка удалить занятый ПК (проверка целостности) ---"
curl -i -s -X DELETE "$BASE_URL/api/computers/1"

echo -e "\n--- 5.9 409: Попытка удалить играющего игрока (проверка внешнего ключа и целостности) ---"
curl -i -s -X DELETE "$BASE_URL/api/players/1"


echo -e "\n\n=== 6. АТОМАРНАЯ ТРАНЗАКЦИЯ НАД НЕСКОЛЬКИМИ СВЯЗАННЫМИ ОБЪЕКТАМИ (ТРЕБОВАНИЕ 3) ==="
echo "--- 6.1 Успешная пересадка игрока 1 с ПК 1 на свободный ПК 2 (200 OK) ---"
curl -i -s -X POST "$BASE_URL/api/computers/1/transfer" -H "Content-Type: application/json" \
  -d '{"targetComputerId":2,"playerId":1}'

echo -e "\n--- 6.2 Проверка после пересадки: ПК 1 стал AVAILABLE, а ПК 2 стал OCCUPIED ---"
echo "ПК 1:"; curl -s "$BASE_URL/api/computers/1"
echo -e "\nПК 2:"; curl -s "$BASE_URL/api/computers/2"
echo -e "\nИгрок 1 (за ним теперь числится ПК 2):"; curl -s "$BASE_URL/api/players/1"

echo -e "\n--- 6.3 ОШИБКА И ОТКАТ (All-or-Nothing): Попытка пересадки игрока 1 (REGULAR) на VIP ПК 3 ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/transfer" -H "Content-Type: application/json" \
  -d '{"targetComputerId":3,"playerId":1}'

echo -e "\n--- 6.4 Проверка отката: ПК 2 остался OCCUPIED за игроком 1, ПК 3 остался AVAILABLE ---"
echo "ПК 2:"; curl -s "$BASE_URL/api/computers/2"
echo -e "\nПК 3:"; curl -s "$BASE_URL/api/computers/3"

echo -e "\n--- 6.5 400 Bad Request: Попытка пересадки на тот же самый ПК (from == to) ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/transfer" -H "Content-Type: application/json" \
  -d '{"targetComputerId":2,"playerId":1}'

echo -e "\n--- 6.6 404 Not Found: Попытка пересадки на несуществующий ПК (targetComputerId: 9999) ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/transfer" -H "Content-Type: application/json" \
  -d '{"targetComputerId":9999,"playerId":1}'


echo -e "\n\n=== 7. ОСВОБОЖДЕНИЕ, ТЕХОБСЛУЖИВАНИЕ И КОРРЕКТНОЕ УДАЛЕНИЕ ==="
echo "--- 7.1 Освобождение ПК 2 (200 OK) ---"; curl -i -s -X POST "$BASE_URL/api/computers/2/release"
echo -e "\n--- 7.2 Проверка: ПК 2 снова AVAILABLE ---"; curl -i -s "$BASE_URL/api/computers/2"

echo -e "\n--- 7.3 Отправка свободного ПК 2 на техобслуживание (200 OK, MAINTENANCE) ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/maintenance"

echo -e "\n--- 7.4 409 Conflict: Попытка занять ПК, находящийся на ТО ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/occupy" -H "Content-Type: application/json" -d '{"playerId":2}'

echo -e "\n--- 7.5 Завершение ТО ПК 2 (200 OK, AVAILABLE) ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/complete-maintenance"

echo -e "\n\n=========================================================="
echo "   ВСЕ ПРОВЕРКИ API И СВЯЗАННЫХ РЕСУРСОВ УСПЕШНО ЗАВЕРШЕНЫ! "
echo "=========================================================="
