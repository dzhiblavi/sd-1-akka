import actor.SearchActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.FSM;
import akka.actor.Props;
import akka.pattern.Patterns;
import api.StubService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import proto.Search.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
        try {
            final int timeout = request.getTimeoutMs();
            final ActorSystem actorSystem = ActorSystem.create("Search");
            final Set<String> result = new HashSet<>();
            final ActorRef masterActor = actorSystem.actorOf(Props.create(SearchActor.class, result), "MasterActor");

            final long startTime = System.currentTimeMillis();
            masterActor.tell(request, ActorRef.noSender());

            FiniteDuration duration = FiniteDuration.create(1, TimeUnit.SECONDS);
//            Await.ready(Patterns.gracefulStop(masterActor, duration), duration);
            while (!masterActor.isTerminated()) {
                System.err.println("wait");
                Thread.sleep(50);
            }
//            Await.ready(actorSystem.whenTerminated(), Duration.Inf());

            final long duration_ms = System.currentTimeMillis() - startTime;
            System.err.println("test took " + duration_ms + "ms");
            Assertions.assertTrue(duration_ms < 2L * timeout);
            return result;
        } catch (final Exception e) {
            // TODO: handle exceptions
            return null;
        }
    }

    private static void testNoLatency(final String request, final int nTop, final List<String> serviceNames) {
        StubService.latencyMs = 0;
        StubService.exception = false;
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
        StubService.exception = false;
        final int timeoutMs = 100;
        final SearchRequest requestProto = SearchRequest.newBuilder()
                .setRequest("aba")
                .setNTop(10)
                .setTimeoutMs(timeoutMs)
                .addAllServices(exampleServiceNames)
                .build();
        doTest(requestProto);
    }

    @Test
    public void testExceptions() {
        StubService.latencyMs = 0;
        StubService.exception = true;
        final int timeoutMs = 1000;
        final SearchRequest requestProto = SearchRequest.newBuilder()
                .setRequest("aba")
                .setNTop(10)
                .setTimeoutMs(timeoutMs)
                .addAllServices(exampleServiceNames)
                .build();
        doTest(requestProto);
    }
}
