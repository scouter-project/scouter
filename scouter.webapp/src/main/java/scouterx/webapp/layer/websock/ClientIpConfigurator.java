package scouterx.webapp.layer.websock;

import java.util.List;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class ClientIpConfigurator extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        // Scouter 내장 Jetty 환경 등에 따라 헤더 이름("X-Forwarded-For" 등)이나 추출 방식이 다를 수 있습니다.
        List<String> forwardedFor = request.getHeaders().get("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            config.getUserProperties().put("client-ip", forwardedFor.get(0));
        }
    }
}
