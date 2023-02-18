package scouterx.webapp.model.configure;

import lombok.Getter;
import lombok.Setter;
import scouter.lang.conf.ValueType;
import scouter.lang.conf.ValueTypeDesc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Setter
@Getter
public class ConfigureData {


    String contents;
    final List<ConfObject> configStateList;
    final Map<String, String> descMap;
    final Map<String, ValueType> valueTypeMap;
    final Map<String, ValueTypeDesc> valueTypeDescMap;
    int status;

    public ConfigureData(){
        this.configStateList = new ArrayList<>();
        this.descMap  = new LinkedHashMap<>();
        this.valueTypeMap  = new LinkedHashMap<>();
        this.valueTypeDescMap  = new LinkedHashMap<>();
        this.status =0;
        this.contents = "";
    }
}
