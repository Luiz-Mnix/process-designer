package org.jbpm.designer.bpmn2.compliance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.Artifact;
import org.eclipse.bpmn2.Assignment;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.BusinessRuleTask;
import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.ConditionalEventDefinition;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataObject;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.ErrorEventDefinition;
import org.eclipse.bpmn2.EscalationEventDefinition;
import org.eclipse.bpmn2.EventDefinition;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.FlowElementsContainer;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.FormalExpression;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.MessageEventDefinition;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.ScriptTask;
import org.eclipse.bpmn2.SendTask;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.SignalEventDefinition;
import org.eclipse.bpmn2.StartEvent;
import org.eclipse.bpmn2.SubProcess;
import org.eclipse.bpmn2.ThrowEvent;
import org.eclipse.bpmn2.TimerEventDefinition;
import org.eclipse.bpmn2.UserTask;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.jbpm.designer.bpmn2.validation.SyntaxCheckerUtils;
import org.jbpm.designer.taskforms.TaskFormInfo;
import org.jbpm.designer.web.profile.IDiagramProfile;
import org.jbpm.designer.web.profile.impl.ExternalInfo;
import org.jbpm.designer.web.server.ServletUtil;
import org.json.JSONObject;
import sun.misc.BASE64Encoder;
import org.jbpm.designer.bpmn2.compliance.NodeDataInfo;

public class BPMN2ComplianceChecker {
	public static final String EXT_BPMN = "bpmn";
    public static final String EXT_BPMN2 = "bpmn2";
    
    private Definitions smdef;
    private Definitions udmdef;
    
	protected Map<String, List<String>> errors = new HashMap<String, List<String>>();
	protected Map<Object, List<Object>> tmpSmList = new HashMap<Object, List<Object>>();
	protected Map<Object, List<Object>> tmpUmList = new HashMap<Object, List<Object>>();
	protected Map<Object, List<Object>> finalSmList = new HashMap<Object, List<Object>>();
	protected Map<Object, List<Object>> finalUmList = new HashMap<Object, List<Object>>();
	
    protected Map<Object, NodeDataInfo> smDataInfo = new HashMap<Object, NodeDataInfo>();
    protected Map<Object, NodeDataInfo> udDataInfo = new HashMap<Object, NodeDataInfo>();
	
	protected List<Object> tmp = new ArrayList<Object>();

	private String json;
	private String preprocessingData;
	private IDiagramProfile profile;
	private String defaultResourceId = "";
	private String uuid;
	    
	private static final Logger _logger = Logger.getLogger(BPMN2ComplianceChecker.class);
	
	public BPMN2ComplianceChecker(Definitions smdef, Definitions udmdef) {
		this.smdef = smdef;
		this.udmdef = udmdef;
	}

