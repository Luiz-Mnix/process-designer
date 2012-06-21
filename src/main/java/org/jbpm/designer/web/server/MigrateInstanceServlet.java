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
import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;
import org.json.JSONObject;

/** 
 * 
 * Migrate Instance of a BPMN2 process.
 * 
 * @author Saiful Omar
 */


public class MigrateInstanceServlet extends HttpServlet {
	
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
	
	
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
	    
		String sessionId = req.getParameter("sessionId");
        String newProcessId = req.getParameter("newProcessId");
        Map<String, Long> mapping = new HashMap<String, Long>();
        
        //WorkflowProcessInstanceUpgrader.upgradeProcessInstance("rsa.application","12", "rsa.application2", mapping);
        
	}
	
	
}

