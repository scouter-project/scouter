package scouter.server.netio.data;

import scouter.lang.pack.Pack;

/**
 * Created by LeeGunHee on 2016-03-08.
 */
public interface IPackProcessor {

    /**
     * Pack processor
     * @return true if pack matched type
     */
    public boolean process(Pack pack);
}
