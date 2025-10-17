package com.cattlescan.systemassistant.model;

import java.util.Map;

public class CustomerSupportAction {
	
    private String action;
    private Map<String, Object> parameters;
    private String result;
    
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public Map<String, Object> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}

}