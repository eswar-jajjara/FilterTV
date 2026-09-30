package com.liskovsoft.smartyoutubetv2.common.filter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.Interceptor;
import okhttp3.Response;

public final class FilterInterceptor implements Interceptor {
    public interface Policy { boolean shouldBlock(String url, String method); }
    private final Policy policy;
    private final AtomicLong evaluated;
    private final AtomicLong blocked;
    public FilterInterceptor(Policy policy, AtomicLong evaluated, AtomicLong blocked) {
        this.policy = policy; this.evaluated = evaluated; this.blocked = blocked;
    }
    @Override public Response intercept(Chain chain) throws IOException {
        evaluated.incrementAndGet();
        if (policy.shouldBlock(chain.request().url().toString(), chain.request().method())) {
            blocked.incrementAndGet();
            throw new IOException("FilterTV blocked media request");
        }
        return chain.proceed(chain.request());
    }
}
