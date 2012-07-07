package org.jbpm.designer.web.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;


import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.di.BPMNDiagram;
import org.eclipse.bpmn2.di.BPMNEdge;
import org.eclipse.bpmn2.di.BPMNPlane;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.di.BpmnDiFactory;
import org.eclipse.dd.dc.Bounds;
import org.eclipse.dd.dc.DcFactory;
import org.eclipse.dd.dc.Point;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.jbpm.designer.bpmn2.resource.JBPMBpmn2ResourceFactoryImpl;
import org.jbpm.designer.bpmn2.resource.JBPMBpmn2ResourceImpl;
import org.jbpm.designer.bpmn2.validation.BPMN2SyntaxChecker;
import org.jbpm.designer.web.batikprotocolhandler.GuvnorParsedURLProtocolHandler;
import org.jbpm.designer.web.profile.IDiagramProfile;
import org.jbpm.designer.web.profile.IDiagramProfileService;
import org.jbpm.designer.web.profile.impl.ExternalInfo;
import org.jbpm.designer.web.profile.impl.JbpmProfileImpl;
import org.jbpm.designer.web.profile.impl.ProfileServiceImpl;
import org.jbpm.designer.web.repository.IUUIDBasedRepository;
import org.jbpm.designer.web.repository.IUUIDBasedRepositoryService;
import org.jbpm.designer.web.repository.UUIDBasedEpnRepository;
import org.jbpm.designer.web.repository.impl.UUIDBasedFileRepository;
import org.jbpm.designer.web.repository.impl.UUIDBasedJbpmRepository;

import org.jbpm.designer.bpmn2.compliance.BPMN2ComplianceChecker;
import org.json.JSONObject;

/** 
 * 
 * Check compliance of a BPMN2 process.
 * 
 * @author Saiful Omar
 */


public class MigrateInstanceServlet extends HttpServlet {
	
	private static final String STANDARD_MODEL = "standard1";

    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
	
	
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
	    
		String json = req.getParameter("data");
        String profileName = req.getParameter("profile");
        String preprocessingData = req.getParameter("pp");
        String uuid = req.getParameter("uuid");
        String smName = req.getParameter("smName");        
        //String smName = "standard1"; 
        String action = req.getParameter("action");

        IDiagramProfile profile = ServletUtil.getProfile(req, profileName, getServletContext());
        
        if(action.equals("getProcessName")) {
        	List<String> allProcessesList;
			try {
				allProcessesList = ServletUtil.getAllProcessesInPackage("globalArea", profile);
			} catch (Throwable t) {
				allProcessesList = new ArrayList<String>();
			}
	        JSONObject jsonObject = new JSONObject();
			if(allProcessesList != null && allProcessesList.size() > 0) {
				for(String allProcesses : allProcessesList) {
					try {
						jsonObject.put(allProcesses, allProcesses);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			resp.setCharacterEncoding("UTF-8");
			resp.setContentType("application/json");
			resp.getWriter().write(jsonObject.toString());
        } else {
	        //Definitions def = profile.createMarshaller().getDefinitions(json, preprocessingData);
	        //List<RootElement> rootElements =  def.getRootElements();
	               
	        String sm = ServletUtil.getProcessSourceContent("globalArea", smName, profile);
			Definitions smdef = getDefinitions(sm);
			
			Definitions udmdef = profile.createMarshaller().getDefinitions(json, preprocessingData);
	
			BPMN2ComplianceChecker checker = new BPMN2ComplianceChecker(smdef, udmdef);
			checker.checkSyntax();
			resp.setCharacterEncoding("UTF-8");
	        resp.setContentType("application/json");
	        
	        if(checker.errorsFound()) {
				resp.getWriter().write(checker.getErrorsAsJson().toString());
			}
        }
}
	
	
	private Definitions getDefinitions(String xml) {
        try {
            ResourceSet resourceSet = new ResourceSetImpl();
            resourceSet
                    .getResourceFactoryRegistry()
                    .getExtensionToFactoryMap()
                    .put(Resource.Factory.Registry.DEFAULT_EXTENSION,
                            new JBPMBpmn2ResourceFactoryImpl());
            resourceSet.getPackageRegistry().put(
                    "http://www.omg.org/spec/BPMN/20100524/MODEL",
                    Bpmn2Package.eINSTANCE);
            XMLResource resource = (XMLResource) resourceSet.createResource(URI
                    .createURI("inputStream://dummyUriWithValidSuffix.xml"));
            resource.getDefaultLoadOptions().put(XMLResource.OPTION_ENCODING,
                    "UTF-8");
            resource.setEncoding("UTF-8");
            Map<String, Object> options = new HashMap<String, Object>();
            options.put(XMLResource.OPTION_ENCODING, "UTF-8");
            InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            resource.load(is, options);
            return ((DocumentRoot) resource.getContents().get(0))
                    .getDefinitions();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}

