package actor;

import akka.actor.UntypedActor;
import api.SearchService;
import api.ServiceFactory;
import proto.Search.*;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ServerSearchActor extends UntypedActor {
//    private final ExecutorService executor;
//
//    public ServerSearchActor(final ExecutorService executor) {
//        this.executor = executor;
//    }

    @Override
    public void onReceive(final Object message) throws Throwable {
        if (message instanceof ServerSearchRequest) {
            final ServerSearchRequest protoRequest = (ServerSearchRequest) message;
            final SearchService searchService = ServiceFactory.createService(protoRequest.getService());
//            Future<SearchResponse> protoResponse = executor.submit(() -> searchService.doRequest(protoRequest));
            final SearchResponse protoResponse = searchService.doRequest(protoRequest);
            sender().tell(protoResponse, self());
            context().stop(self());
        }
    }
}
