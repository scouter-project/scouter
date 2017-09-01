/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouterx.webapp.api.controller;

import scouterx.client.server.ServerManager;
import scouterx.webapp.api.exception.ErrorState;
import scouterx.webapp.api.fw.controller.ro.CommonResultView;
import scouterx.webapp.api.requestmodel.PageableXLogRequest;
import scouterx.webapp.api.requestmodel.XLogByTokenRequest;
import scouterx.webapp.api.service.XLogService;
import scouterx.webapp.api.viewmodel.PageableXLogView;
import scouterx.webapp.api.viewmodel.RealTimeXLogView;
import scouterx.webapp.util.ZZ;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Path("/v1/xlog")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class XLogController {
	private final XLogService xLogService;

	public XLogController() {
		this.xLogService = new XLogService();
	}

	/**
	 * get values of several counters for given an object
	 * uri : /xlog/realTime/0/100?objHashes=10001,10002 or ?objHashes=[10001,100002]
	 *
	 * @param objHashByCommaSeparator
	 * @param serverId
	 */
	@GET
	@Path("/realTime/{xLogLoop}/{xLogIndex}")
	@Consumes(MediaType.APPLICATION_JSON)
	public CommonResultView<RealTimeXLogView> retrieveRealTimeXLog(
			@QueryParam("objHashes") final String objHashByCommaSeparator,
			@PathParam("xLogLoop") final int xLogLoop,
			@PathParam("xLogIndex") final int xLogIndex,
			@QueryParam("serverId") final int serverId) {

		Object o = ZZ.<Integer>splitParam(objHashByCommaSeparator);

		RealTimeXLogView xLogView = xLogService.retrieveRealTimeXLog(
				ServerManager.getInstance().getServer(serverId),
				ZZ.splitParamAsInteger(objHashByCommaSeparator),
				xLogIndex,
				xLogLoop);

		return CommonResultView.success(xLogView);
	}

	/**
	 * request xlog token for range request.
	 * uri : /xlog/{date}?startTime=... @see {@link PageableXLogRequest}
	 *
	 * @param xLogRequest
	 * @return PageableXLogView @see {@link PageableXLogView}
	 */
	@GET
	@Path("/{date}")
	@Consumes(MediaType.APPLICATION_JSON)
	public CommonResultView<PageableXLogView> retrievePageableXLog(@Valid @BeanParam PageableXLogRequest xLogRequest) {

		PageableXLogView view = xLogService.retrievePageableXLog(xLogRequest);
		return CommonResultView.success(view);
	}

	/**
	 * request xlog by token
	 * uri : /xlog/token/{requestToken}?pageCount=3000
	 * @param xLogByTokenRequest
	 * @return ?
	 * @see XLogByTokenRequest
	 */
	@GET
	@Path("/token/{requestToken}")
	@Consumes(MediaType.APPLICATION_JSON)
	public CommonResultView<String> requestXLogsByToken(@Valid @BeanParam XLogByTokenRequest xLogByTokenRequest) {
		//hasnext
//		String requestToken = xLogService.requestXLogToken(xLogRequest)
//		return CommonResultView.success(requestToken);
		ErrorState.throwNotImplementedException();
		return null;
	}

}
