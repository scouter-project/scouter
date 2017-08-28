package scouterx.webapp.api.fw.controller.ro;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jetty.http.HttpStatus;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by gunlee on 2017. 8. 25.
 */
@XmlRootElement
@Getter
@Setter
public class CommonResultView<T> {
	private static final int SUCCESS = 0;

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
}
