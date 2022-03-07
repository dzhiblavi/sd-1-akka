package actor;

import akka.actor.UntypedActor;
import api.SearchService;
import api.ServiceFactory;
import proto.Search.*;

public class ServerSearchActor extends UntypedActor {
    @Override
    public void onReceive(final Object message) throws Throwable {
        if (message instanceof ServerSearchRequest) {
            final ServerSearchRequest protoRequest = (ServerSearchRequest) message;
            final SearchService searchService = ServiceFactory.createService(protoRequest.getService());
            final SearchResponse protoResponse = searchService.doRequest(protoRequest);
            sender().tell(protoResponse, self());
            context().stop(self());
        }
    }
}
