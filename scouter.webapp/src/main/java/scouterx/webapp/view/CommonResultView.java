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

package scouterx.webapp.view;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.MDC;
import scouterx.webapp.framework.filter.LoggingInitServletFilter;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * Created by gunlee on 2017. 8. 25.
 */
@XmlRootElement
@Getter
@Setter
public class CommonResultView<T> {
	private static final int SUCCESS = 0;
	private static final ObjectMapper objectMapper = new ObjectMapper().configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
	
	private int status = HttpStatus.OK_200;
	private String requestId;
	private int resultCode;
	private String message;
	private T result;

	public CommonResultView() {}

	public CommonResultView(int resultCode, String message, T result) {
		this.resultCode = resultCode;
		this.message = message;
		this.result = result;
		this.requestId = MDC.get(LoggingInitServletFilter.requestId);
	}

	public CommonResultView(int status, int resultCode, String message, T result) {
		this.status = status;
		this.resultCode = resultCode;
		this.message = message;
		this.result = result;
		this.requestId = MDC.get(LoggingInitServletFilter.requestId);
	}

	public static CommonResultView<Boolean> success() {
		return new CommonResultView<>(SUCCESS, "success", true);
	}

	public static <T> CommonResultView<T> success(T result) {
		return new CommonResultView<>(SUCCESS, "success", result);
	}

	public static <T> CommonResultView fail(int resultCode, String message, T result) {
		return new CommonResultView<>(HttpStatus.INTERNAL_SERVER_ERROR_500, resultCode, message, result);
	}

	public static <T> CommonResultView fail(int status, int resultCode, String message, T result) {
		return new CommonResultView<>(status, resultCode, message, result);
	}

	public static void jsonArrayStream(OutputStream os, Consumer<JsonGenerator> itemGenerator) throws IOException {
		JsonGenerator jg = objectMapper.getFactory().createGenerator(os);

		jg.writeStartObject();

		{
			jg.writeNumberField("status", HttpStatus.OK_200);
			jg.writeNumberField("resultCode", SUCCESS);
			jg.writeStringField("message", "");

			jg.writeArrayFieldStart("result");

			{
				itemGenerator.accept(jg);
			}

			jg.writeEndArray();
		}

		jg.writeEndObject();
		jg.flush();
		jg.close();
	}

	public static void jsonStream(OutputStream os, Consumer<JsonGenerator> jsonItemgenerator) throws IOException {
		JsonGenerator jg = objectMapper.getFactory().createGenerator(os);

		jg.writeStartObject();

		{
			jg.writeNumberField("status", HttpStatus.OK_200);
			jg.writeNumberField("resultCode", SUCCESS);
			jg.writeStringField("message", "");

			jg.writeObjectFieldStart("result");

			{
				jsonItemgenerator.accept(jg);
			}

			jg.writeEndObject();
		}

		jg.writeEndObject();
		jg.flush();
		jg.close();
	}
}
