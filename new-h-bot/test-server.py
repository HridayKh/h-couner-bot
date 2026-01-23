from http.server import HTTPServer, BaseHTTPRequestHandler
import sys


class UniversalHandler(BaseHTTPRequestHandler):
	protocol_version = "HTTP/1.0"

	def log_message(self, format, *args):
		# sys.stderr.write()
		pass

	def handle_request_logic(self):
		print(f"\n{'='*20} NEW REQUEST {'='*20}")
		print(f"{self.command} {self.path}")
		print(self.headers)

		content_length = int(self.headers.get('Content-Length', 0))
		if content_length > 0:
			body = self.rfile.read(content_length)
			print(body.decode('utf-8'))

		self.send_response(200)
		self.send_header("Content-type", "application/json")
		self.end_headers() 

		response = '{"status": "success", "message": "Constant Response", "data": 12}'
		self.wfile.write(response.encode("utf-8"))


# Map all standard HTTP methods to our logic function

	def do_GET(self):
		self.handle_request_logic()

	def do_POST(self):
		self.handle_request_logic()

	def do_PUT(self):
		self.handle_request_logic()

	def do_DELETE(self):
		self.handle_request_logic()

	def do_PATCH(self):
		self.handle_request_logic()


def run(port=6942):
	server_address = ('', port)
	httpd = HTTPServer(server_address, UniversalHandler)
	print(f"Server listening on http://localhost:{port}...")
	try:
		httpd.serve_forever()
	except KeyboardInterrupt:
		print("\nServer stopping...")
		httpd.server_close()


if __name__ == "__main__":
	run()
