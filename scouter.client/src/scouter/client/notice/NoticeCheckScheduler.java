package scouter.client.notice;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import scouter.client.util.ExUtil;
import scouter.util.DateUtil;

import java.io.IOException;
import java.util.*;

public enum NoticeCheckScheduler {

    INSTANCE;

    private final String NOTICE_URL = "http://notice.scouterapm.com:6181/latest-notice";
    String scch = UUID.randomUUID().toString();

    public void initialize() {
        ExUtil.asyncRun(() -> {
            process();
            registerTimer();
        });
    }

    void registerTimer() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);
        Date checkTime = calendar.getTime();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                process();
            }
        }, checkTime, DateUtil.MILLIS_PER_DAY);
    }

    private void process() {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(NOTICE_URL);
        httpGet.addHeader("X-SCCH", scch);
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpGet);
            System.out.println("Response Code : "
                    + response.getStatusLine().getStatusCode());
            String tag = response.getFirstHeader("X-Scouter-ETag").getValue();

            // TODO compare tag
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity, "UTF-8");
            System.out.println(html);
            //

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
