package scouterx.webapp.model.alertscript;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiDesc {
    String desc;
    String methodName;
    String returnTypeName;
    String fullSignature;
}
