package xyz.wagyourtail.devserver;

import com.sun.net.httpserver.*;
import xyz.wagyourtail.devserver.applier.InstrumentationChangeProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeListener {
    private final InstrumentationChangeProvider provider = new InstrumentationChangeProvider();
    private final String apiKey = System.getProperty("DevServerAPIKey", "changeme!");

    public ChangeListener() {
        start();
    }

    public void start() {
        new Thread(this::run, "DevServer ChangeListener").start();
    }

    public void run() {
        // start web server
        try {
            int port = Integer.parseInt(System.getProperty("DevServerPort", "25401"));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            System.out.println("[DevServer] ChangeListener started on port " + port);
            server.createContext("/", new ChangeListenerHandler(provider));
            server.setExecutor(null);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class ChangeListenerHandler implements HttpHandler {
        private final InstrumentationChangeProvider provider;

        public ChangeListenerHandler(InstrumentationChangeProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> params = new HashMap<>();
            ChangeListener.parseQuery(exchange.getRequestURI().getQuery(), params);
            String modid = (String) params.get("modid");
            String apiKey = (String) params.get("apiKey");

            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
                return;
            }

            if (modid == null || modid.isEmpty()) {
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
                return;
            }

            if (!apiKey.equals(ChangeListener.this.apiKey)) {
                exchange.sendResponseHeaders(403, 0);
                exchange.close();
                return;
            }

            // get file
            Headers requestHeaders = exchange.getRequestHeaders();
            List<String> contentType = requestHeaders.get("Content-Type");
            if (contentType == null || contentType.size() != 1 || !contentType.get(0).equals("application/java-archive")) {
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
                return;
            }

            System.out.println("Received change for " + modid);

            // create temp file
            Path tempFile = Files.createTempFile("devserver-" + modid, ".jar");
            try (InputStream stream = exchange.getRequestBody()) {
                Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            tempFile.toFile().deleteOnExit();

            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream responseBody = exchange.getResponseBody();
            responseBody.write("OK".getBytes());
            responseBody.close();

            System.out.println("Applying change for " + modid);
            provider.applyChanges(modid, tempFile);
        }
    }

    public static void parseQuery(String query, Map<String,
            Object> parameters) throws UnsupportedEncodingException {

        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] param = pair.split("=");
                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = URLDecoder.decode(param[0],
                            System.getProperty("file.encoding"));
                }

                if (param.length > 1) {
                    value = URLDecoder.decode(param[1],
                            System.getProperty("file.encoding"));
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values = (List<String>) obj;
                        values.add(value);

                    } else if (obj instanceof String) {
                        List<String> values = new ArrayList<>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        int readBytes = 0;
        byte[] bytes = new byte[8192];
        // read into bytes
        int read;
        while ((read = in.read(bytes, readBytes, bytes.length - readBytes)) != -1) {
            readBytes += read;
            if (readBytes == bytes.length) {
                byte[] old = bytes;
                bytes = new byte[readBytes << 1];
                System.arraycopy(old, 0, bytes, 0, readBytes);
            }
        }
        if (readBytes == bytes.length) return bytes;
        byte[] trimmed = new byte[readBytes];
        System.arraycopy(bytes, 0, trimmed, 0, readBytes);
        return trimmed;
    }

}
