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

package scouterx.webapp.layer.controller;

import io.swagger.annotations.Api;
import scouter.lang.pack.TextPack;
import scouterx.webapp.layer.service.DictionaryService;
import scouterx.webapp.request.DictionaryRequest;
import scouterx.webapp.view.CommonResultView;

import javax.inject.Singleton;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 29.
 */
@Path("/v1/dictionary")
@Api("Dictionary")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class DictionaryController {
	private final DictionaryService dictionaryService;

	public DictionaryController() {
		this.dictionaryService = new DictionaryService();
	}

	/**
	 * get text values from dictionary keys requested
	 * uri : /dictionary/{yyyymmdd}?dictKeys=[service:10001,service:10002,obj:20001,sql:55555] (bracket is optional)
	 *
	 * @param dictionaryRequest @see {@link DictionaryRequest}
	 * @return
	 */
	@GET
	@Path("/{yyyymmdd}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response retrieveTextFromDictionary(@Valid @BeanParam DictionaryRequest dictionaryRequest) {
		StreamingOutput stream = os -> {
			CommonResultView.jsonArrayStream(os, jsonGenerator -> {
				dictionaryService.retrieveTextFromDictionary(dictionaryRequest, in -> {
					TextPack textPack = (TextPack) in.readPack();
					jsonGenerator.writeStartObject();
					jsonGenerator.writeStringField("textType", textPack.xtype);
					jsonGenerator.writeNumberField("dictKey", textPack.hash);
					jsonGenerator.writeStringField("text", textPack.text);
					jsonGenerator.writeEndObject();
				});
			});
		};

		return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
	}
}
