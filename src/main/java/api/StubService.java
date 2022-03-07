package api;

import proto.Search.*;

import java.util.stream.IntStream;

public class StubService implements SearchService {
    public static int latencyMs = 0;
    private final String service;

    private static void latency() {
        if (latencyMs > 0) {
            try {
                Thread.sleep(latencyMs);
            } catch (final InterruptedException exc) {
                System.err.println("Sleep interrupted.");
                // pass
            }
        }
    }

    public StubService(final String service) {
        this.service = service;
    }

    @Override
    public SearchResponse doRequest(final ServerSearchRequest searchRequest) throws RuntimeException {
        latency();
        final SearchResponse.Builder responseBuilder = SearchResponse.newBuilder();
        IntStream.range(0, searchRequest.getNTop())
                .forEach(i -> responseBuilder.addUrl(String.format("%s: %s#%d", this.service, searchRequest.getRequest(), i)));
        return responseBuilder.build();
    }
}
