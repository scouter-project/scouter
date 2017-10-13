package scouterx.webapp.layer.controller;

import org.apache.commons.collections.CollectionUtils;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;
import scouterx.webapp.layer.service.AgentService;
import scouterx.webapp.layer.service.ObjectService;
import scouterx.webapp.model.ProcessObject;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author leekyoungil (leekyoungil@gmail.com) on 2017. 10. 14.
 */
@Path("/v1/object")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class ObjectRequestController {

    @Context
    HttpServletRequest servletRequest;

    private final ObjectService objectService;
    private final AgentService agentService;

    public ObjectRequestController() {
        this.objectService = new ObjectService();
        this.agentService = new AgentService();
    }

    @GET
    @Path("/realTime/top/ofType/{objType}")
    @Consumes(MediaType.APPLICATION_JSON)
    public CommonResultView<List<ProcessObject>> retrieveRealTimeTopByObjType(
            @PathParam("objType") @Valid @NotNull final String objType,
            @QueryParam("serverId") final int serverId) {
        final Server server = ServerManager.getInstance().getServerIfNullDefault(serverId);
        final int objHash = agentService.getObjHashFromAgentInfoByObjType(objType, server);

        if (objHash == 0) {
            return CommonResultView.fail(500, "Failed to get the 'ObjHash', Check your the 'ObjType'", null);
        }

        final List<ProcessObject> processObjects = this.objectService.retrieveRealTimeTopByObjType(objHash, server);

        return (CollectionUtils.isNotEmpty(processObjects))
                ? CommonResultView.success(processObjects)
                : CommonResultView.fail(500, "There was an error while doing 'top command'", null);
    }
}
