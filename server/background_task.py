import threading
import time
from sensors import simulate_sensor_change
from app import log_action

def background_sensor_loop(interval=60):
    """Фоновый поток: имитирует срабатывание датчиков каждые interval секунд"""
    while True:
        spot, status = simulate_sensor_change()
        log_action(f"[AUTO] Место {spot} изменено → {status}")
        time.sleep(interval)

def start_background_thread():
    t = threading.Thread(target=background_sensor_loop, daemon=True)
    t.start()
