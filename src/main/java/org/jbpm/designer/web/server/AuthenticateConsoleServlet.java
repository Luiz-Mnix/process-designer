package org.jbpm.designer.web.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class AuthenticateConsoleServlet extends HttpServlet {
	
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
	
    
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

		String instanceid = req.getParameter("instanceid");  
        JSONObject json = null;
		
         String detailsURL = "http://localhost:8080/gwt-console-server/rs/process/definition/"+ instanceid + "/instances";
         
         try {
			String jsonResponse = getDataFromService(detailsURL, "GET");
            json = (JSONObject) JSONSerializer.toJSON( jsonResponse );
            
            
         } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");
		resp.getWriter().write(json.toString());
		
    } 
	
	public String getDataFromService(String urlpath, String method) 
				throws Exception {
			HttpClient httpclient = new HttpClient();
			
			HttpMethod theMethod = null;
			StringBuffer sb = new StringBuffer();
			
			if ("GET".equalsIgnoreCase(method)) {
			    theMethod = new GetMethod(urlpath);
			} else if ("POST".equalsIgnoreCase(method)) {
			    theMethod = new PostMethod(urlpath);
			}
			
			try {
			    httpclient.executeMethod(theMethod);
			} catch (IOException e) {
			    e.printStackTrace();
			} finally {
			    theMethod.releaseConnection();
			}

			
			PostMethod authMethod = new PostMethod(
			             "http://localhost:8080/gwt-console-server/rs/identity/secure/j_security_check");
			NameValuePair[] data = { new NameValuePair("j_username", "saiful"),
			            new NameValuePair("j_password", "saiful") };
			authMethod.setRequestBody(data);
			try {
			    httpclient.executeMethod(authMethod);
			} catch (IOException e) {
			    e.printStackTrace();
			} finally {
			    authMethod.releaseConnection();
			}
			
			try {
			    httpclient.executeMethod(theMethod);
			    sb.append(theMethod.getResponseBodyAsString());
			    System.out.println("JSon Result: => " + sb.toString());
			    return sb.toString();
			
			} catch (Exception e) {
			    throw e;
			} finally {
			    theMethod.releaseConnection();
			}
			
			}
	
}
	