	public void checkSyntax() {
		List<String> smList = new ArrayList<String>();
		List<String> udmList = new ArrayList<String>();
		List<RootElement> udmrootElements =  udmdef.getRootElements();
		List<RootElement> smrootElements =  smdef.getRootElements();
		
		
		for(RootElement root : udmrootElements) {
			if(root instanceof Process) {
        		Process process = (Process) root;
        		if(process.getFlowElements() != null && process.getFlowElements().size() > 0) {
        			defaultResourceId = process.getFlowElements().get(0).getId();
        		}
        		
        		int count = 0;

            	List<FlowElement> flowElements =  process.getFlowElements();
            	for(FlowElement fe : flowElements) {
            		
            		if(fe instanceof FlowNode) {
            			if(!(fe instanceof StartEvent) && !(fe instanceof EndEvent) && !(fe instanceof Gateway)) {
            				udmList.add(fe.getName());	
           				}
       				}
           		}            	
			}
		}
		
		for(RootElement root : smrootElements) {
			if(root instanceof Process) {
        		Process process = (Process) root;
        	
           		List<FlowElement> flowElements =  process.getFlowElements();
           		for(FlowElement fe : flowElements) {
           			if(fe instanceof FlowNode) {
           				if(!(fe instanceof StartEvent) && !(fe instanceof EndEvent) && !(fe instanceof Gateway)) {
           						smList.add(fe.getName());
           				}
       				}
           		}        	
			}
		}
			
		   
		   Iterator< String > iter = smList.iterator();
		   while(iter.hasNext()){
			   Object element = iter.next();
			   if(!(udmList.contains(element))){
				   addError(defaultResourceId, "Missing Node: "+element);
			   }
		   }
		   
		   
		   //SM
		   for(RootElement root : smrootElements) {
				if(root instanceof Process) {
	        		Process process = (Process) root;
	        	
	           		List<FlowElement> flowElements =  process.getFlowElements();
	           		for(FlowElement fe : flowElements) {
	           			if(fe instanceof SequenceFlow) {
	        				SequenceFlow sf = (SequenceFlow) fe;
	        				if(!(sf.getTargetRef() instanceof EndEvent)){
	        					addSequenceFlow(sf.getTargetRef(),sf.getSourceRef());
	        				}
		           		}
	           		}
	           		
	           		
				}
		   }
		   
		   //UD
		   for(RootElement root : udmrootElements) {
				if(root instanceof Process) {
	        		Process process = (Process) root;
	        	
	           		List<FlowElement> flowElements =  process.getFlowElements();
	           		for(FlowElement fe : flowElements) {
	           			if(fe instanceof SequenceFlow) {
	        				SequenceFlow sf = (SequenceFlow) fe;
	        				if(!(sf.getTargetRef() instanceof EndEvent)){
	        					addSequenceFlowUdm(sf.getTargetRef(),sf.getSourceRef());
	        				}
		           		}
	           		}
				}
		   }

		   // Standard Model
   			Set set = tmpSmList.entrySet(); 
   			Iterator iter1 = set.iterator();
		   	while(iter1.hasNext()){
		   		tmp.clear();
		   		Map.Entry me = (Map.Entry)iter1.next(); 
		   		FlowNode sf = (FlowNode) me.getKey();
		   		if(!(sf instanceof Gateway)) {
			   		List current = new ArrayList();
			   		current = tmpSmList.get(me.getKey());
			   		List<Object> _obj = new ArrayList<Object>();
			   		
			   		for(Object temp: current ) {
		   				FlowNode ssf = (FlowNode) temp;
		   				if(!(ssf instanceof Gateway)) {
		   					_obj.add(temp);  
		   				}
		   				if(ssf instanceof Gateway) {
		   					findTarget(temp);	   					
		   					for(Object _current:tmp){
		   						_obj.add(_current); 
		   						FlowNode _val = (FlowNode) _current;
		   					}
		   				}
			   		}
			   		finalSmList.put(sf,_obj);
		   		}
		   	} 
		   	
		   	// User Define
		   	Set set3 = tmpUmList.entrySet(); 
   			Iterator iter3 = set3.iterator();
		   	while(iter3.hasNext()){
		   		tmp.clear();
		   		Map.Entry me = (Map.Entry)iter3.next(); 
		   		FlowNode sf = (FlowNode) me.getKey();
		   		List<Object> _obj = new ArrayList<Object>();
		   		if(!(sf instanceof Gateway) ) {
			   			findTargetUdm(sf);
			   			for(Object _current:tmp){
	   						_obj.add(_current); 
	   					}
			   			finalUmList.put(sf,_obj);
		   		}
		   	} 

			for (Map.Entry<Object, List<Object>> entry : tmpSmList.entrySet()) {
				FlowNode _key = (FlowNode) entry.getKey();
		   		List<Object> current = new ArrayList<Object>();
		   		current = entry.getValue();
		   		for(Object _value:current ) {
		   			FlowNode sf = (FlowNode) _value;
		   		}
			}
						
			for (Map.Entry<Object, List<Object>> entry : finalSmList.entrySet()) {
				FlowNode _key = (FlowNode) entry.getKey();
		   		List<Object> current = new ArrayList<Object>();
		   		current = entry.getValue();
		   		for(Object _value:current ) {
		   			FlowNode sf = (FlowNode) _value;
		   		}
			}
			 			
			
			for (Map.Entry<Object, List<Object>> entrySm : finalSmList.entrySet()) {
				boolean match = false;
				FlowNode _keySm = (FlowNode) entrySm.getKey();

				for (Map.Entry<Object, List<Object>> entryUd : finalUmList.entrySet()) {
					boolean foundError = true;

					FlowNode _keyUd = (FlowNode) entryUd.getKey();
					
					if(_keySm.getName().equals(_keyUd.getName())){
						match = true; 
				   		List<Object> currentSm = new ArrayList<Object>();
				   		List<Object> currentUd = new ArrayList<Object>();
				   		currentSm = entrySm.getValue();
				   		currentUd = entryUd.getValue();				   		
				   		
				   		for(Object _valueSm:currentSm ) {
				   			FlowNode fnSm = (FlowNode) _valueSm;
					   			for(Object _valueUd:currentUd) {				   				
						   			FlowNode fnUd = (FlowNode) _valueUd;
					   				if((fnSm.getName()).equals(fnUd.getName()) || (fnSm instanceof StartEvent)){

					   					foundError = false;
					   				} 
					   			}
				   			if(foundError){
				   				FlowNode udKey = (FlowNode) entryUd.getKey();
						   		addError(udKey.getId(), "Missing Pre-condition node: "+fnSm.getName());
				   			}
				   			foundError = false;
				   		}
					}
				}
			}
	          		
	    // collect sm usertask & input / output data      		
  		for(RootElement root : smrootElements) {
			if(root instanceof Process) {
        		Process process = (Process) root;
        		
        		smCheckData(process);
		
			}
  		}
	    
  		// collect ud usertask & input / output data
  		for(RootElement root : udmrootElements) {
			if(root instanceof Process) {
        		Process process = (Process) root;
        		
        		udCheckData(process);
		
			}
  		}
  		
  		// comparison check sm & ud usertask
  		
		for (Map.Entry<Object, NodeDataInfo> _smDataInfo : smDataInfo.entrySet()) {
			FlowNode smFn = (FlowNode) _smDataInfo.getKey();

			if(!(udDataInfo.isEmpty())){
				for (Map.Entry<Object, NodeDataInfo> _udDataInfo : udDataInfo.entrySet()) {
					FlowNode udFn = (FlowNode) _udDataInfo.getKey();
					if (smFn.getName().equals(udFn.getName())) {
						NodeDataInfo udCurrent = _udDataInfo.getValue();;
						NodeDataInfo smCurrent = _smDataInfo.getValue();
						for(Object _nd : smCurrent.getDataIn()){
							if(!(udCurrent.getDataIn().contains(_nd))){
								addError(udFn.getId(),"Missing Data Input:" + _nd);
							}
						}
						
						for(Object _nd : smCurrent.getDataOut()){
							if(!(udCurrent.getDataOut().contains(_nd))){
								addError(udFn.getId(),"Missing Data Output:" + _nd);
							}
						}
		
					}
				} 
			} else {
				for (Map.Entry<Object, List<Object>> entry : tmpUmList.entrySet()) {
					FlowNode _key = (FlowNode) entry.getKey();
					if (smFn.getName().equals(_key.getName())) {
						NodeDataInfo smCurrent = _smDataInfo.getValue();
						for(Object _nd : smCurrent.getDataIn()){
							addError(_key.getId(),"Missing Data Input:" + _nd);
						}
						for(Object _nd : smCurrent.getDataOut()){
							addError(_key.getId(),"Missing Data Output:" + _nd);
						}						
					}
				}
			
			}
		}
			
			
	}
	

