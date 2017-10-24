package scouterx.webapp.layer.controller;

import io.swagger.annotations.*;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.layer.service.HostObjectRequestService;
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
 */
@Path("/v1/object/host")
@Api(value = "/v1/object/host")
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
    @ApiOperation(value = "/realTime/top/ofObject/{objHash}", notes = "Hello world example - java code.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK - java code"),
            @ApiResponse(code = 400, message = "Bad Request examp - java code.") })
    @ApiImplicitParams(value = {
            @ApiImplicitParam(name = "xyz", dataType = "string", paramType = "path"),
            @ApiImplicitParam(name = "abc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "my-header", dataType = "string", paramType = "header") })
    @Path("/realTime/top/ofObject/{objHash}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<ProcessObject>> retrieveRealTimeTopByObjType(
            @PathParam("objHash") @Valid @NotNull final int objHash, @QueryParam("serverId") final int serverId) {
        final Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);

        final List<ProcessObject> processObjects = this.objectRequestService.retrieveRealTimeTopByObjType(objHash, server);

        return CommonResultView.success(processObjects);
    }
}
