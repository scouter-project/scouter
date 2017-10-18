package scouterx.webapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import scouter.lang.pack.MapPack;

/**
 * Created by csk746(csk746@naver.com) on 2017. 10. 15..
 */
@Getter
@ToString
@AllArgsConstructor
@Builder
public class VisitorGroup {

    private String time;

    private long value;

    public static VisitorGroup of(MapPack mapPack){
        return VisitorGroup.builder().time(mapPack.getText("time")).value(mapPack.getLong("value")).build();
    }

}
