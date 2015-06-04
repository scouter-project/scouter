package scouter.util;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeEnumer {
	final int max;
	int current;
	private NodeList nodeList;

	public NodeEnumer(NodeList nodeList) {
		this.max = nodeList == null ? 0 : nodeList.getLength();
		this.current = 0;
		this.nodeList = nodeList;
	}

	public boolean hasNext() {
		return current < max;
	}

	public Node next() {
		return nodeList.item(current++);
	}
}
