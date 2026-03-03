package WebFramework.Examples;

import WebFramework.HttpServer;

import static WebFramework.HttpServer.get;
import static WebFramework.HttpServer.setRoutePrefix;
import static WebFramework.HttpServer.staticfiles;

public class MathServices {
    public static void main(String[] args) throws Exception {
        staticfiles("/webroot");
        setRoutePrefix("/App");
        get("/hello", (req, resp) -> "Hello " + req.getValues("name"));
        get("/pi", (req, resp) -> String.valueOf(Math.PI));
        HttpServer.main(args);
    }
}
