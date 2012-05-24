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
import org.eclipse.bpmn2.Artifact;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.BusinessRuleTask;
import org.eclipse.bpmn2.CallActivity;
import org.eclipse.bpmn2.CatchEvent;
import org.eclipse.bpmn2.CompensateEventDefinition;
import org.eclipse.bpmn2.ConditionalEventDefinition;
import org.eclipse.bpmn2.DataObject;
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
import org.jbpm.designer.web.profile.IDiagramProfile;
import org.jbpm.designer.web.profile.impl.ExternalInfo;
import org.jbpm.designer.web.server.ServletUtil;
import org.json.JSONObject;
import sun.misc.BASE64Encoder;


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
	
	//protected List<FlowNode> tmp = new ArrayList<FlowNode>();
	protected List<Object> tmp = new ArrayList<Object>();

	private String json;
	private String preprocessingData;
	private IDiagramProfile profile;
	private String defaultResourceId = "";
	private String uuid;
	
    //static NodeSequence nodes[] = new NodeSequence[50];
    
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
		

	try{
			  // Create file 
			  FileWriter fstream = new FileWriter("root1.log",false);
			  BufferedWriter out = new BufferedWriter(fstream);
		
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
//            				nodes[count] = new NodeSequence(fe.getId(), fe.getName(), "");
//            				count++;
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
           						//out.write(fe.getName()+"\n");
           				}
       				}
           		}        	
			}
		}
			
		   
		   Iterator< String > iter = smList.iterator();
		   while(iter.hasNext()){
			   Object element = iter.next();
			   if(!(udmList.contains(element))){
				   //out.write(element+"\n\n");
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
	        				//if(!(sf.getSourceRef() instanceof StartEvent)&&!(sf.getTargetRef() instanceof EndEvent)){
	        				if(!(sf.getTargetRef() instanceof EndEvent)){
	        					out.write(sf.getTargetRef().getName()+":"+sf.getTargetRef().getId()+"\t"+sf.getSourceRef().getName()+":"+sf.getSourceRef().getId()+"\n");
	        					addSequenceFlow(sf.getTargetRef(),sf.getSourceRef());
	        				}
		           		}
	           		}
	           		
	           		
				}
		   }
		   
		   out.write("\n\n");
		   //UD
		   for(RootElement root : udmrootElements) {
				if(root instanceof Process) {
	        		Process process = (Process) root;
	        	
	           		List<FlowElement> flowElements =  process.getFlowElements();
	           		for(FlowElement fe : flowElements) {
	           			if(fe instanceof SequenceFlow) {
	        				SequenceFlow sf = (SequenceFlow) fe;
	        				//if(!(sf.getSourceRef() instanceof StartEvent)&&!(sf.getTargetRef() instanceof EndEvent)){
	        				if(!(sf.getTargetRef() instanceof EndEvent)){
	        					out.write(sf.getTargetRef().getName()+":"+sf.getTargetRef().getId()+"\t"+sf.getSourceRef().getName()+":"+sf.getSourceRef().getId()+"\n");
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
		   			//out.write("\n"+sf.getName() +"\t");
			   		List current = new ArrayList();
			   		current = tmpSmList.get(me.getKey());
			   		List<Object> _obj = new ArrayList<Object>();
			   		
			   		for(Object temp: current ) {
		   				FlowNode ssf = (FlowNode) temp;
		   				if(!(ssf instanceof Gateway)) {
		   					_obj.add(temp);  
		   					//out.write(ssf.getName()+ " ,"); 
		   				}
		   				if(ssf instanceof Gateway) {
		   					findTarget(temp);	   					
		   					for(Object _current:tmp){
		   						_obj.add(_current); 
		   						FlowNode _val = (FlowNode) _current;
		   						//out.write(_val.getName() + " ,");
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
				//out.write("\n\n");
		   		Map.Entry me = (Map.Entry)iter3.next(); 
		   		FlowNode sf = (FlowNode) me.getKey();
		   		List<Object> _obj = new ArrayList<Object>();
		   		if(!(sf instanceof Gateway) ) {
			   			findTargetUdm(sf);
			   			for(Object _current:tmp){
	   						_obj.add(_current); 
	   						//FlowNode _val = (FlowNode) _current;
	   						//out.write(_current + "\n");
	   					}
			   			finalUmList.put(sf,_obj);
		   		}
		   	} 

			for (Map.Entry<Object, List<Object>> entry : tmpSmList.entrySet()) {
				FlowNode _key = (FlowNode) entry.getKey();
				out.write("\n"+_key.getName() +"\t");
		   		List<Object> current = new ArrayList<Object>();
		   		current = entry.getValue();
		   		for(Object _value:current ) {
		   			FlowNode sf = (FlowNode) _value;
		   			out.write(sf.getName()+ " | ");
		   		}
			}
			
			out.write("\n\n******************\n\n");
			
			for (Map.Entry<Object, List<Object>> entry : finalSmList.entrySet()) {
				FlowNode _key = (FlowNode) entry.getKey();
				out.write("\n"+_key.getName() +"\t");
		   		List<Object> current = new ArrayList<Object>();
		   		current = entry.getValue();
		   		for(Object _value:current ) {
		   			FlowNode sf = (FlowNode) _value;
		   			out.write(sf.getName()+ " | ");
		   		}
			}
			 			
			out.write("\n\n******************\n\n");
			
			for (Map.Entry<Object, List<Object>> entry : tmpUmList.entrySet()) {
				FlowNode _key = (FlowNode) entry.getKey();
				out.write("\n"+_key.getName() +"\t");
		   		List<Object> current = new ArrayList<Object>();
		   		current = entry.getValue();
		   		for(Object _value:current ) {
		   			FlowNode sf = (FlowNode) _value;
		   			out.write(sf.getName()+ " | ");
		   		}
			}
			
			out.write("\n\n******************\n\n");
			
			for (Map.Entry<Object, List<Object>> entry : finalUmList.entrySet()) {
				FlowNode _key = (FlowNode) entry.getKey();
				out.write("\n"+_key.getName() +"\t");
		   		List<Object> current = new ArrayList<Object>();
		   		current = entry.getValue();
		   		for(Object _value:current ) {
		   			FlowNode sf = (FlowNode) _value;
		   			out.write(sf.getName()+ " | ");
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
				   			//if(fnSm.getName() != "") {
					   			for(Object _valueUd:currentUd) {				   				
						   			FlowNode fnUd = (FlowNode) _valueUd;
					   				//if((fnSm.getName()).equals(fnUd.getName()) || fnSm.getName().equals("Start")){
					   				if((fnSm.getName()).equals(fnUd.getName()) || (fnSm instanceof StartEvent)){

					   					foundError = false;
					   				} 
					   			}
				   			//}
				   			if(foundError){
				   				FlowNode udKey = (FlowNode) entryUd.getKey();
						   		//out.write("\n"+udKey.getId()+ "Missiong Pre-condition node: "+fnSm.getName());
						   		addError(udKey.getId(), "Missing Pre-condition node: "+fnSm.getName());
				   			}
				   			foundError = false;
				   		}
					}
				}
			}
			
						
		   		out.close();
	          		  }catch (Exception e){//Catch exception if any
	          		  System.err.println("Error: " + e.getMessage());
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
		   				} else {
			   				_obj = ssf;
			   				findTargetUdm(ssf);
		   				}
		   			}
	   	       }
	   	   }
	}
