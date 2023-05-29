package scouterx.webapp.model.alertscript;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ScriptingLogStateData {
    long loop;
    long index;
    List<String> message;

    //default define
    public ScriptingLogStateData(){
        this.loop=0;
        this.index=0;
        this.message= new ArrayList<>();
    }
}
