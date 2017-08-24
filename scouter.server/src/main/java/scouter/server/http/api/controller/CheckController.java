package scouter.server.http.api.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by gunlee on 2017. 8. 24.
 */
@Path("/check")
public class CheckController {
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String check() {
		return "OK";
	}
}
