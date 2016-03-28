package scouter.server.extif

import scouter.lang.pack.AlertPack
import scouter.server.Configure
import scouter.lang.AlertLevel
import com.google.gson.Gson
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client._
import org.apache.http.entity.StringEntity
import scouter.server.core.AgentManager
import scouter.server.Logger
import scouter.server.util.ThreadScala

case class Message(chat_id: String, text: String)

/**
  * singleton object to send a alert message using telegram.
  */
object TelegramMessage {
  
    val conf = Configure.getInstance()
  
    def send(alert: AlertPack) {
        val level = conf.send_alert_level
        
        if (level <= alert.level) {
            ThreadScala.start("scouter.server.extif.TelegramMessage") {
                val token = conf.telegram_bot_token
                val chat_id = conf.telegram_chat_id
                
                assert(token != null, "telegram_bot_token must not be null.")
                assert(chat_id != null, "telegram_chat_id must not be null.")
            
                val url = "https://api.telegram.org/bot" + token + "/sendMessage"
                
                var name = AgentManager.getAgentName(alert.objHash)
                if (name == null) {
                    name = "N/A"
                }
              
                val contents = "[TYPE] : " + alert.objType.toUpperCase() + "\n" + 
                               "[NAME] : " + name + "\n" + 
                               "[LEVEL] : " + AlertLevel.getName(alert.level) + "\n" +
                               "[TITLE] : " + alert.title + "\n" + 
                               "[MESSAGE] : " + alert.message
              
                val message = new Message(chat_id, contents)
                val param = new Gson().toJson(message)
      
                val post = new HttpPost(url)
                post.addHeader("Content-Type","application/json")
                post.setEntity(new StringEntity(param))
              
                val client: CloseableHttpClient = HttpClientBuilder.create().build()
              
                // send the post request
                Logger.println(client.execute(post));
            }
        }
    }
}