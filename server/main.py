from http.server import BaseHTTPRequestHandler, HTTPServer
import json

class SmartParkingHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/":
            response = {"status": "ok", "message": "SmartParking server running"}
            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(response).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

def run(server_class=HTTPServer, handler_class=SmartParkingHandler):
    server_address = ('', 8000)
    httpd = server_class(server_address, handler_class)
    print("🚗 SmartParkingServer started on http://localhost:8000")
    httpd.serve_forever()

if __name__ == '__main__':
    run()
