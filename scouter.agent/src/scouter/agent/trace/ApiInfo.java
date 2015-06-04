package scouter.agent.trace;

public class ApiInfo {
	public String className;
	public String methodName;
	public String methodDesc;
	public Object _this;
	public Object[] arg;

	public ApiInfo(String className, String methodName, String methodDesc, Object _this, Object[] arg) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this._this = _this;
		this.arg = arg;
	}

}