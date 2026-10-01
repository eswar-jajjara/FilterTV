package com.liskovsoft.smartyoutubetv2.tv;

import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.exoplayer2.ext.cronet.CronetDataSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.RequestUrlGate;
import com.liskovsoft.sharedutils.cronet.CronetManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Exercises the real Android transports against an isolated loopback server. */
@RunWith(AndroidJUnit4.class)
public class RequestGateTest {
    @Test public void packagedBraveEngineMatchesRulesThroughJni() throws Exception {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("The APK must contain the native Brave matcher",
                com.liskovsoft.smartyoutubetv2.common.filter.BraveFilterEngine.available());
        com.liskovsoft.smartyoutubetv2.common.filter.FilterNetwork.load(context);
        long deadline = android.os.SystemClock.elapsedRealtime() + 15000;
        while (!com.liskovsoft.smartyoutubetv2.common.filter.FilterNetwork.ready()
                && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(50);
        assertTrue("Initial rule compilation must finish off the UI thread",
                com.liskovsoft.smartyoutubetv2.common.filter.FilterNetwork.ready());
        assertTrue(com.liskovsoft.smartyoutubetv2.common.filter.FilterNetwork.testUrl("https://ads.example.test/video.mp4"));
        assertFalse(com.liskovsoft.smartyoutubetv2.common.filter.FilterNetwork.testUrl("https://media.example.test/video.mp4"));
    }

    @Test public void javaTransportChecksInitialAndRedirectUrls() throws Exception {
        verify(gate -> new DefaultHttpDataSourceFactory("FilterTV-test", 3000, 3000, false)
                .setRequestUrlGate(gate).createDataSource());
    }

    @Test public void cronetTransportChecksInitialAndRedirectUrls() throws Exception {
        ExecutorService callbacks = Executors.newSingleThreadExecutor();
        try {
            org.chromium.net.CronetEngine engine = CronetManager.getEngine(
                    InstrumentationRegistry.getInstrumentation().getTargetContext());
            assertNotNull("The packaged native Cronet engine must load", engine);
            verify(gate -> {
                CronetDataSource source = new CronetDataSource(engine, callbacks);
                source.setRequestUrlGate(gate);
                return source;
            });
        } finally { callbacks.shutdownNow(); }
    }

    private interface Factory { DataSource create(RequestUrlGate gate); }

    private void verify(Factory factory) throws Exception {
        try (Loopback server = new Loopback()) {
            List<String> checked = Collections.synchronizedList(new ArrayList<>());
            RequestUrlGate gate = (url, method) -> {
                String path = Uri.parse(url).getPath();
                checked.add(method + " " + path);
                if ("/blocked".equals(path)) throw new IOException("Test rule blocked request");
            };
            assertBlocked(factory.create(gate), server.url("/blocked"));
            assertEquals(Collections.singletonList("GET /blocked"), checked);
            assertTrue("Initial block must not contact server", server.requests.isEmpty());
            checked.clear();
            assertBlocked(factory.create(gate), server.url("/redirect-block"));
            assertEquals(Arrays.asList("GET /redirect-block", "GET /blocked"), checked);
            assertEquals(Collections.singletonList("/redirect-block"), server.requests);
            checked.clear();
            assertEquals("media", read(factory.create(gate), server.url("/redirect-ok")));
            assertEquals(Arrays.asList("GET /redirect-ok", "GET /allowed"), checked);
            assertEquals(Arrays.asList("/redirect-block", "/redirect-ok", "/allowed"), server.requests);
            assertEquals("media", read(factory.create(null), server.url("/allowed")));
        }
    }

    private static void assertBlocked(DataSource source, String url) throws Exception {
        try { read(source, url); fail("Blocked request was allowed"); }
        catch (IOException expected) {
            Throwable cause = expected;
            while (cause.getCause() != null) cause = cause.getCause();
            assertEquals("Test rule blocked request", cause.getMessage());
        }
    }

    private static String read(DataSource source, String url) throws Exception {
        try {
            source.open(new DataSpec(Uri.parse(url)));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[64];
            int count;
            while ((count = source.read(buffer, 0, buffer.length)) != -1) bytes.write(buffer, 0, count);
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        } finally { source.close(); }
    }

    private static final class Loopback implements AutoCloseable {
        final List<String> requests = Collections.synchronizedList(new ArrayList<>());
        final ServerSocket socket = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        Loopback() throws IOException {
            worker.execute(() -> {
                while (!socket.isClosed()) {
                    try (Socket client = socket.accept()) {
                        client.setSoTimeout(3000);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
                        String request = reader.readLine();
                        if (request == null) continue;
                        String path = request.split(" ")[1];
                        requests.add(path);
                        String header;
                        while ((header = reader.readLine()) != null && !header.isEmpty()) { }
                        String response = path.startsWith("/redirect-")
                                ? "HTTP/1.1 302 Found\r\nLocation: " + url(path.equals("/redirect-block") ? "/blocked" : "/allowed") + "\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                : "HTTP/1.1 200 OK\r\nContent-Type: video/mp4\r\nContent-Length: 5\r\nConnection: close\r\n\r\nmedia";
                        client.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
                        client.getOutputStream().flush();
                    } catch (IOException error) {
                        if (!socket.isClosed()) throw new RuntimeException(error);
                    }
                }
            });
        }
        String url(String path) { return "http://127.0.0.1:" + socket.getLocalPort() + path; }
        @Override public void close() throws IOException { socket.close(); worker.shutdownNow(); }
    }
}
