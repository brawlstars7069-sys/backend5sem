#!/usr/bin/env bash
BASE_URL="http://localhost:8080"

echo "=== 1. PING, 405 И 404 ==="
echo "--- GET /ping (200) ---"; curl -i -s "$BASE_URL/ping"
echo -e "\n--- POST /ping (405 с Allow) ---"; curl -i -s -X POST "$BASE_URL/ping"
echo -e "\n--- GET /unknown (404) ---"; curl -i -s "$BASE_URL/unknown"

echo -e "\n\n=== 2. CRUD COMPUTERS ==="
echo "--- GET /api/computers ---"; curl -i -s "$BASE_URL/api/computers"
echo -e "\n--- GET /api/computers?type=VIP (Фильтр) ---"; curl -i -s "$BASE_URL/api/computers?type=VIP"
echo -e "\n--- GET /api/computers?status=AVAILABLE ---"; curl -i -s "$BASE_URL/api/computers?status=AVAILABLE"
echo -e "\n--- GET /api/computers?maxRate=150 ---"; curl -i -s "$BASE_URL/api/computers?maxRate=150"
echo -e "\n--- GET /api/computers/1 ---"; curl -i -s "$BASE_URL/api/computers/1"
echo -e "\n--- GET /api/computers/999 (404) ---"; curl -i -s "$BASE_URL/api/computers/999"
echo -e "\n--- POST /api/computers (201) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d '{"name":"PC-STREAM","type":"VIP","hourlyRate":350.0}'
echo -e "\n--- POST /api/computers (400: отрицательный тариф) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d '{"name":"BAD","type":"STANDARD","hourlyRate":-50.0}'
echo -e "\n--- POST /api/computers (409: дубликат имени) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d '{"name":"PC-01","type":"STANDARD","hourlyRate":150.0}'
echo -e "\n--- PUT /api/computers/4 (200) ---"
curl -i -s -X PUT "$BASE_URL/api/computers/4" -H "Content-Type: application/json" -d '{"name":"PC-STREAM-PRO","type":"VIP","hourlyRate":380.0}'
echo -e "\n--- DELETE /api/computers/4 (204) ---"
curl -i -s -X DELETE "$BASE_URL/api/computers/4"

echo -e "\n\n=== 3. CRUD PLAYERS ==="
echo "--- GET /api/players ---"; curl -i -s "$BASE_URL/api/players"
echo -e "\n--- GET /api/players?type=VIP ---"; curl -i -s "$BASE_URL/api/players?type=VIP"
echo -e "\n--- GET /api/players?hasActiveSession=false ---"; curl -i -s "$BASE_URL/api/players?hasActiveSession=false"
echo -e "\n--- GET /api/players?search=Zombie ---"; curl -i -s "$BASE_URL/api/players?search=Zombie"
echo -e "\n--- GET /api/players/1 ---"; curl -i -s "$BASE_URL/api/players/1"
echo -e "\n--- POST /api/players (201) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" -d '{"nickname":"CyberHunter","phone":"+79995554433","type":"VIP"}'
echo -e "\n--- POST /api/players (400: пустой телефон) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" -d '{"nickname":"Ghost","phone":"","type":"REGULAR"}'
echo -e "\n--- POST /api/players (409: дубликат телефона) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" -d '{"nickname":"Clone","phone":"+79991112233","type":"REGULAR"}'
echo -e "\n--- PUT /api/players/3 (200) ---"
curl -i -s -X PUT "$BASE_URL/api/players/3" -H "Content-Type: application/json" -d '{"nickname":"Hunter_PRO","phone":"+79995554433","type":"VIP"}'
echo -e "\n--- DELETE /api/players/3 (204) ---"
curl -i -s -X DELETE "$BASE_URL/api/players/3"

