package scouterx.webapp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Created by David Kim (david100gom@gmail.com) on 2019. 5. 25.
 *
 * Github : https://github.com/david100gom
 */
@Getter
@Setter
@ToString
public class HeapHistogramData {

    public int no;
    public int count;
    public long size;
    public String name;

}