/*
 * private void findTargetUdm(Object _obj){
   	    
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
			   			} else {
			   			if((ssf instanceof Gateway) && ((FlowNode) me.getKey() instanceof Gateway)){ 
			   				_obj = ssf;
			   				findTargetUdm(ssf);
			   			}
			   		}
			   	}
	   	       }
	   	   }
	}*/
	
	
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

	public void checkSyntax_bak() {
		
		List<RootElement> rootElements =  smdef.getRootElements();

        for(RootElement root : rootElements) {
        	if(root instanceof Process) {
        		Process process = (Process) root;
        		if(process.getFlowElements() != null && process.getFlowElements().size() > 0) {
        			defaultResourceId = process.getFlowElements().get(0).getId();
        		}
        		
        		if(isEmpty(process.getId())) {
        			addError(defaultResourceId, "Process has no id.");
        		} else {
        			if(!SyntaxCheckerUtils.isNCName(process.getId())) {
        				addError(defaultResourceId, "Invalid process id. See http://www.w3.org/TR/REC-xml-names/#NT-NCName for more info.");
        			} else {
        				String[] packageAssetInfo = ServletUtil.findPackageAndAssetInfo(uuid, profile);
		        		String processImageName = process.getId() + "-image";
		        		if(!ServletUtil.assetExistsInGuvnor(packageAssetInfo[0], processImageName, profile)) {
		        			addError(defaultResourceId, "Could not find process image.");
		        		}
        			}
        		}
        		
        		String pname = null;
        		Iterator<FeatureMap.Entry> iter = process.getAnyAttribute().iterator();
        		boolean foundPackageName = false;
                while(iter.hasNext()) {
                    FeatureMap.Entry entry = iter.next();
                    if(entry.getEStructuralFeature().getName().equals("packageName")) {
                    	foundPackageName = true;
                        pname = (String) entry.getValue();
                        if(isEmpty(pname)) {
                        	addError(defaultResourceId, "Process has no package name.");
                        }
                    }
                }
                if(!foundPackageName) {
                	addError(defaultResourceId, "Process has no package name.");
                } else {
                	if(!isEmpty(pname)) {
                		String[] packageAssetInfo = findPackageAndAssetInfo(uuid, profile);
                		String guvnorPackageName = packageAssetInfo[0];
                		if(!guvnorPackageName.equals(pname)) {
                			addError(defaultResourceId, "Process package name is not valid.");
                		}
                	}
                }
                
                if(isEmpty(process.getName())) {
        			addError(defaultResourceId, "Process has no name.");
        		}
                
                boolean foundStartEvent = false;
                boolean foundEndEvent = false;
        		List<FlowElement> flowElements =  process.getFlowElements();
        		for(FlowElement fe : flowElements) {
        			if(fe instanceof StartEvent) {
        				foundStartEvent = true;
        			}
        			if(fe instanceof EndEvent) {
        				foundEndEvent = true;
        			}
        		}
        		if(!foundStartEvent && !isAdHocProcess(process)) {
        			addError(defaultResourceId, "Process has no start node.");
        		}
        		if(!foundEndEvent && !isAdHocProcess(process)) {
        			addError(defaultResourceId, "Process has no end node.");
        		}
        		
        		checkFlowElements(process);
        	}
        }
	}
	
	private void checkFlowElements(FlowElementsContainer container) {
		
		for(FlowElement fe : container.getFlowElements()) {
			if(fe instanceof StartEvent) {
				StartEvent se = (StartEvent) fe;
				if(se.getOutgoing() == null && se.getOutgoing().size() < 1) {
					addError(se, "Start node has no outgoing connections");
				}
			} else if (fe instanceof EndEvent) {
				EndEvent ee = (EndEvent) fe;
				if(ee.getIncoming() == null && ee.getIncoming().size() < 1) {
					addError(ee, "End node has no outgoing connections");
				}
			} else {
				if(fe instanceof FlowNode) {
					FlowNode fn = (FlowNode) fe;
					if(fn.getOutgoing() == null && fn.getOutgoing().size() < 1) {
    					addError(fn, "Node has no outgoing connections");
    				}
					if(fn.getIncoming() == null && fn.getIncoming().size() < 1) {
    					addError(fn, "Node has no outgoing connections");
    				}
				}
			}
			
			if(fe instanceof BusinessRuleTask) {
				BusinessRuleTask bt = (BusinessRuleTask) fe;
				Iterator<FeatureMap.Entry> biter = bt.getAnyAttribute().iterator();
				boolean foundRuleflowGroup = false;
	            while(biter.hasNext()) {
	                FeatureMap.Entry entry = biter.next();
	                if(entry.getEStructuralFeature().getName().equals("ruleFlowGroup")) {
	                	foundRuleflowGroup = true;
	                	String ruleflowGroup = (String) entry.getValue();
	                	if(isEmpty(ruleflowGroup)) {
	                		addError(bt, "Business Rule Task has no ruleflow-group.");
	                	}
	                }
	            }
	            if(!foundRuleflowGroup) {
	            	addError(bt, "Business Rule Task has no ruleflow-group.");
	            }
			}
			
			if(fe instanceof ScriptTask) {
				ScriptTask st = (ScriptTask) fe;
				if(isEmpty(st.getScript())) {
					addError(st, "Script Task has no script.");
				}
				if(isEmpty(st.getScriptFormat())) {
					addError(st, "Script Task has no script format.");
				}
			}
			
			if(fe instanceof SendTask) {
				SendTask st = (SendTask) fe;
				if(st.getOperationRef() == null) {
					addError(st, "Send Task has no operation.");
				}
				if(st.getMessageRef() == null) {
					addError(st, "Send Task has no message.");
				}
			}
			
			if(fe instanceof UserTask) {
				UserTask ut = (UserTask) fe;
				String taskName = null;
				Iterator<FeatureMap.Entry> utiter = ut.getAnyAttribute().iterator();
				boolean foundTaskName = false;
		        while(utiter.hasNext()) {
		            FeatureMap.Entry entry = utiter.next();
		            if(entry.getEStructuralFeature().getName().equals("taskName")) {
		            	foundTaskName = true;
		            	taskName = (String) entry.getValue();
		            	if(isEmpty(taskName)) {
		            		addError(ut, "User Task has no task name.");
		            	}
		            }
		        }
		        if(!foundTaskName) {
		        	addError(ut, "User Task has no task name.");
		        } else {
		        	if(taskName != null) {
		        		String[] packageAssetInfo = findPackageAndAssetInfo(uuid, profile);
		        		String packageName = packageAssetInfo[0];
		        		String assetName = packageAssetInfo[1];
		        		String taskFormName = taskName + "-taskform";
		        		if(!taskFormExistsInGuvnor(packageName, assetName, taskFormName, profile)) {
		        			addError(ut, "User Task has no task form defined.");
		        		}
		        	} 
		        }
			}
			
			if(fe instanceof CatchEvent) {
				CatchEvent event = (CatchEvent) fe;
				List<EventDefinition> eventdefs = event.getEventDefinitions();
				for(EventDefinition ed : eventdefs) {
    				if(ed instanceof TimerEventDefinition) {
    	                TimerEventDefinition ted = (TimerEventDefinition) ed;
    	                if(ted.getTimeDate() == null) {
    	                	addError(event, "Catch Event has no timedate.");
    	                }
    	                if(ted.getTimeDuration() == null) {
    	                	addError(event, "Catch Event has no timeduration.");
    	                }
    	                if(ted.getTimeCycle() == null) {
    	                	addError(event, "Catch Event has no timecycle.");
    	                }
    	            } else if( ed instanceof SignalEventDefinition) {
    	                if(((SignalEventDefinition) ed).getSignalRef() == null) {
    	                	addError(event, "Catch Event has no signalref.");
    	                }
    	            } else if( ed instanceof ErrorEventDefinition) {
    	                if(((ErrorEventDefinition) ed).getErrorRef() == null || ((ErrorEventDefinition) ed).getErrorRef().getErrorCode() == null) {
    	                	addError(event, "Catch Event has no errorref.");
    	                }
    	            } else if( ed instanceof ConditionalEventDefinition ) {
    	                FormalExpression conditionalExp = (FormalExpression) ((ConditionalEventDefinition) ed).getCondition();
    	                if(conditionalExp.getBody() == null) {
    	                	addError(event, "Catch Event has no conditionexpression.");
    	                }
    	            } else if( ed instanceof EscalationEventDefinition ) {
    	                if(((EscalationEventDefinition) ed).getEscalationRef() == null) {
    	                	addError(event, "Catch Event has no escalationref.");
    	                }
    	            } else if( ed instanceof MessageEventDefinition) {
    	                if(((MessageEventDefinition) ed).getMessageRef() == null) {
    	                    addError(event, "Catch Event has no messageref.");
    	                }
    	            }  else if( ed instanceof CompensateEventDefinition) {
    	                if(((CompensateEventDefinition) ed).getActivityRef() == null) {
    	                	addError(event, "Catch Event has no activityref.");
    	                }
    	            } 
				}
			}
			
			if(fe instanceof ThrowEvent) {
				ThrowEvent event = (ThrowEvent) fe;
				List<EventDefinition> eventdefs = event.getEventDefinitions();
		        for(EventDefinition ed : eventdefs) {
		            if(ed instanceof TimerEventDefinition) {
		                TimerEventDefinition ted = (TimerEventDefinition) ed;
		                if(ted.getTimeDate() == null) {
		                	addError(event, "Throw Event has no timedate.");
		                }
		                if(ted.getTimeDuration() == null) {
		                	addError(event, "Throw Event has no timeduration.");
		                }
		                if(ted.getTimeCycle() != null) {
		                	addError(event, "Throw Event has no timecycle.");
		                }
		            } else if( ed instanceof SignalEventDefinition) {
		                if(((SignalEventDefinition) ed).getSignalRef() == null) {
		                	addError(event, "Throw Event has no signalref.");
		                }
		            } else if( ed instanceof ErrorEventDefinition) {
		                if(((ErrorEventDefinition) ed).getErrorRef() == null || ((ErrorEventDefinition) ed).getErrorRef().getErrorCode() == null) {
		                	addError(event, "Throw Event has no errorref.");
		                }
		            } else if( ed instanceof ConditionalEventDefinition ) {
		                FormalExpression conditionalExp = (FormalExpression) ((ConditionalEventDefinition) ed).getCondition();
		                if(conditionalExp.getBody() == null) {
		                	addError(event, "Throw Event has no conditional expression.");
		                }
		            } else if( ed instanceof EscalationEventDefinition ) {
		                if(((EscalationEventDefinition) ed).getEscalationRef() == null) {
		                	addError(event, "Throw Event has no conditional escalationref.");
		                }
		            } else if( ed instanceof MessageEventDefinition) {
		                if(((MessageEventDefinition) ed).getMessageRef() == null) {
		                	addError(event, "Throw Event has no conditional messageref.");
		                }
		            }  else if( ed instanceof CompensateEventDefinition) {
		                if(((CompensateEventDefinition) ed).getActivityRef() == null) {
		                	addError(event, "Throw Event has no conditional activityref.");
		                }
		            }  
		        }
			}
			
			if(fe instanceof SequenceFlow) {
				SequenceFlow sf = (SequenceFlow) fe;
				if(sf.getSourceRef() == null) {
					addError((SequenceFlow) fe, "An Edge must have a source node.");
				}
				if(sf.getTargetRef() == null) {
					addError((SequenceFlow) fe, "An Edge must have a target node.");
				}
			}
			
			if(fe instanceof Gateway) {
				Gateway gw = (Gateway) fe;
				if(gw.getGatewayDirection() == null) {
					addError((Gateway) fe, "Gateway has no direction.");
				}
			}
			
			if(fe instanceof CallActivity) {
				CallActivity ca = (CallActivity) fe;
				if(ca.getCalledElement() == null || ca.getCalledElement().length() < 1) {
					addError((CallActivity) fe, "Reusable Subprocess has no called element specified.");
				} else {
					String[] packageAssetInfo = findPackageAndAssetInfo(uuid, profile);
	        		String packageName = packageAssetInfo[0];
	        		List<String> allProcessesInPackage = getAllProcessesInPackage(packageName, profile);
	        		boolean foundCalledElementProcess = false;
	        		for(String p : allProcessesInPackage) {
	        			String processContent = getProcessSourceContent(packageName, p, profile);
	        			Pattern pattern = Pattern.compile("<\\S*process[\\s\\S]*id=\"" + ca.getCalledElement() + "\"", Pattern.MULTILINE);
	                    Matcher m = pattern.matcher(processContent);
	                    if(m.find()) {
	                    	foundCalledElementProcess = true;
	                    	break;
	                    }
	        		}
	        		if(!foundCalledElementProcess) {
	        			addError((CallActivity) fe, "No existing process with id=" + ca.getCalledElement() + " could be found.");
	        		}
				}
			}
			
			if(fe instanceof DataObject) {
				DataObject dao = (DataObject) fe;
				if(dao.getName() == null || dao.getName().length() < 1) {
					addError((DataObject) fe, "Data Object has no name defined.");
				} else {
					if(containsWhiteSpace(dao.getName())) {
						addError((DataObject) fe, "Data Object name contains white spaces.");
					}
				}
			}
			
			if(fe instanceof SubProcess) {
				checkFlowElements((SubProcess) fe);
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
