package scouterx.webapp.api.fw.exception.mapper;
/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

import scouterx.webapp.api.controller.CheckController;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 8. 25.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
	@Override
	public Response toResponse(Throwable ex) {
		CheckController.ToDo todo = new CheckController.ToDo("morning todo", "brew a coffee", new CheckController.Job("mytypes", "jobname!!"));
		return Response.status(500).entity(todo).type(MediaType.APPLICATION_JSON).build();

	}

//	private void setHttpStatus(Throwable ex, ErrorMessage errorMessage) {
//		if(ex instanceof WebApplicationException) {
//			errorMessage.setStatus(((WebApplicationException)ex).getResponse().getStatus());
//		} else {
//			errorMessage.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); //defaults to internal server error 500
//		}
//	}
}
