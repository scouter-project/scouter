package scouter.client.summary.modules;

public class SummaryData {
	int hash;
	int count;
	int errorCount;
	long elapsedSum;
	long cpu;
	long mem;

    public void addData(SummaryData another) {
        if (this.hash != another.hash) return;
        this.count += another.count;
        this.errorCount += another.errorCount;
        this.elapsedSum += another.elapsedSum;
        this.cpu += another.cpu;
        this.mem += another.mem;
    }
}
