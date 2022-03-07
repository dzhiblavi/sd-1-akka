package actor;

import akka.actor.*;
import proto.Search.*;
import scala.concurrent.duration.Duration;

public class SearchActor extends UntypedActor {
    private final SearchResponse.Builder responseBuilder = SearchResponse.newBuilder();
    private final static SupervisorStrategy strategy = OneForOneStrategy.stoppingStrategy();
    private int numChildren;
    private ActorRef senderRef;

    private void finish() {
        senderRef.tell(responseBuilder.build(), getSelf());
        getContext().stop(getSelf());
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public void onReceive(final Object message) {
        if (message instanceof SearchRequest) {
            this.senderRef = getSender();
            final SearchRequest requestProto = (SearchRequest) message;
            final String request = requestProto.getRequest();
            if (requestProto.getServicesList().isEmpty()) {
                finish();
            }
            for (final String service : requestProto.getServicesList()) {
                ++numChildren;
                final ActorRef childActor = getContext().actorOf(
                        Props.create(ServerSearchActor.class),
                        String.format("ServiceSearch:%s", service)
                );
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
            if (--numChildren == 0) {
                finish();
            }
        } else if (message instanceof ReceiveTimeout) {
            getContext().setReceiveTimeout(Duration.Undefined());
            finish();
        }
    }
}
