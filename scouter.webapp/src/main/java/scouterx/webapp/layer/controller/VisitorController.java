package scouterx.webapp.layer.controller;

import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.framework.util.ZZ;
import scouterx.webapp.layer.service.VisitorService;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * Created by geonheelee on 2017. 10. 13..
 */
@Path("/vi/visitor")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class VisitorController {

    @Context
    HttpServletRequest servletRequest;

    final VisitorService visitorService = new VisitorService();

    @GET
    @Path("/realTime/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Long> retrieveVisitorRealTimeCountersByObjId(
            @PathParam("objHash") final int objHash,
            @QueryParam("serverId") final int serverId){

        Long visitorRealTime = visitorService.retrieveVisitorRealTimeCountersByObjId(objHash, ServerManager.getInstance().getServerIfNullDefault(serverId));
        return CommonResultView.success(visitorRealTime);
    }

    @GET
    @Path("/realTime/total/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Long> retrieveVisitorTotalRealTimeCounterByObjType(
            @PathParam("objType") final String objType,
            @QueryParam("serverId") final int serverId){
        Long visitorTotalRealTime = visitorService.retrieveVisitorTotalRealTimeCounterByObjType(objType, ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(visitorTotalRealTime);
    }

    @GET
    @Path("/realTime/group")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<Long> retrieveVisitorGroupRealTimeCounterByObjId(
            @QueryParam("objHashes") String objHashes,
            @QueryParam("serverId") final int serverId){
        List<Integer> objList = ZZ.splitParamAsInteger(objHashes);

        Long visitorGroupRealTime = visitorService.retrieveVisitorGroupRealTimeCounterByObjId(objList,ServerManager.getInstance().getServerIfNullDefault(serverId));

        return CommonResultView.success(visitorGroupRealTime);
    }

}
