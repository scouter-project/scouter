package scouterx.webapp.api.fw.controller.ro;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jetty.http.HttpStatus;

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
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private int status = HttpStatus.OK_200;
	private int resultCode;
	private String message;
	private T result;

	public CommonResultView(int resultCode, String message, T result) {
		this.resultCode = resultCode;
		this.message = message;
		this.result = result;
	}

	public CommonResultView(int status, int resultCode, String message, T result) {
		this.status = status;
		this.resultCode = resultCode;
		this.message = message;
		this.result = result;
	}

	public static CommonResultView success() {
		return new CommonResultView(SUCCESS, "success", true);
	}

	public static CommonResultView success(Object result) {
		return new CommonResultView(SUCCESS, "success", result);
	}

	public static CommonResultView fail(int resultCode, String message, Object result) {
		return new CommonResultView(HttpStatus.INTERNAL_SERVER_ERROR_500, resultCode, message, result);
	}

	public static CommonResultView fail(int status, int resultCode, String message, Object result) {
		return new CommonResultView(status, resultCode, message, result);
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

	public static void jsonStream(OutputStream os, Consumer<JsonGenerator> itemGenerator) throws IOException {
		JsonGenerator jg = objectMapper.getFactory().createGenerator(os);

		jg.writeStartObject();

		{
			jg.writeNumberField("status", HttpStatus.OK_200);
			jg.writeNumberField("resultCode", SUCCESS);
			jg.writeStringField("message", "");

			jg.writeObjectFieldStart("result");

			{
				itemGenerator.accept(jg);
			}

			jg.writeEndObject();
		}

		jg.writeEndObject();
		jg.flush();
		jg.close();
	}
}
