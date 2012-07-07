package org.jbpm.designer.bpmn2.compliance;

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

import org.apache.log4j.Logger;
import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.DataInput;
import org.eclipse.bpmn2.DataInputAssociation;
import org.eclipse.bpmn2.DataOutput;
import org.eclipse.bpmn2.DataOutputAssociation;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.EndEvent;
import org.eclipse.bpmn2.FlowElement;
import org.eclipse.bpmn2.FlowNode;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.ItemAwareElement;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.Property;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.StartEvent;
import org.json.JSONObject;
import org.jbpm.designer.bpmn2.compliance.NodeDataInfo;
import org.jbpm.designer.web.profile.IDiagramProfile;

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
	
	protected List<Object> smProcessVar;
	protected List<Object> udProcessVar;

	private String defaultResourceId = "";
	private Boolean stopLoop;
	    
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
		  
			for (Map.Entry<Object, List<Object>> entry : tmpSmList.entrySet()) {
		   		tmp.clear();
				FlowNode _key = (FlowNode) entry.getKey();
				if(!(_key instanceof Gateway)){
			   		List<Object> _obj = new ArrayList<Object>();
					findTarget(_key);
		   			for(Object _current:tmp){
						_obj.add(_current); 
					}
			   		finalSmList.put(_key,_obj);
				}
		   	} 
		    
		   	// User Define
			for (Map.Entry<Object, List<Object>> entry : tmpUmList.entrySet()) {
		   		tmp.clear();
		   		stopLoop = true;
				FlowNode _key = (FlowNode) entry.getKey();
				if(!(_key instanceof Gateway)){
			   		List<Object> _obj = new ArrayList<Object>();
					findTargetUdm(_key,_key);
		   			for(Object _current:tmp){
   						_obj.add(_current); 
   					}
			   		finalUmList.put(_key,_obj);
				}
			}
			


			 			
	  		// comparison model sm & ud usertask

			for (Map.Entry<Object, List<Object>> entrySm : finalSmList.entrySet()) {
				//boolean match = false;
				FlowNode _keySm = (FlowNode) entrySm.getKey();

				for (Map.Entry<Object, List<Object>> entryUd : finalUmList.entrySet()) {

					FlowNode _keyUd = (FlowNode) entryUd.getKey();
					
					if(_keySm.getName().equals(_keyUd.getName())){
						//match = true; 
				   		List<Object> currentSm = new ArrayList<Object>();
				   		List<Object> currentUd = new ArrayList<Object>();
				   		currentSm = entrySm.getValue();
				   		currentUd = entryUd.getValue();				   		
				   		
				   		for(Object _valueSm:currentSm ) {
				   			FlowNode fnSm = (FlowNode) _valueSm;
				   				boolean foundError = true;
					   			for(Object _valueUd:currentUd) {				   				
						   			FlowNode fnUd = (FlowNode) _valueUd;
					   				if((fnSm.getName()).equals(fnUd.getName())){// || (fnSm instanceof StartEvent)){
					   					foundError = false;
					   				}
					   				if(fnSm instanceof StartEvent && fnUd instanceof StartEvent){
					   					foundError = false;
					   				}
					   			}
					   			if(foundError){
					   				FlowNode udKey = (FlowNode) entryUd.getKey();
							   		addError(udKey.getId(), "Missing Pre-condition node: "+fnSm.getName());
					   			}
					   			//foundError = false;
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
  		
  		// comparison data sm & ud usertask
  		for (Map.Entry<Object, NodeDataInfo> _smDataInfo : smDataInfo.entrySet()) {
			FlowNode smFn = (FlowNode) _smDataInfo.getKey();
			if(!(udDataInfo.isEmpty())){
				for (Map.Entry<Object, NodeDataInfo> _udDataInfo : udDataInfo.entrySet()) {
					FlowNode udFn = (FlowNode) _udDataInfo.getKey();
					if (smFn.getName().equals(udFn.getName())) {
						NodeDataInfo udCurrent = _udDataInfo.getValue();
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
   	  try{ 
   		
		for (Map.Entry<Object, List<Object>> entry : tmpSmList.entrySet()) {
			FlowNode _key = (FlowNode) entry.getKey();
				if (_obj.equals(_key)) {
			   		List current = new ArrayList();
			   		current = entry.getValue();
			   		for(Object _value: current ) {
		   				FlowNode value = (FlowNode) _value;
		   				if(!(value instanceof Gateway)&&!tmp.contains(value)) {
		   					tmp.add(value);
			   			} else if((value instanceof Gateway)){ 
			   				findTarget(value);
			   			}
			   		}
			   	}
	   	   }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	private void findTargetUdm(Object _obj, Object _mainKey){ //key g1 - g2
		FlowNode value = null;
		try{		
			for (Map.Entry<Object, List<Object>> entry : tmpUmList.entrySet()) { 
					stopLoop = true;
 					FlowNode _key = (FlowNode) entry.getKey();
 					if (_obj.equals(_key)) { 
 				   		List current = new ArrayList();
 				   		current = entry.getValue();
	 				   	for (int i = 0; i < current.size(); i++) { 
 			   				value = (FlowNode) current.get(i);
 			   				if(!(value instanceof Gateway) && (!tmp.contains(value)) && (!value.equals(_mainKey))) {
 			   					tmp.add(value); 
 				   			} 
 			   				if(value instanceof Gateway){
 			   				List keyVal = new ArrayList(); 
 				   			keyVal =	tmpUmList.get(value); 
 					   		for(Object _keyVal: keyVal ) {
 				   				FlowNode _val = (FlowNode) _keyVal; 
 				   				if(tmp.contains(_val)) { 
 				   					stopLoop = false; 
 				   				} 
 		 				   	}
 				   			if(stopLoop){
 					   			findTargetUdm(value,_mainKey); 
 					   		}
 				   			}
 			   			}
	 				   List keyVal = new ArrayList(); 
			   			keyVal =	tmpUmList.get(value); 
				   		for(Object _keyVal: keyVal ) {
			   				FlowNode _val = (FlowNode) _keyVal; 
			   				if(tmp.contains(_val)) { 
			   					stopLoop = false; 
			   				} 
	 				   	}
			   			if(stopLoop){
				   			findTargetUdm(value,_mainKey); 
				   		}
 		   	      }
 		   	  }
			} catch (Exception e) {
				e.printStackTrace();
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

	
	
	private void smCheckData(Process container) {
					  
		try{
		  
	          List<Property> processProperties = container.getProperties();

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
	                
	                
	                if(!dataInputs.isEmpty() && dataInputs != null){
		                for(DataInput din : dataInputs) {
	                        for(DataInputAssociation inputAssociations : dataInputAssociations) {
	                                if(inputAssociations.getTargetRef().getId().equals(din.getId()) && !(din.getName().equals("TaskName") || din.getName().equals("ActorId") || din.getName().equals("GroupId")
                                            || din.getName().equals("Skippable") || din.getName().equals("Priority") || din.getName().equals("Comment"))){
	                                	for(Property prop : processProperties) {
	                                        if(prop.getId().equals(inputAssociations.getSourceRef().get(0).getId())) {
	                                        	dataIn.add(prop.getId());
	                                        }
	                                    }
	                                }
	                        }
	                    }
	                }

		            if(!dataOutputs.isEmpty() && dataOutputs != null){    
	                	for(DataOutput dout : dataOutputs) {
	                        for(DataOutputAssociation outputAssociation : dataOutputAssociations) {
	                            List<ItemAwareElement> sources = outputAssociation.getSourceRef();
	                            for(ItemAwareElement iae : sources) {
	                                if(iae.getId().equals(dout.getId())) {
	                                	for(Property prop : processProperties) {
	                                        if(prop.getId().equals(outputAssociation.getTargetRef().getId()) ) {
	        			                		dataOut.add(prop.getId());
	                                        }
	                                    }
	                                }
	                            }
	                        }
	                    }
		            }    
	                
		                
		                if((!dataInputs.isEmpty() && dataInputs != null) || (!dataOutputs.isEmpty() && dataOutputs != null)){ 
			                NodeDataInfo nodeDataInfo = new NodeDataInfo(dataIn, dataOut);
			        		smDataInfo.put(fn, nodeDataInfo); 
		                }
		            }
	            
		 }
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void udCheckData(Process container) {
		
		try{
		
          	List<Property> processProperties = container.getProperties();

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
                    for(DataInputAssociation inputAssociations : dataInputAssociations) {
                        if(inputAssociations.getTargetRef().getId().equals(din.getId()) && !(din.getName().equals("TaskName") || din.getName().equals("ActorId") || din.getName().equals("GroupId")
                                || din.getName().equals("Skippable") || din.getName().equals("Priority") || din.getName().equals("Comment"))){
                            	for(Property prop : processProperties) {
                                    if(prop.getId().equals(inputAssociations.getSourceRef().get(0).getId())) {
                                    	dataIn.add(prop.getId());
                                    }
                                }
                            }
                    }
                }
                
                for(DataOutput dout : dataOutputs) {
                    for(DataOutputAssociation outputAssociation : dataOutputAssociations) {
                        List<ItemAwareElement> sources = outputAssociation.getSourceRef();
                        for(ItemAwareElement iae : sources) {
                            if(iae.getId().equals(dout.getId())) {
                            	for(Property prop : processProperties) {
                                    if(prop.getId().equals(outputAssociation.getTargetRef().getId()) ) {
    			                		dataOut.add(prop.getId());
                                    }
                                }
                            }
                        }
                    }
                }
                
               
                
                if((!dataInputs.isEmpty() && dataInputs != null) || (!dataOutputs.isEmpty() && dataOutputs != null)){ 
	                NodeDataInfo nodeDataInfo = new NodeDataInfo(dataIn, dataOut);
	        		udDataInfo.put(fn, nodeDataInfo); 
                }
			}
		}
			
		} catch (Exception e) {
			e.printStackTrace();
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
	 
 
}
