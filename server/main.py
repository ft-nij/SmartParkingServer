from flask import Flask, jsonify, request
from flask_cors import CORS
import json
from datetime import datetime

app = Flask(__name__)
CORS(app)

# Создаём список из 20 парковочных мест
parking_places = []

for i in range(1, 21):  # 20 мест
    parking_places.append({
        "id": i,
        "status": "free" if i % 2 == 0 else "busy"
    })

@app.route("/places", methods=["GET"])
def get_places():
    return jsonify({"places": parking_places})

@app.route("/update", methods=["POST"])
def update_place():
    data = request.get_json()
    place_id = data.get("id")
    new_status = data.get("status")

    for place in parking_places:
        if place["id"] == place_id:
            place["status"] = new_status
            log_action(f"Место {place_id} изменено на {new_status}")
            return jsonify({"success": True, "message": "Статус обновлён"})

    return jsonify({"success": False, "message": "Место не найдено"}), 404

def log_action(message):
    with open("server_log.txt", "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {message}\n")

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)
