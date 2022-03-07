package api;

import proto.Search.*;

public interface SearchService {
    SearchResponse doRequest(final ServerSearchRequest searchRequest);
}
