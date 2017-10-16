package scouterx.webapp.layer.controller;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import scouterx.webapp.framework.client.net.INetReader;
import scouterx.webapp.layer.service.XLogSearchService;
import scouterx.webapp.request.CondSearchXLogRequest;

@Path("/v1/xlogsearch")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class XLogCondSearchController {
	
    @Context
    HttpServletRequest servletRequest;

    private final XLogSearchService searchService;
    

    public XLogCondSearchController() {
        this.searchService = new XLogSearchService();
    }
    
    
    @GET
    @Path("/search/{condition}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void handleRealTimeXLog(final CondSearchXLogRequest condXLogRequest, final INetReader reader) {
    	searchService.handleCondSearchXLog(condXLogRequest, reader);
    }

}
