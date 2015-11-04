package scouter.server.alert;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

public class Test {
	public static void main(String[] args) throws Exception {
		ClassPool cp = ClassPool.getDefault();
		CtClass cc = cp.get("scouter.server.alsert.AlertRule");

		CtClass c1 = cp.makeClass("XXX", cc);
		CtMethod m = CtNewMethod.make("public void say(Context c){}", c1);
		c1.addMethod(m);
		m.setBody("warning($1,10f);");
		m.insertBefore("System.out.println(\"ssss\");");
		Class c = c1.toClass();
		AlertRule h = (AlertRule) c.newInstance();
		
	}
}