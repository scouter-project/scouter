package scouter.agent.trace;

public class ErrorEntity {

    final private Throwable th;
    final private int message;
    final private int sql;
    final private int api;

    public static ErrorEntity of (Throwable th, int message, int sql, int api) {
        return new ErrorEntity(th, message, sql, api);
    }

    private ErrorEntity(Throwable th, int message, int sql, int api) {
        this.th = th;
        this.message = message;
        this.sql = sql;
        this.api = api;
    }

    public Throwable getThrowable() {
        return th;
    }

    public int getMessage() {
        return message;
    }

    public int getSql() {
        return sql;
    }

    public int getApi() {
        return api;
    }
}
