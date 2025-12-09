from flask import Flask, jsonify, request
from flask_cors import CORS
from datetime import datetime

app = Flask(__name__)
CORS(app)

# -----------------------------------------
# Генерация парковочных мест 0..19
# id = index + 1 (как требуют задания)
# -----------------------------------------
parking_places = [
    {"status": "free"}
    for i in range(20)
]


# -----------------------------------------
# Вернуть список мест
# -----------------------------------------
@app.get("/places")
def get_places():
    return jsonify({
        "places": [
            {"id": i + 1, "status": place["status"]}
            for i, place in enumerate(parking_places)
        ]
    })


# -----------------------------------------
# Обновление статуса места
# -----------------------------------------
@app.post("/update")
def update_place():
    data = request.get_json()

    place_id = data.get("id")      # id от 1 до 20
    new_status = data.get("status")

    # Проверки
    if place_id is None or not (1 <= place_id <= 20):
        return jsonify({"success": False, "message": "Invalid id"}), 400
    if new_status not in ("free", "busy"):
        return jsonify({"success": False, "message": "Invalid status"}), 400

    index = place_id - 1           # переводим id → индекс

    old_status = parking_places[index]["status"]
    parking_places[index]["status"] = new_status

    log_action(f"Place {place_id}: {old_status} -> {new_status}")

    return jsonify({"success": True, "message": "Status updated"})


# -----------------------------------------
# Логирование в файл
# -----------------------------------------
def log_action(message: str):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open("server_log.txt", "a", encoding="utf-8") as f:
        f.write(f"[{timestamp}] {message}\n")


# -----------------------------------------
# Запуск сервера
# -----------------------------------------
@app.get("/stats")
def get_stats():
    free = 0
    busy = 0

    for place in parking_places:
        if place["status"] == "free":
            free += 1
        else:
            busy += 1

    total = len(parking_places)
    load = int(busy / total * 100)  # процент занятых мест

    return jsonify({
        "free": free,
        "busy": busy,
        "total": total,
        "load": f"{load}%"
    })

if __name__ == "__main__":
    print("🚗 SmartParkingServer running at http://localhost:8000")
    app.run(host="0.0.0.0", port=8000)
