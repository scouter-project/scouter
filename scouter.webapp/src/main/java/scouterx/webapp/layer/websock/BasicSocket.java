package scouterx.webapp.layer.websock;

import lombok.extern.slf4j.Slf4j;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/basic")
@Slf4j
public class BasicSocket
{
    private Session session;
    private RemoteEndpoint.Async remote;

    @OnClose
    public void onWebSocketClose(CloseReason close)
    {
        this.session = null;
        this.remote = null;
        log.info("WebSocket Close: {} - {}",close.getCloseCode(),close.getReasonPhrase());
    }

    @OnOpen
    public void onWebSocketOpen(Session session)
    {
        this.session = session;
        this.remote = this.session.getAsyncRemote();
        log.info("WebSocket Connect: {}",session);
        this.remote.sendText("You are now connected to " + this.getClass().getName());
    }

    @OnError
    public void onWebSocketError(Throwable cause)
    {
        log.warn("WebSocket Error",cause);
    }

    @OnMessage
    public String onWebSocketText(String message)
    {
        log.info("Echoing back text message [{}]",message);
        // Using shortcut approach to sending messages.
        // You could use a void method and use remote.sendText()
        return message;
    }
}
