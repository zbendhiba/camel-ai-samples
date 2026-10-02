package org.acme.ward;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;

/**
 * The demo page's live view connects here and receives every ingested summary as JSON,
 * pushed by {@link WardLivePublisher} from the hospital-feed route.
 */
@WebSocket(path = "/ws/ward", endpointId = WardLiveSocket.ENDPOINT_ID)
public class WardLiveSocket {

    public static final String ENDPOINT_ID = "ward-live";

    @OnOpen
    public void onOpen() {
        // nothing to do: the connection just waits for broadcasts
    }
}
