import actor.SearchActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import api.StubService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import proto.Search.*;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

public class TestSearch {
    private static final List<String> exampleServiceNames = List.of(
            "google", "bing", "yandex", "kgeorgiy"
    );

    private static Set<String> getStubResults(final List<String> serviceNames, final String request, final int nTop) {
        final Set<String> urls = new HashSet<>();
        serviceNames.forEach(service ->
                IntStream.range(0, nTop)
                        .forEach(i -> urls.add(String.format("%s: %s#%d", service, request, i)))
        );
        return urls;
    }

    private static Set<String> doTest(final SearchRequest request) {
        final ActorSystem actorSystem = ActorSystem.create("Search");
        try {
            final int timeout = request.getTimeoutMs();
            final ActorRef masterActor = actorSystem.actorOf(Props.create(SearchActor.class), "MasterActor");

            FiniteDuration duration = FiniteDuration.create(2L * timeout, TimeUnit.MILLISECONDS);
            final Object result = Await.result(Patterns.ask(masterActor, request, duration.toMillis()), duration);

            return new HashSet<>(((SearchResponse) result).getUrlList());
        } catch (final TimeoutException tle) {
            System.err.println("Timeout exceeded: " + tle);
            Assertions.fail();
        } catch (final Exception e) {
            System.err.println("Unhandled exception from test: " + e);
            Assertions.fail();
        } finally {
            actorSystem.terminate();
        }
        return null;
    }

    private static void testNoLatency(final String request, final int nTop, final List<String> serviceNames) {
        StubService.latencyMs = 0;
        final int timeoutMs = 100;
        final SearchRequest requestProto = SearchRequest.newBuilder()
                .setRequest(request)
                .setNTop(nTop)
                .setTimeoutMs(timeoutMs)
                .addAllServices(serviceNames)
                .build();
        Assertions.assertEquals(getStubResults(serviceNames, request, nTop), doTest(requestProto));
    }

    @Test
    public void testSimpleNoLatency() {
        testNoLatency("aba", 10, exampleServiceNames);
    }

    @Test
    public void testNoServices() {
        testNoLatency("aba", 10, List.of());
    }

    @Test
    public void testEmptyRequest() {
        testNoLatency("", 10, exampleServiceNames);
    }

    @Test
    public void testNTopZero() {
        testNoLatency("aba", 0, exampleServiceNames);
    }

    @Test
    public void testBadLatency() {
        StubService.latencyMs = 2000;
        final int timeoutMs = 100;
        final SearchRequest requestProto = SearchRequest.newBuilder()
                .setRequest("aba")
                .setNTop(10)
                .setTimeoutMs(timeoutMs)
                .addAllServices(exampleServiceNames)
                .build();
        doTest(requestProto);
    }
}
