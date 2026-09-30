import com.liskovsoft.smartyoutubetv2.common.filter.*;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class FilterTests {
    private static int checks;
    static void check(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        DomainFilter f = new DomainFilter("! comment\n||ads.example.test^\n@@||safe.ads.example.test^");
        check(f.matchedRule("ads.example.test") != null, "exact domain");
        check(f.matchedRule("sub.ads.example.test") != null, "subdomain");
        check(f.matchedRule("ADS.EXAMPLE.TEST.") != null, "case and trailing dot");
        check(f.matchedRule("badads.example.test") == null, "domain boundary");
        check(f.matchedRule("ads.example.test.evil.test") == null, "suffix spoof");
        check(f.matchedRule("safe.ads.example.test") == null, "exception");
        check(f.matchedRule("a.safe.ads.example.test") == null, "exception subdomain");
        for (String invalid : new String[]{"banner", "||ads.test^$media", "||ads.test/path^", "||a..test^", "||*^"}) {
            boolean rejected=false; try {new DomainFilter(invalid);} catch(IllegalArgumentException e){rejected=true;}
            check(rejected, "reject unsupported rule " + invalid);
        }
        AtomicLong evaluated = new AtomicLong(), blocked = new AtomicLong();
        FilterInterceptor gate = new FilterInterceptor((url, method) -> f.matchedRule(HttpUrl.parse(url).host()) != null, evaluated, blocked);
        AtomicLong dnsCalls = new AtomicLong();
        OkHttpClient client = new OkHttpClient.Builder().dns(host -> {
            dnsCalls.incrementAndGet(); return Collections.singletonList(InetAddress.getByName("127.0.0.1"));
        }).addInterceptor(gate).addNetworkInterceptor(gate).build();
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            String root = "http://media.example.test:" + server.getPort();
            try { client.newCall(new Request.Builder().url("http://ads.example.test:" + server.getPort()).build()).execute(); throw new AssertionError("initial block"); }
            catch(IOException expected) { check(expected.getMessage().contains("FilterTV"), "block reason"); }
            check(dnsCalls.get()==0, "initial block before DNS");
            check(server.getRequestCount()==0, "initial block before network");
            server.enqueue(new MockResponse().setBody("video"));
            try(Response response=client.newCall(new Request.Builder().url(root+"/video").build()).execute()) {
                check("video".equals(response.body().string()), "allowed body");
            }
            server.takeRequest(1, TimeUnit.SECONDS);
            server.enqueue(new MockResponse().setResponseCode(302).addHeader("Location", "http://ads.example.test:"+server.getPort()+"/ad"));
            try {client.newCall(new Request.Builder().url(root+"/redirect").build()).execute();throw new AssertionError("redirect block");}
            catch(IOException expected){check(expected.getMessage().contains("FilterTV"), "redirect reason");}
            check(server.getRequestCount()==2, "blocked destination received no HTTP request");
            check(blocked.get()==2, "block counters");
        }
        System.out.println("PASS: " + checks + " assertions, including real HTTP and redirect interception");
    }
}