	private void findTarget(Object _obj){
   	    
		Set set = tmpSmList.entrySet(); 
		Iterator iter1 = set.iterator();
		while(iter1.hasNext()){
			Map.Entry me = (Map.Entry)iter1.next(); 
			FlowNode sf = (FlowNode) me.getKey();
				if (_obj.equals(sf)) {
			   		List current = new ArrayList();
			   		current = tmpSmList.get(me.getKey());
			   		for(Object temp: current ) {
		   				FlowNode ssf = (FlowNode) temp;
		   				if(!(ssf instanceof Gateway)){
		   					if(!tmp.contains(ssf)) {
		   						tmp.add(temp);
		   					}
			   			} else {
			   			if((ssf instanceof Gateway) && ((FlowNode) me.getKey() instanceof Gateway)){ 
			   				_obj = ssf;
			   				findTarget(ssf);
			   			}
			   		}
			   	}
	   	       }
	   	   }
	}
		
	private void findTargetUdm(Object _obj){
   	    
		Set set = tmpUmList.entrySet(); 
		Iterator iter1 = set.iterator();
		while(iter1.hasNext()){
			Map.Entry me = (Map.Entry)iter1.next(); 
			FlowNode sf = (FlowNode) me.getKey();
				if (_obj.equals(sf)) {
			   		List current = new ArrayList();
			   		current = tmpUmList.get(me.getKey());
			   		for(Object temp: current ) {
		   				FlowNode ssf = (FlowNode) temp;
		   				if(!(ssf instanceof Gateway)){
		   					if(!tmp.contains(ssf)) {
		   						tmp.add(temp);
		   					}
		   				}
			   				_obj = ssf;
			   				findTargetUdm(ssf);
		   			}
	   	       }
	   	   }
	}
	
