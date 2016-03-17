package scouter.server.extif

import scouter.server.Configure
import scouter.lang.pack.AlertPack
import scouter.server.util.ThreadScala
import scouter.server.core.AgentManager
import scouter.lang.AlertLevel
import org.apache.commons.mail._
import scouter.server.Logger

/**
  * singleton object to send a alert message using mail.
  */
object EmailMessage {
  
    val conf = Configure.getInstance()
  
    def send(alert: AlertPack) {
        val level = conf.send_alert_level
        
        if (level <= alert.level) {
            ThreadScala.start("scouter.server.extif.MailMessage") {
                val hostname = conf.email_smtp_hostname
                val port = conf.email_smtp_port
                val username = conf.email_username
                val password = conf.email_password
                val tlsEnabled = conf.email_tls_enabled
                val from = conf.email_from_address
                val to = conf.email_to_address
                val cc = conf.email_cc_address
                
                assert(hostname != null, "email_smtp_hostname must not be null.")
                assert(port > 0, "email_smtp_port must be grather than 0.")
                assert(username != null, "email_username must not be null.")
                assert(password != null, "email_password must not be null.")
                assert(from != null, "email_from_address must not be null.")
                assert(to != null, "email_to_address must not be null.")
                
                var name = AgentManager.getAgentName(alert.objHash)
                if (name == null) {
                    name = "N/A"
                }
              
                val subject = "[" + AlertLevel.getName(alert.level) + "] " + alert.objType.toUpperCase() + 
                              "(" + name + ") : " + alert.title

                val message = "[TYPE] : " + alert.objType.toUpperCase() + "\n" + 
                              "[NAME] : " + name + "\n" + 
                              "[LEVEL] : " + AlertLevel.getName(alert.level) + "\n" +
                              "[TITLE] : " + alert.title + "\n" + 
                              "[MESSAGE] : " + alert.message
                              
                val email = new SimpleEmail()
                
                email.setHostName(hostname)
                email.setSmtpPort(port)
                email.setAuthenticator(new DefaultAuthenticator(username, password))
                email.setStartTLSEnabled(tlsEnabled)
                email.setFrom(from)
                email.setSubject(subject)
                email.setMsg(message)
                
                to.split(",").foreach(email.addTo(_)) 
                
                println("to : " + to)
                
                if (cc != null) {
                    cc.split(",").foreach(email.addCc(_)) 
                }
                
                // send the email
                email.send()
                
                Logger.println("Email sent to [" + to + "] successfully.")
            }
        }
    }
}