import actor.SearchActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import proto.Search.*;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(final String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: search <timeout(ms)> <n-top> <request> <service>+");
            System.exit(1);
        }

        try {
            final ActorSystem actorSystem = ActorSystem.create("Search");
            final Set<String> result = new HashSet<>();
            final ActorRef masterActor = actorSystem.actorOf(Props.create(SearchActor.class, result), "MasterActor");
            masterActor.tell(
                    SearchRequest.newBuilder()
                            .setTimeoutMs(Integer.parseInt(args[0]))
                            .setNTop(Integer.parseInt(args[1]))
                            .setRequest(args[2])
                            .addAllServices(Arrays.asList(args).subList(3, args.length))
                            .build(),
                    ActorRef.noSender()
            );
            Await.result(actorSystem.whenTerminated(), Duration.Inf());
            System.out.format("Got the following results:\n%s", String.join("\n", result));
        } catch (final Exception exc) {
            System.err.format("Failed to do request: %s\n", exc);
        }
    }
}
