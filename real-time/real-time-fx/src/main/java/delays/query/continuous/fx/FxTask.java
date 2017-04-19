package delays.query.continuous.fx;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

public class FxTask extends Task<Void> {

   static final String HTTP_HOST = System.getProperty("http.host", "real-time-vertx-myproject.127.0.0.1.xip.io");
   static final int HTTP_PORT = Integer.getInteger("http.port", 80);

   private ObservableList<StationBoardView> partialResults =
         FXCollections.observableArrayList();

   private Vertx vertx = Vertx.vertx();
   private HttpClient client = vertx.createHttpClient();

   private BlockingQueue<StationBoardView> queue = new ArrayBlockingQueue<>(128);

   public final ObservableList<StationBoardView> getPartialResults() {
      return partialResults;
   }

   @Override
   protected Void call() throws Exception {
      connectHttp();
      while (true) {
         if (isCancelled()) break;
         StationBoardView entry = queue.poll(1, TimeUnit.SECONDS);
         Thread.sleep(200);
         if (entry != null) {
            Platform.runLater(() ->
                  partialResults.add(entry));
         }
      }
      return null;
   }

   private void connectHttp() {
      client.websocket(HTTP_PORT, HTTP_HOST, "/eventbus/websocket", ws -> {
         System.out.println("Connected");
         sendPing(ws);

         // Send pings periodically to avoid the websocket connection being closed
         vertx.setPeriodic(5000, id -> {
            sendPing(ws);
         });

         // Register
         JsonObject msg = new JsonObject().put("type", "register").put("address", "delays");
         ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));

         ws.handler(buff -> {
            System.out.println(buff);
            JsonObject json = new JsonObject(new JsonObject(buff.toString()).getString("body"));
            queue.add(new StationBoardView(json.getString("type"),
                  json.getString("departure"),
                  json.getString("station"),
                  json.getString("destination"),
                  Integer.toString(json.getInteger("delay")),
                  json.getString("trainName")));
         });
      });
   }

   static void sendPing(WebSocket ws) {
      JsonObject msg = new JsonObject().put("type", "ping");
      ws.writeFrame(io.vertx.core.http.WebSocketFrame.textFrame(msg.encode(), true));
   }

   @Override
   protected void cancelled() {
      client.close();
      vertx.close();
   }

}
