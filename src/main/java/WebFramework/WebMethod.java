package WebFramework;

@FunctionalInterface
public interface WebMethod {
    String execute(Request request, Response response) throws Exception;
}
