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

package scouterx.webapp.api.exception;

import lombok.extern.slf4j.Slf4j;
import scouterx.webapp.api.fw.controller.ro.CommonResultView;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 8. 25.
 */
@Provider
@Slf4j
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
	@Context
	UriInfo uriInfo;

	@Override
	public Response toResponse(Throwable throwable) {
		if (throwable instanceof ErrorStateBizException) {
            return handleErrorStateBizException((ErrorStateBizException) throwable);

		} else if (throwable instanceof ErrorStateException) {
            return handlerErrorStateException((ErrorStateException) throwable);

		} else if(throwable instanceof WebApplicationException) {
            return handlerWebApplicationException((WebApplicationException) throwable);

		} else {
            return handleThrowable(throwable);
		}
	}

    private Response handleThrowable(Throwable throwable) {
        CommonResultView<?> commonResultView = CommonResultView.fail(500, 500, throwable.getMessage(), null);

        log.error("[WebApplicationException] {} - {}, [uri]{}", commonResultView.getStatus()
                , commonResultView.getMessage(), uriInfo.getPath(), throwable);

        return Response.status(commonResultView.getStatus()).entity(commonResultView).type(MediaType.APPLICATION_JSON).build();
    }

    private Response handlerWebApplicationException(WebApplicationException throwable) {
        WebApplicationException ex = throwable;
        CommonResultView<?> commonResultView = CommonResultView.fail(ex.getResponse().getStatus()
                , ex.getResponse().getStatus(), ex.getMessage(), null);


        log.error("[WebApplicationException] {} - {}, [uri]{}", commonResultView.getStatus()
                , commonResultView.getMessage(), uriInfo.getPath());

        return Response.status(commonResultView.getStatus()).entity(commonResultView).type(MediaType.APPLICATION_JSON).build();
    }

    private Response handlerErrorStateException(ErrorStateException throwable) {
        ErrorStateException ex = throwable;
        ErrorState errorState = ex.getErrorState();
        CommonResultView<?> commonResultView = CommonResultView.fail(errorState.getStatus().getStatusCode()
                , errorState.getErrorCode(), errorState.getErrorMessage(), null);

        StackTraceElement lastStack = Arrays.stream(ex.getStackTrace()).findFirst().orElse(null);
        log.error("[ErrorStateException] {} - {} - {}, [uri]{} at {}", commonResultView.getStatus(),
                commonResultView.getMessage(), ex.getMessage(), uriInfo.getPath(), lastStack, ex);

        return Response.status(commonResultView.getStatus()).entity(commonResultView).type(MediaType.APPLICATION_JSON).build();
    }

    private Response handleErrorStateBizException(ErrorStateBizException throwable) {
        ErrorStateBizException ex = throwable;
        ErrorState errorState = ex.getErrorState();
        CommonResultView<?> commonResultView = CommonResultView.fail(errorState.getStatus().getStatusCode()
                , errorState.getErrorCode(), errorState.getErrorMessage(), null);

        StackTraceElement lastStack = Arrays.stream(ex.getStackTrace()).findFirst().orElse(null);
        log.error("[ErrorStateBizException] {} - {} - {}, [uri]{} at {}", commonResultView.getStatus()
                , commonResultView.getMessage(), ex.getMessage(), uriInfo.getPath(), lastStack);

        return Response.status(commonResultView.getStatus()).entity(commonResultView).type(MediaType.APPLICATION_JSON).build();
    }
}
