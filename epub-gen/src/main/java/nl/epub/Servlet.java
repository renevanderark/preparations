package nl.epub;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.*;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class Servlet extends AbstractHandler
{
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException
    {
        baseRequest.setHandled(true);
        if(request.getMethod().equalsIgnoreCase("GET")) {
            try {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setCharacterEncoding("UTF-8");
                response.setHeader("access-control-allow-origin",  "*");
                response.setHeader("content-type", "application/json");
                DidlConverter didlConverter = new DidlConverter(request.getParameter("urn"));
                if (!didlConverter.getFile().exists()) {
                    didlConverter.generateEpub();
                }
                PrintWriter out = response.getWriter();
                out.println("{\"filename\": \"" + didlConverter.getFile().getName() + "\" }");
                out.flush();
                out.close();
            } catch (Exception e) {
                throw new ServletException(e);
            }
        } else {

        }
    }

    public static void main(String[] args) throws Exception
    {
        Server server = new Server(8080);
        server.setHandler(new Servlet());

        server.start();
        server.join();
    }
}