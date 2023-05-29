package scouterx.webapp.model.alertscript;

import lombok.Getter;
import lombok.Setter;
import scouter.util.HashUtil;

import java.util.Map;
import java.util.TreeMap;

@Getter
@Setter
public class ScriptingLoadData {

    String ruleText;
    String configText;
    int configHash;
    int ruleTextHash;
    Map<String,ApiDesc> realCounterDescMap;
    Map<String,ApiDesc> pluginHelperDescMap;



    public ScriptingLoadData(){
        this.ruleText="";
        this.configText="";
        this.realCounterDescMap=new TreeMap<>();
        this.pluginHelperDescMap=new TreeMap<>();
    }

    public void setConfigText(String configText) {
        this.configText = configText;
        this.configHash = HashUtil.hash(configText);
    }
    public void setRuleText(String ruleText){
        this.ruleText = ruleText;
        this.ruleTextHash = HashUtil.hash(this.ruleText);
    }
}