	private void addSequenceFlow(Object resourceId, Object obj) {		
		if(tmpSmList.containsKey(resourceId) && tmpSmList.get(resourceId) != null) {
			tmpSmList.get(resourceId).add(obj);
		} else {
			List<Object> _obj = new ArrayList<Object>();
			_obj.add(obj);    				
			tmpSmList.put(resourceId, _obj);
		}
	}

	private void addSequenceFlowUdm(Object resourceId, Object obj) {		
		if(tmpUmList.containsKey(resourceId) && tmpUmList.get(resourceId) != null) {
			tmpUmList.get(resourceId).add(obj);
		} else {
			List<Object> _obj = new ArrayList<Object>();
			_obj.add(obj);    				
			tmpUmList.put(resourceId, _obj);
		}
	}

	
	
	private void smCheckData(FlowElementsContainer container) {
					  
		for(FlowElement fe : container.getFlowElements()) {
					
			if(fe instanceof Activity) {
				Activity ut = (Activity) fe;
				FlowNode fn = (FlowNode) fe;
				List<DataInput> dataInputs = ut.getIoSpecification().getDataInputs();
                List<DataOutput> dataOutputs = ut.getIoSpecification().getDataOutputs();
                List<DataInputAssociation> dataInputAssociations = ut.getDataInputAssociations();
                List<DataOutputAssociation> dataOutputAssociations = ut.getDataOutputAssociations();
                
                List<String> dataIn = new ArrayList<String>();
                List<String> dataOut = new ArrayList<String>();
                
                for(DataInput din : dataInputs) {
                	if(!(din.getName().equals("TaskName"))){
                		dataIn.add(din.getName());
                	}
                }
                for(DataOutput din : dataOutputs) {
            		dataOut.add(din.getName());
                }
                
                if(!(dataInputs.isEmpty() || !(dataOutputs.isEmpty()))){ 
	                NodeDataInfo nodeDataInfo = new NodeDataInfo(dataIn, dataOut);
	        		smDataInfo.put(fn, nodeDataInfo); 
                }
            }
		}
	}
	
	private void udCheckData(FlowElementsContainer container) {
		
		for(FlowElement fe : container.getFlowElements()) {
					
			if(fe instanceof Activity) {
				Activity ut = (Activity) fe;
				FlowNode fn = (FlowNode) fe;
				List<DataInput> dataInputs = ut.getIoSpecification().getDataInputs();
                List<DataOutput> dataOutputs = ut.getIoSpecification().getDataOutputs();
                List<DataInputAssociation> dataInputAssociations = ut.getDataInputAssociations();
                List<DataOutputAssociation> dataOutputAssociations = ut.getDataOutputAssociations();
                
                List<String> dataIn = new ArrayList<String>();
                List<String> dataOut = new ArrayList<String>();
                
                for(DataInput din : dataInputs) {
                	if(!(din.getName().equals("TaskName"))){
                		dataIn.add(din.getName());
                	}
                }
                for(DataOutput din : dataOutputs) {
            		dataOut.add(din.getName());
                }
                
                if(!(dataInputs.isEmpty() || !(dataOutputs.isEmpty()))){ 
	                NodeDataInfo nodeDataInfo = new NodeDataInfo(dataIn, dataOut);
	        		udDataInfo.put(fn, nodeDataInfo);
                }
			}
		}
	}

	public Map<String, List<String>> getErrors() {
		return errors;
	}

