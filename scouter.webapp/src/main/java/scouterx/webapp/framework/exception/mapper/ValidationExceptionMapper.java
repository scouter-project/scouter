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

package scouterx.webapp.framework.exception.mapper;

import lombok.extern.slf4j.Slf4j;
import scouterx.webapp.view.CommonResultView;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 8. 25.
 */
@Provider
@Slf4j
public class ValidationExceptionMapper implements ExceptionMapper<javax.validation.ValidationException> {
	@Context
	UriInfo uriInfo;

	@Override
	public Response toResponse(javax.validation.ValidationException e) {
        final StringBuilder strBuilder = new StringBuilder("[ValidationException] ");
        for (ConstraintViolation<?> cv : ((ConstraintViolationException) e).getConstraintViolations()) {
            strBuilder.append(cv.getPropertyPath().toString() + " " + cv.getMessage());
        }
        CommonResultView resultView = CommonResultView.fail(400, 400, strBuilder.toString(), null);

        return Response.status(resultView.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(resultView).build();
    }
}
