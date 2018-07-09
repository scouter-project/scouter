package scouter.agent.error;

public class TOO_MANY_COLLECTION_MEMBERS extends IllegalStateException {

	public TOO_MANY_COLLECTION_MEMBERS() {
	}

	public TOO_MANY_COLLECTION_MEMBERS(String reason) {
		super(reason);
	}
}
