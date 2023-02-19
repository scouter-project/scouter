package scouterx.webapp.model.alertscript;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ScriptingSaveStateData {
    int status;
    String message;

    //default define
    public ScriptingSaveStateData(){
        this.message = "No settings have been changed";
        this.status = 301;
    }
}
