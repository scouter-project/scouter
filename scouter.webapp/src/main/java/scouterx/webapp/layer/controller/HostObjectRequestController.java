package scouterx.webapp.layer.controller;

import io.swagger.annotations.*;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.layer.service.HostObjectRequestService;
import scouterx.webapp.model.HostDiskData;
import scouterx.webapp.model.ProcessObject;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 14.
 *
 * Modified by David Kim (david100gom@gmail.com) on 2019. 5. 12.
 *
 *
 */
@Path("/v1/object/host")
@Api("Host object request")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class HostObjectRequestController {

    @Context
    HttpServletRequest servletRequest;

    private final HostObjectRequestService objectRequestService;

    public HostObjectRequestController() {
        this.objectRequestService = new HostObjectRequestService();
    }

    @GET
    @ApiOperation(value = "/realTime/top/ofObject/{objHash}", notes = "Get system process information by TOP command.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Path("/realTime/top/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<ProcessObject>> retrieveRealTimeTopByObjType(
            @PathParam("objHash") @Valid @NotNull final int objHash, @QueryParam("serverId") final int serverId) {
        final Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);

        final List<ProcessObject> processObjects = this.objectRequestService.retrieveRealTimeTopByObjType(objHash, server);

        return CommonResultView.success(processObjects);
    }

    @GET
    @ApiOperation(value = "/realTime/disk/ofObject/{objHash}", notes = "Get disk usage information")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK - Json Data"),
            @ApiResponse(code = 500, message = "Server error")
    })
    @Path("/realTime/disk/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<HostDiskData>> retrieveRealTimeDiskByObjType(
            @PathParam("objHash") @Valid @NotNull final int objHash, @QueryParam("serverId") final int serverId) {

        Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        List<HostDiskData> processObjects = this.objectRequestService.retrieveRealTimeDiskByObjType(objHash, server);
        return CommonResultView.success(processObjects);
    }
}