	public JSONObject getErrorsAsJson() {
		JSONObject jsonObject = new JSONObject();
		for (Entry<String,List<String>> error: this.getErrors().entrySet()) {
			try {
				jsonObject.put(error.getKey(), error.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return jsonObject;
	}

	public boolean errorsFound() {
		return errors.size() > 0;
	}

	public void clearErrors() {
		errors.clear();
	}
	
	private void addError(BaseElement element, String error) {
		addError(element.getId(), error);
	}
	
	private void addError(String resourceId, String error) {
		if(errors.containsKey(resourceId) && errors.get(resourceId) != null) {
			errors.get(resourceId).add(error);
		} else {
			List<String> value = new ArrayList<String>();
			value.add(error);
			errors.put(resourceId, value);
		}
	}
	
	private static boolean isEmpty(final CharSequence str) {
		if ( str == null || str.length() == 0 ) {
			return true;
	    }
	    for ( int i = 0, length = str.length(); i < length; i++ ) {
	    	if ( str.charAt( i ) != ' ' ) {
	    		return false;
	        }
	    }
	    return true;
	}
	
	private String[] findPackageAndAssetInfo(String uuid,
            IDiagramProfile profile) {
        List<String> packages = new ArrayList<String>();
        String packagesURL = ExternalInfo.getExternalProtocol(profile)
                + "://"
                + ExternalInfo.getExternalHost(profile)
                + "/"
                + profile.getExternalLoadURLSubdomain().substring(0,
                        profile.getExternalLoadURLSubdomain().indexOf("/"))
                + "/rest/packages/";
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory
                    .createXMLStreamReader(getInputStreamForURL(packagesURL,
                            "GET", profile));
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamReader.START_ELEMENT) {
                    if ("title".equals(reader.getLocalName())) {
                        packages.add(reader.getElementText());
                    }
                }
            }
        } catch (Exception e) {
            // we dont want to barf..just log that error happened
            _logger.error(e.getMessage());
        }

        boolean gotPackage = false;
        String[] pkgassetinfo = new String[2];
        for (String nextPackage : packages) {
            String packageAssetURL = ExternalInfo.getExternalProtocol(profile)
                    + "://"
                    + ExternalInfo.getExternalHost(profile)
                    + "/"
                    + profile.getExternalLoadURLSubdomain().substring(0,
                            profile.getExternalLoadURLSubdomain().indexOf("/"))
                    + "/rest/packages/" + nextPackage + "/assets/";
            try {
                XMLInputFactory factory = XMLInputFactory.newInstance();
                XMLStreamReader reader = factory
                        .createXMLStreamReader(getInputStreamForURL(
                                packageAssetURL, "GET", profile));
                String title = "";
                while (reader.hasNext()) {
                    int next = reader.next();
                    if (next == XMLStreamReader.START_ELEMENT) {
                        if ("title".equals(reader.getLocalName())) {
                            title = reader.getElementText();
                        }
                        if ("uuid".equals(reader.getLocalName())) {
                            String eleText = reader.getElementText();
                            if (uuid.equals(eleText)) {
                                pkgassetinfo[0] = nextPackage;
                                pkgassetinfo[1] = title;
                                gotPackage = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // we dont want to barf..just log that error happened
                _logger.error(e.getMessage());
            }
            if (gotPackage) {
                // noo need to loop through rest of packages
                break;
            }
        }
        return pkgassetinfo;
    }
	
	private InputStream getInputStreamForURL(String urlLocation,
            String requestMethod, IDiagramProfile profile) throws Exception {
        URL url = new URL(urlLocation);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(requestMethod);
        connection
                .setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.2.16) Gecko/20110319 Firefox/3.6.16");
        connection
                .setRequestProperty("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "en-us,en;q=0.5");
        connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
        connection.setRequestProperty("charset", "UTF-8");
        connection.setReadTimeout(5 * 1000);

        applyAuth(profile, connection);

        connection.connect();

        BufferedReader sreader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), "UTF-8"));
        StringBuilder stringBuilder = new StringBuilder();

        String line = null;
        while ((line = sreader.readLine()) != null) {
            stringBuilder.append(line + "\n");
        }

        return new ByteArrayInputStream(stringBuilder.toString().getBytes(
                "UTF-8"));
    }
    
    private void applyAuth(IDiagramProfile profile, HttpURLConnection connection) {
        if (profile.getUsr() != null && profile.getUsr().trim().length() > 0
                && profile.getPwd() != null
                && profile.getPwd().trim().length() > 0) {
            BASE64Encoder enc = new sun.misc.BASE64Encoder();
            String userpassword = profile.getUsr() + ":" + profile.getPwd();
            String encodedAuthorization = enc.encode(userpassword.getBytes());
            connection.setRequestProperty("Authorization", "Basic "
                    + encodedAuthorization);
        }
    }
    
    private boolean taskFormExistsInGuvnor(String packageName, String assetName, String taskFormName, IDiagramProfile profile) {
    	try {	
    		String formURL = ExternalInfo.getExternalProtocol(profile)
    	        + "://"
    	        + ExternalInfo.getExternalHost(profile)
    	        + "/"
    	        + profile.getExternalLoadURLSubdomain().substring(0,
    	                profile.getExternalLoadURLSubdomain().indexOf("/"))
    	        + "/rest/packages/" + packageName + "/assets/" + URLEncoder.encode(taskFormName, "UTF-8");
    	
    	
			URL checkURL = new URL(formURL);
			HttpURLConnection checkConnection = (HttpURLConnection) checkURL
			        .openConnection();
			applyAuth(profile, checkConnection);
			checkConnection.setRequestMethod("GET");
			checkConnection
			        .setRequestProperty("Accept", "application/atom+xml");
			checkConnection.connect();
			_logger.info("check connection response code: " + checkConnection.getResponseCode());
			if (checkConnection.getResponseCode() == 200) {
				return true;
			}
		} catch (Exception e) {
			_logger.error(e.getMessage());
		}
        return false;
    }
    
    public List<String> getAllProcessesInPackage(String pkgName, IDiagramProfile profile) {
        List<String> processes = new ArrayList<String>();
        String assetsURL = ExternalInfo.getExternalProtocol(profile)
                + "://"
                + ExternalInfo.getExternalHost(profile)
                + "/"
                + profile.getExternalLoadURLSubdomain().substring(0,
    	                profile.getExternalLoadURLSubdomain().indexOf("/"))
                + "/rest/packages/"
                + pkgName
                + "/assets/";
        
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(getInputStreamForURL(assetsURL, "GET", profile));

            String format = "";
            String title = ""; 
            while (reader.hasNext()) {
                int next = reader.next();
                if (next == XMLStreamReader.START_ELEMENT) {
                    if ("format".equals(reader.getLocalName())) {
                        format = reader.getElementText();
                    } 
                    if ("title".equals(reader.getLocalName())) {
                        title = reader.getElementText();
                    }
                    if ("asset".equals(reader.getLocalName())) {
                        if(format.equals(EXT_BPMN) || format.equals(EXT_BPMN2)) {
                            processes.add(title);
                            title = "";
                            format = "";
                        }
                    }
                }
            }
            // last one
            if(format.equals(EXT_BPMN) || format.equals(EXT_BPMN2)) {
                processes.add(title);
            }
        } catch (Exception e) {
        	_logger.error("Error finding processes in package: " + e.getMessage());
        } 
        return processes;
    }
    
