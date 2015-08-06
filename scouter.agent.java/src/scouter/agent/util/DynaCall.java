package scouter.agent.util;

import java.lang.reflect.Method;

public class DynaCall {
	boolean enable = true;
	Class dynaClass;
	Method method;

	public DynaCall(Object o, String name, Class... arg) {
		try {
			this.dynaClass = o.getClass();
			this.method = dynaClass.getMethod(name, arg);
		} catch (Exception e) {
			enable = false;
		}
	}

	public boolean isEnabled(){
		return enable;
	}
	public void disabled(){
		this.enable=false;
	}
	public Object call(Object o , Object ... args) throws Exception{
		return method.invoke(o, args);
	}
}
