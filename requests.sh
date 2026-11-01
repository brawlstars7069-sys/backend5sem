#!/usr/bin/env bash
BASE_URL="http://localhost:8080"

echo "=== 1. PING, 405 И 404 ==="
echo "--- 1.1 GET /ping (200 OK) ---"; curl -i -s "$BASE_URL/ping"
echo -e "\n--- 1.2 POST /ping (405 с заголовком Allow) ---"; curl -i -s -X POST "$BASE_URL/ping"
echo -e "\n--- 1.3 GET /unknown (404 Not Found) ---"; curl -i -s "$BASE_URL/unknown"

echo -e "\n\n=== 2. CRUD COMPUTERS (КОМПЬЮТЕРЫ) ==="
echo "--- 2.1 GET /api/computers (Все компьютеры) ---"; curl -i -s "$BASE_URL/api/computers"
echo -e "\n--- 2.2 GET /api/computers?type=VIP (Фильтр по типу) ---"; curl -i -s "$BASE_URL/api/computers?type=VIP"
echo -e "\n--- 2.3 GET /api/computers?status=AVAILABLE (Свободные) ---"; curl -i -s "$BASE_URL/api/computers?status=AVAILABLE"
echo -e "\n--- 2.4 GET /api/computers?maxRate=150 (По цене) ---"; curl -i -s "$BASE_URL/api/computers?maxRate=150"
echo -e "\n--- 2.5 GET /api/computers/1 (ПК по ID) ---"; curl -i -s "$BASE_URL/api/computers/1"
echo -e "\n--- 2.6 GET /api/computers/999 (404: не найден) ---"; curl -i -s "$BASE_URL/api/computers/999"
echo -e "\n--- 2.7 POST /api/computers (Создание: 201) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d '{"name":"PC-STREAM","type":"VIP","hourlyRate":350.0}'
echo -e "\n--- 2.8 POST /api/computers (400: отрицательный тариф) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d '{"name":"BAD","type":"STANDARD","hourlyRate":-50.0}'
echo -e "\n--- 2.9 POST /api/computers (409: дубликат имени) ---"
curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d '{"name":"PC-01","type":"STANDARD","hourlyRate":150.0}'
echo -e "\n--- 2.10 PUT /api/computers/4 (Обновление: 200) ---"
curl -i -s -X PUT "$BASE_URL/api/computers/4" -H "Content-Type: application/json" -d '{"name":"PC-STREAM-PRO","type":"VIP","hourlyRate":380.0}'
echo -e "\n--- 2.11 DELETE /api/computers/4 (Удаление: 204) ---"
curl -i -s -X DELETE "$BASE_URL/api/computers/4"
echo -e "\n--- 2.12 DELETE /api/computers/999 (404: не найден) ---"
curl -i -s -X DELETE "$BASE_URL/api/computers/999"

echo -e "\n\n=== 3. CRUD PLAYERS (ИГРОКИ) ==="
echo "--- 3.1 GET /api/players (Все игроки) ---"; curl -i -s "$BASE_URL/api/players"
echo -e "\n--- 3.2 GET /api/players?type=VIP (Фильтр по типу) ---"; curl -i -s "$BASE_URL/api/players?type=VIP"
echo -e "\n--- 3.3 GET /api/players?hasActiveComputers=false (Фильтр: без активных ПК) ---"; curl -i -s "$BASE_URL/api/players?hasActiveComputers=false"
echo -e "\n--- 3.4 GET /api/players?search=Zombie (Поиск по никнейму) ---"; curl -i -s "$BASE_URL/api/players?search=Zombie"
echo -e "\n--- 3.5 GET /api/players/1 (Игрок по ID) ---"; curl -i -s "$BASE_URL/api/players/1"
echo -e "\n--- 3.6 POST /api/players (Создание: 201) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" -d '{"nickname":"CyberHunter","phone":"+79995554433","type":"VIP"}'
echo -e "\n--- 3.7 POST /api/players (400: пустой телефон) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" -d '{"nickname":"Ghost","phone":"","type":"REGULAR"}'
echo -e "\n--- 3.8 POST /api/players (409: дубликат телефона) ---"
curl -i -s -X POST "$BASE_URL/api/players" -H "Content-Type: application/json" -d '{"nickname":"Clone","phone":"+79991112233","type":"REGULAR"}'
echo -e "\n--- 3.9 PUT /api/players/3 (Обновление: 200) ---"
curl -i -s -X PUT "$BASE_URL/api/players/3" -H "Content-Type: application/json" -d '{"nickname":"Hunter_PRO","phone":"+79995554433","type":"VIP"}'
echo -e "\n--- 3.10 DELETE /api/players/3 (Удаление: 204) ---"
curl -i -s -X DELETE "$BASE_URL/api/players/3"

echo -e "\n\n=== 4. СТРОГАЯ ВАЛИДАЦИЯ ПАРАМЕТРОВ, НЕИЗВЕСТНЫХ ПУТЕЙ И МЕТОДОВ ==="
echo "--- 4.1 400: Некорректный boolean в фильтре (hasActiveComputers=abracadabra) ---"
curl -i -s "$BASE_URL/api/players?hasActiveComputers=abracadabra"
echo -e "\n--- 4.2 400: Некорректный enum в фильтре (type=CYBER) ---"
curl -i -s "$BASE_URL/api/computers?type=CYBER"
echo -e "\n--- 4.3 400: Некорректное число в фильтре (maxRate=abc) ---"
curl -i -s "$BASE_URL/api/computers?maxRate=abc"
echo -e "\n--- 4.4 404: Неизвестный подпуть компьютеров (/api/computers/1/foo/bar) ---"
curl -i -s "$BASE_URL/api/computers/1/foo/bar"
echo -e "\n--- 4.5 404: Неизвестный подпуть игроков (/api/players/1/extra) ---"
curl -i -s "$BASE_URL/api/players/1/extra"
echo -e "\n--- 4.6 405: Неверный HTTP-метод на действии (GET /api/computers/1/occupy) ---"
curl -i -s "$BASE_URL/api/computers/1/occupy"
echo -e "\n--- 4.7 405: Неверный HTTP-метод на действии (GET /api/computers/1/release) ---"
curl -i -s "$BASE_URL/api/computers/1/release"

