package WebFramework.Examples;

import WebFramework.HttpServer;

import static WebFramework.HttpServer.get;


import java.io.IOException;
import java.net.URISyntaxException;



public class MathServices {
    public static void main(String[] args) throws IOException, URISyntaxException {
        get("/pi", ()->"Pi="+Math.PI);
        HttpServer.main(args);
    }
}
