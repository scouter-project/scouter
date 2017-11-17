package scouterx.webapp.request;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Hyanghee Jeon (gaiajeon@gmail.com) on 2017. 8. 27.
 */

@Getter
@Setter
@ToString
public class CondSearchXLogRequest {

    int serverId;
    
    @NotNull
    @PathParam("yyyymmdd")
    String yyyymmdd;
    
    @NotNull
    @Min(1)
    @QueryParam("startTimeMillis")
    long startTimeMillis;

    @NotNull
    @Min(1)
    @QueryParam("endTimeMillis")
    long endTimeMillis;
    
    @QueryParam("objHash")
    long objHash;    
    
    @QueryParam("service")
    String service ;

    @QueryParam("ip")
    String ip;
    
    @QueryParam("login")
    String login;
    
    @QueryParam("desc")
    String desc;
    
    @QueryParam("text1")
    String text1;

    @QueryParam("text2")
    String text2;
    
    @QueryParam("text3")
    String text3;
    
    @QueryParam("text4")
    String text4;
    
    @QueryParam("text5")
    String text5;
    
    
    public CondSearchXLogRequest(){
    	
    }
    
    public CondSearchXLogRequest(CondSearchXLogDataRequest dataRequest) throws ParseException {
    	this.serverId = dataRequest.getServerId();
        this.yyyymmdd = dataRequest.getYyyymmdd();
     
        this.service = dataRequest.getService();
        this.objHash = dataRequest.getObjHash();
        this.ip = dataRequest.getIp();
        this.login = dataRequest.getLogin();
        this.desc = dataRequest.getDesc();
        this.text1 = dataRequest.getText1();
        this.text2 = dataRequest.getText2();
        this.text3 = dataRequest.getText3();
        this.text4 = dataRequest.getText4();
        this.text5 = dataRequest.getText5();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

        this.startTimeMillis = sdf.parse(this.yyyymmdd + dataRequest.startHms).getTime();
        this.endTimeMillis = sdf.parse(this.yyyymmdd + dataRequest.endHms).getTime();
    }
        
}
