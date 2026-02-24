package WebFramework;

import java.net.MalformedURLException;
import java.net.URL;

public class URLParser {
    public static void main(String[] args) throws MalformedURLException {
        URL myurl=new URL("https://www.bing.com/ck/a?!&&p=19365060ec66c70fa00e8209da8269d1643ec36beaa6081675772baf6f727ac5JmltdHM9MTc3MTg5MTIwMA&ptn=3&ver=2&hsh=4&fclid=3778fe5e-692e-696a-3f80-e954685c6860&psq=eci+moodle&u=a1aHR0cHM6Ly9jYW1wdXN2aXJ0dWFsLmVzY3VlbGFpbmcuZWR1LmNvL21vb2RsZS8");
        System.out.println("Protocol = " + myurl.getProtocol());
        System.out.println("Authority = " + myurl.getAuthority());
        System.out.println("Host = " + myurl.getHost());
        System.out.println("Port = " + myurl.getPort());
        System.out.println("Path = " + myurl.getPath());
        System.out.println("Query = " + myurl.getQuery());
        System.out.println("File = " + myurl.getFile());
        System.out.println("Ref = " + myurl.getRef());



    }
}
