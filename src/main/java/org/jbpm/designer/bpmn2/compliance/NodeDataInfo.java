package org.jbpm.designer.bpmn2.compliance;

import java.util.ArrayList;
import java.util.List;

public class NodeDataInfo {
	String NodeName;
	List<String> DataIn = new ArrayList<String>();
	List<String> DataOut = new ArrayList<String>();
	
	public NodeDataInfo(List dataIn, List dataOut){
		DataIn = dataIn;
		DataOut = dataOut;
	}
	
	public NodeDataInfo(String nodeName){
		NodeName = nodeName;
	}

	public String getNodeName() {
		return NodeName;
	}

	public void setNodeName(String nodeName) {
		NodeName = nodeName;
	}

	public List<String> getDataIn() {
		return DataIn;
	}

	public void setDataIn(List<String> dataIn) {
		DataIn = dataIn;
	}

	public List<String> getDataOut() {
		return DataOut;
	}

	public void setDataOut(List<String> dataOut) {
		DataOut = dataOut;
	}
	

}
