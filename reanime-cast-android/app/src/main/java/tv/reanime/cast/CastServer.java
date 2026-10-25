package tv.reanime.cast;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Phone-hosted pairing endpoint on local Wi-Fi, not a video proxy. */
final class CastServer {
    static final int PORT = 9867;
    private final String code;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private volatile JSONObject session = new JSONObject();
    private volatile boolean running;
    private ServerSocket listener;

    CastServer(String code) { this.code = code; }

    void start() throws IOException {
        listener = new ServerSocket(PORT, 12, InetAddress.getByName("0.0.0.0"));
        running = true;
        workers.execute(() -> {
            while (running) {
                try {
                    Socket incoming = listener.accept();
                    workers.execute(() -> handle(incoming));
                } catch (IOException e) {
                    if (!running) break;
                }
            }
        });
    }
    void publish(String url, String format, String title, String subtitle,
                 String referer, String origin, String userAgent) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", Long.toString(System.currentTimeMillis()));
            obj.put("url", url);
            obj.put("format", format);
            obj.put("title", title);
            obj.put("subtitle", subtitle == null ? "" : subtitle);
            JSONObject browser = new JSONObject();
            browser.put("referer", referer == null ? "" : referer);
            browser.put("origin", origin == null ? "" : origin);
            browser.put("userAgent", userAgent == null ? "" : userAgent);
            obj.put("headers", browser);
        } catch (Exception ignored) {}
        session = obj;
    }
    void stop() {
        running = false;
        try { if (listener != null) listener.close(); } catch (IOException ignored) {}
        workers.shutdownNow();
    }
    private void handle(Socket client) {
        try (Socket sock = client) {
            sock.setSoTimeout(3000);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.US_ASCII));
            String line = in.readLine();
            if (line == null || line.length() > 2048) return;
            String[] parts = line.split(" ");
            if (parts.length < 2 || !"GET".equals(parts[0])) return;
            URI path = new URI(parts[1]);
            int bytes = 0;
            while ((line = in.readLine()) != null && !line.isEmpty() && bytes < 8192) bytes += line.length();
            String supplied = "";
            String query = path.getRawQuery();
            if (query != null) {
                for (String value : query.split("&")) {
                    if (value.startsWith("code=")) supplied = URLDecoder.decode(value.substring(5), "UTF-8");
                }
            }
            if (!"/session".equals(path.getPath()) || !code.equals(supplied))
                respond(sock, 403, "{\"error\":\"not paired\"}");
            else respond(sock, 200, session.toString());
        } catch (Exception ignored) {}
    }
    private void respond(Socket sock, int status, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + (status == 200 ? " OK" : " Forbidden") + "\r\n" +
            "Content-Type: application/json; charset=utf-8\r\n" +
            "Content-Length: " + data.length + "\r\n" +
            "Cache-Control: no-store\r\n" +
            "Connection: close\r\n\r\n";
        OutputStream out = sock.getOutputStream();
        out.write(headers.getBytes(StandardCharsets.US_ASCII));
        out.write(data);
        out.flush();
    }
}