echo -e "\n\n=== 4. СЕССИИ, БИЗНЕС-ПРАВИЛА И ТЕХОБСЛУЖИВАНИЕ ==="
echo "--- POST /api/sessions (REGULAR берет STANDARD ПК на 3ч: 201) ---"
curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"playerId":1,"computerId":1,"hours":3}'
echo -e "\n--- Проверка ПК 1 (статус OCCUPIED) ---"; curl -i -s "$BASE_URL/api/computers/1"
echo -e "\n--- 409: Попытка занять уже занятый ПК 1 ---"
curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"playerId":2,"computerId":1,"hours":1}'
echo -e "\n--- 409: REGULAR превышает лимит ПК (макс. 1) ---"
curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"playerId":1,"computerId":2,"hours":1}'
echo -e "\n--- 409: REGULAR пытается занять VIP компьютер ---"
curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"playerId":1,"computerId":3,"hours":1}'
echo -e "\n--- 409: REGULAR превышает макс. время (макс. 4ч, запрошено 6) ---"
curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"playerId":1,"computerId":2,"hours":6}'
echo -e "\n--- 409: Удаление занятого ПК ---"; curl -i -s -X DELETE "$BASE_URL/api/computers/1"
echo -e "\n--- 409: Удаление играющего игрока ---"; curl -i -s -X DELETE "$BASE_URL/api/players/1"
echo -e "\n--- 409: ТО для занятого ПК ---"; curl -i -s -X POST "$BASE_URL/api/computers/1/maintenance"
echo -e "\n--- Завершение сессии ПК 1 (200, COMPLETED) ---"; curl -i -s -X POST "$BASE_URL/api/computers/1/stop"
echo -e "\n--- Проверка: ПК 1 снова AVAILABLE ---"; curl -i -s "$BASE_URL/api/computers/1"

echo -e "\n--- Отправка свободного ПК 2 на ТО (200, MAINTENANCE) ---"; curl -i -s -X POST "$BASE_URL/api/computers/2/maintenance"
echo -e "\n--- 409: Попытка занять ПК на ТО ---"
curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"playerId":2,"computerId":2,"hours":1}'
echo -e "\n--- Завершение ТО ПК 2 (200, AVAILABLE) ---"; curl -i -s -X POST "$BASE_URL/api/computers/2/complete-maintenance"

echo -e "\n--- VIP занимает VIP-ПК на 8ч со скидкой 20% (201) ---"
curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"playerId":2,"computerId":3,"hours":8}'
echo -e "\n--- VIP занимает 2-й ПК (ПК 2) для тиммейта (201, лимит VIP = 2) ---"
curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{"playerId":2,"computerId":2,"hours":4}'
echo -e "\n--- 409: Понижение VIP до REGULAR при 2 активных ПК ---"
curl -i -s -X PUT "$BASE_URL/api/players/2" -H "Content-Type: application/json" -d '{"nickname":"Pro","phone":"+79998887766","type":"REGULAR"}'

echo -e "\n--- GET /api/sessions?status=ACTIVE ---"; curl -i -s "$BASE_URL/api/sessions?status=ACTIVE"
echo -e "\n--- Завершение сессии через /api/sessions/2/stop ---"; curl -i -s -X POST "$BASE_URL/api/sessions/2/stop"
echo -e "\n--- Завершение сессии через /api/computers/2/stop ---"; curl -i -s -X POST "$BASE_URL/api/computers/2/stop"

echo -e "\n\n=== 5. НЕКОРРЕКТНЫЙ JSON И ТИПЫ ==="
echo "--- 400: Битый JSON ---"; curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d '{bad-json'
echo -e "\n--- 400: Пустое тело ---"; curl -i -s -X POST "$BASE_URL/api/sessions" -H "Content-Type: application/json" -d ''
echo -e "\n--- 400: Нечисловой ID /api/computers/abc ---"; curl -i -s "$BASE_URL/api/computers/abc"
echo -e "\n--- 400: Недопустимый enum /api/computers?type=CYBER ---"; curl -i -s "$BASE_URL/api/computers?type=CYBER"
echo -e "\n\nТестирование Zombie Playa завершено!"
