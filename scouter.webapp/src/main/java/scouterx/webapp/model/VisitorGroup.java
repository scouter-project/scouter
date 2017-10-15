package scouterx.webapp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import scouter.lang.constants.ParamConstant;
import scouter.lang.pack.MapPack;

/**
 * Created by geonheelee on 2017. 10. 15..
 */
@Getter
@ToString
@AllArgsConstructor
public class VisitorGroup {

    private long time;

    private long value;

    public static VisitorGroup of(MapPack mapPack){
        return new VisitorGroup(
                mapPack.getLong(ParamConstant.DATE),
                mapPack.getLong(ParamConstant.VALUE));
    }

}