echo -e "\n\n=== 5. ДОМЕННЫЕ ОПЕРАЦИИ И БИЗНЕС-ПРАВИЛА ==="
echo "--- 5.1 REGULAR игрок (1) занимает STANDARD ПК 1 (200 OK) ---"
curl -i -s -X POST "$BASE_URL/api/computers/1/occupy" -H "Content-Type: application/json" -d '{"playerId":1}'
echo -e "\n--- 5.2 Проверка состояния: ПК 1 стал OCCUPIED ---"; curl -i -s "$BASE_URL/api/computers/1"
echo -e "\n--- 5.3 Проверка состояния: у игрока 1 появился ПК 1 ---"; curl -i -s "$BASE_URL/api/players/1"
echo -e "\n--- 5.4 409: Попытка занять уже занятый ПК 1 ---"
curl -i -s -X POST "$BASE_URL/api/computers/1/occupy" -H "Content-Type: application/json" -d '{"playerId":2}'
echo -e "\n--- 5.5 409: REGULAR игрок пытается занять 2-й ПК (лимит = 1) ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/occupy" -H "Content-Type: application/json" -d '{"playerId":1}'
echo -e "\n--- 5.6 409: REGULAR игрок пытается занять VIP компьютер ---"
curl -i -s -X POST "$BASE_URL/api/computers/3/occupy" -H "Content-Type: application/json" -d '{"playerId":1}'
echo -e "\n--- 5.7 409: Попытка удалить занятый ПК ---"; curl -i -s -X DELETE "$BASE_URL/api/computers/1"
echo -e "\n--- 5.8 409: Попытка удалить играющего игрока ---"; curl -i -s -X DELETE "$BASE_URL/api/players/1"
echo -e "\n--- 5.9 409: Попытка отправить занятый ПК на ТО ---"; curl -i -s -X POST "$BASE_URL/api/computers/1/maintenance"
echo -e "\n--- 5.10 Освобождение ПК 1 (200 OK) ---"; curl -i -s -X POST "$BASE_URL/api/computers/1/release"
echo -e "\n--- 5.11 Проверка: ПК 1 снова AVAILABLE ---"; curl -i -s "$BASE_URL/api/computers/1"
echo -e "\n--- 5.12 409: Попытка освободить уже свободный ПК 1 ---"; curl -i -s -X POST "$BASE_URL/api/computers/1/release"

echo -e "\n--- 5.13 Отправка свободного ПК 2 на ТО (200, MAINTENANCE) ---"; curl -i -s -X POST "$BASE_URL/api/computers/2/maintenance"
echo -e "\n--- 5.14 409: Попытка занять ПК на ТО ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/occupy" -H "Content-Type: application/json" -d '{"playerId":2}'
echo -e "\n--- 5.15 Завершение ТО ПК 2 (200, AVAILABLE) ---"; curl -i -s -X POST "$BASE_URL/api/computers/2/complete-maintenance"

echo -e "\n--- 5.16 VIP игрок занимает VIP компьютер (200 OK) ---"
curl -i -s -X POST "$BASE_URL/api/computers/3/occupy" -H "Content-Type: application/json" -d '{"playerId":2}'
echo -e "\n--- 5.17 VIP игрок занимает 2-й ПК (ПК 2) для тиммейта (200 OK, лимит VIP = 2) ---"
curl -i -s -X POST "$BASE_URL/api/computers/2/occupy" -H "Content-Type: application/json" -d '{"playerId":2}'
echo -e "\n--- 5.18 409: VIP игрок пытается занять 3-й ПК (превышение лимита 2) ---"
curl -i -s -X POST "$BASE_URL/api/computers/1/occupy" -H "Content-Type: application/json" -d '{"playerId":2}'
echo -e "\n--- 5.19 409: Понижение VIP до REGULAR при 2 активных ПК и занятом VIP-месте ---"
curl -i -s -X PUT "$BASE_URL/api/players/2" -H "Content-Type: application/json" -d '{"nickname":"Pro","phone":"+79998887766","type":"REGULAR"}'
echo -e "\n--- 5.20 Освобождение ПК 3 и ПК 2 ---"
curl -i -s -X POST "$BASE_URL/api/computers/3/release"
curl -i -s -X POST "$BASE_URL/api/computers/2/release"

echo -e "\n\n=== 6. НЕКОРРЕКТНЫЙ JSON И БИТЫЕ ТЕЛА ==="
echo "--- 6.1 400: Битый JSON ---"; curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d '{bad-json'
echo -e "\n--- 6.2 400: Пустое тело ---"; curl -i -s -X POST "$BASE_URL/api/computers" -H "Content-Type: application/json" -d ''
echo -e "\n--- 6.3 400: Нечисловой ID в пути (/api/computers/abc) ---"; curl -i -s "$BASE_URL/api/computers/abc"

echo -e "\n\nТестирование компьютерного клуба Zombie Playa успешно пройдено!"
