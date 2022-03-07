package actor;

import akka.actor.*;
import proto.Search.*;
import scala.concurrent.duration.Duration;

import java.util.Set;

public class SearchActor extends UntypedActor {
    private final SearchResponse.Builder responseBuilder = SearchResponse.newBuilder();
    private final static SupervisorStrategy strategy = OneForOneStrategy.stoppingStrategy();
    private final Set<String> result;
    private int numChildren;

    private void finish() {
        final SearchResponse finalResponse = responseBuilder.build();
        System.err.println("Master's size: " + finalResponse.getUrlList().size());
        this.result.addAll(finalResponse.getUrlList());
        getContext().getChildren().forEach(child -> getContext().stop(child));
//        getContext().getChildren().forEach(child -> child.tell(Kill.getInstance(), ActorRef.noSender()));
        getContext().stop(getSelf());
//        getContext().system().terminate();
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public SearchActor(final Set<String> result) {
        this.result = result;
    }

    @Override
    public void onReceive(final Object message) {
        if (message instanceof SearchRequest) {
            final SearchRequest requestProto = (SearchRequest) message;
            final String request = requestProto.getRequest();
            if (requestProto.getServicesList().isEmpty()) {
                System.err.println("Stopping because no services specified.");
                finish();
            }
            for (final String service : requestProto.getServicesList()) {
                ++numChildren;
                final ActorRef childActor = getContext().actorOf(
                        Props.create(ServerSearchActor.class),
                        String.format("ServiceSearch:%s", service)
                );
                System.err.format("Create child %s\n", childActor);
                childActor.tell(
                        ServerSearchRequest.newBuilder()
                                .setService(service)
                                .setNTop(requestProto.getNTop())
                                .setRequest(request)
                                .build(),
                        self()
                );
            }
            getContext().setReceiveTimeout(Duration.create(String.format("%d ms", requestProto.getTimeoutMs())));
        } else if (message instanceof SearchResponse) {
            final SearchResponse responseProto = (SearchResponse) message;
            responseBuilder.addAllUrl(responseProto.getUrlList());
            System.err.format("Child completed: %s\n", sender());
            System.err.println(responseProto.getUrlList());
            if (--numChildren == 0) {
                System.err.println("Stopping because completed.");
                finish();
            }
        } else if (message instanceof ReceiveTimeout) {
            System.err.println("Stopping due to timeout.");
            getContext().setReceiveTimeout(Duration.Undefined());
            finish();
        }
    }
}
