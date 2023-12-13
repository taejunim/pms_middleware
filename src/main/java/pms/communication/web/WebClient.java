package pms.communication.web;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

import static pms.system.PMSManager.applicationProperties;

public class WebClient {
    public static WebSocketClient webSocketClient;
    private static final String SERVER_URI = applicationProperties.getProperty("websocket.uri");
    public static final String MIDDLEWARE_ID = applicationProperties.getProperty("websocket.id");

    public String getMiddlewareId() {
        return MIDDLEWARE_ID;
    }

    public void execute() throws URISyntaxException {
        webSocketClient = new WebSocketClient(new URI(SERVER_URI)) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                new WebSender().sendConnect();
            }

            @Override
            public void onMessage(String message) {
                WebReceiver webReceiver = new WebReceiver();
                webReceiver.receive(message);
            }

            @Override
            public void onClose(int i, String s, boolean b) {

            }

            @Override
            public void onError(Exception e) {

            }
        };

        webSocketClient.connect();
    }
}
