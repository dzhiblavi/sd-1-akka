package api;

public class ServiceFactory {
    public static SearchService createService(final String service) {
        return new StubService(service);
    }
}
