package scouterx.webapp.api.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import scouterx.webapp.api.fw.controller.ro.CommonResultView;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by gunlee on 2017. 8. 24.
 */
@Path("/check")
@Produces(MediaType.APPLICATION_JSON)
public class CheckController {

	@GET
	public String check() {
		return "OK";
	}

	@GET @Path("/more")
	public String checkMore() {
		return "OK-More";
	}

	@GET @Path("/todo")
	public ToDo checkTodo() {
		return new ToDo("morning todo", "brew a coffee", new Job("mytypes", "jobname!!"));
	}

	@GET @Path("/todoAsResult")
	public CommonResultView<ToDo> checkTodoAsResult() {
		ToDo todo = new ToDo("morning todo", "brew a coffee", new Job("mytypes", "jobname!!"));
		CommonResultView<ToDo> resultView = CommonResultView.success(todo);
		return resultView;
	}

	@GET @Path("/exception")
	public CommonResultView<ToDo> exception() {
		if (true) {
			throw new RuntimeException("my exception");
		}
		return null;
	}



	@Setter
	@Getter
	@AllArgsConstructor
	public static class ToDo {
		String title;
		String desc;
		Job job2;
	}

	@Setter
	@Getter
	@AllArgsConstructor
	public static class Job {
		String type;
		String name;
	}
}
