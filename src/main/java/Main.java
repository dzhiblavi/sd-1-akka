import actor.SearchActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import proto.Search.*;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(final String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: search <timeout(ms)> <n-top> <request> <service>+");
            System.exit(1);
        }

        try {
            final ActorSystem actorSystem = ActorSystem.create("Search");
            final ActorRef masterActor = actorSystem.actorOf(Props.create(SearchActor.class), "MasterActor");
            final int timeout_ms = Integer.parseInt(args[0]);
            final SearchRequest request = SearchRequest.newBuilder()
                    .setTimeoutMs(timeout_ms)
                    .setNTop(Integer.parseInt(args[1]))
                    .setRequest(args[2])
                    .addAllServices(Arrays.asList(args).subList(3, args.length))
                    .build();

            final FiniteDuration timeout = FiniteDuration.create(timeout_ms, TimeUnit.MILLISECONDS);
            final Object result = Await.result(Patterns.ask(masterActor, request, timeout.toMillis()), timeout);

            if (result instanceof SearchResponse) {
                System.out.format("Got the following results:\n%s", String.join("\n", ((SearchResponse) result).getUrlList()));
            } else {
                System.err.println("Invalid response from master actor!");
                System.exit(1);
            }
        } catch (final Exception exc) {
            System.err.format("Failed to do request: %s\n", exc);
        }
    }
}
