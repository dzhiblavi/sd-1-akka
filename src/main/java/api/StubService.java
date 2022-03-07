package api;

import proto.Search.*;

import java.util.Random;
import java.util.stream.IntStream;

public class StubService implements SearchService {
    public static boolean exception = false;
    public static int latencyMs = 0;

    private static final Random random = new Random(239);
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

    private static void exceptions() throws RuntimeException {
        if (exception && (random.nextInt(2) == 3)) {
            System.err.println("exception = " + exception);
            System.err.println("StubService is throwing an exception!");
            throw new RuntimeException("StubServer generated exception");
        }
    }

    public StubService(final String service) {
        this.service = service;
    }

    @Override
    public SearchResponse doRequest(final ServerSearchRequest searchRequest) throws RuntimeException {
        exceptions();
        latency();
        final SearchResponse.Builder responseBuilder = SearchResponse.newBuilder();
        IntStream.range(0, searchRequest.getNTop())
                .forEach(i -> responseBuilder.addUrl(String.format("%s: %s#%d", this.service, searchRequest.getRequest(), i)));
        return responseBuilder.build();
    }
}
