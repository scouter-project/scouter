package scouter.agent.trace;

import scouter.lang.step.StepSingle;

public class ConcurrentProfileCollector implements IProfileCollector {

	private IProfileCollector inner;

	public ConcurrentProfileCollector(IProfileCollector inner) {
		this.inner = inner;
	}

	public synchronized void add(StepSingle step) {
		this.inner.add(step);
	}

	public synchronized void push(StepSingle step) {
		this.inner.push(step);
	}

	public synchronized void pop(StepSingle step) {
		this.inner.pop(step);
	}

	public synchronized void close(boolean ok) {
		this.inner.close(ok);
	}

}