    private String getProcessSourceContent(String packageName, String assetName, IDiagramProfile profile) {
        String assetSourceURL = ExternalInfo.getExternalProtocol(profile)
                + "://"
                + ExternalInfo.getExternalHost(profile)
                + "/"
                + profile.getExternalLoadURLSubdomain().substring(0,
    	                profile.getExternalLoadURLSubdomain().indexOf("/"))
                + "/rest/packages/" + packageName + "/assets/" + assetName
                + "/source/";

        try {
            InputStream in = getInputStreamForURL(assetSourceURL, "GET", profile);
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer);
            return writer.toString();
        } catch (Exception e) {
        	_logger.error("Error retrieving asset content: " + e.getMessage());
            return "";
        }
    }
    
    private boolean isAdHocProcess(Process process) {
        Iterator<FeatureMap.Entry> iter = process.getAnyAttribute().iterator();
        while(iter.hasNext()) {
            FeatureMap.Entry entry = iter.next();
            if(entry.getEStructuralFeature().getName().equals("adHoc")) {
            	return Boolean.parseBoolean(((String)entry.getValue()).trim());
            }
        }
        return false;
    }
    
    private boolean containsWhiteSpace(String testString){
        if(testString != null){
            for(int i = 0; i < testString.length(); i++){
                if(Character.isWhitespace(testString.charAt(i))){
                    return true;
                }
            }
        }
        return false;
    }
}
