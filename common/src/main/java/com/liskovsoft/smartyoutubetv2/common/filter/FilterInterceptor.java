package com.liskovsoft.smartyoutubetv2.common.filter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import okhttp3.Interceptor;
import okhttp3.Response;

public final class FilterInterceptor implements Interceptor {
    public interface Policy { DomainFilter activeFilter(); }
    private final Policy policy;
    private final AtomicLong evaluated;
    private final AtomicLong blocked;
    public FilterInterceptor(Policy policy, AtomicLong evaluated, AtomicLong blocked) {
        this.policy = policy; this.evaluated = evaluated; this.blocked = blocked;
    }
    @Override public Response intercept(Chain chain) throws IOException {
        DomainFilter filter = policy.activeFilter();
        if (filter != null) {
            evaluated.incrementAndGet();
            String rule = filter.matchedRule(chain.request().url().host());
            if (rule != null) {
                blocked.incrementAndGet();
                throw new IOException("FilterTV blocked media request: " + rule);
            }
        }
        return chain.proceed(chain.request());
    }
}
