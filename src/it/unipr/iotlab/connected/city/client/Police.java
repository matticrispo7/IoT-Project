package it.unipr.iotlab.connected.city.client;

import java.io.UnsupportedEncodingException;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.Request;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.eclipse.californium.core.coap.CoAP.Code;

/**
 * @author Crispino Mattia - 320745
 */

public class Police {
	private static final int N = 10;				// #bridges
	
	public static void main(String[] args) {
		
		CoapClient p = new CoapClient();
		try {
			Thread.sleep(10);
			int totClients = 0;									// tot clients that crossed the bridge
			int totReds = 0;									// tot clients that crossed with RED
			for(int i = 0; i < N; i++) {						// loop through every resource
				String uri = "coap://127.0.0.1:5683/tf"+i+"_";
				for (int j = 1; j < 3; j++) {
					// send the request to the server or to every single tf
					p.setURI(uri+j);
					Request req = new Request(Code.GET);
					req.setPayload("POLICE");
					CoapResponse resp = p.advanced(req);
					System.out.println(Utils.prettyPrint(resp));
					// parsing the json received
					Object obj = new JSONParser().parse(new String(resp.getPayload(), "UTF-8"));
					// typecasting obj to JSONObject
			        JSONObject jo = (JSONObject) obj;
			        long counter = (long) jo.get("counter");
			        long red = (long) jo.get("red");
			        totClients += counter;
			        totReds += red;
			        System.out.println("counter: " + counter + " red: " + red);
			       
				}
								
			}
			System.out.println(totClients + " have crossed the bridges");
			System.out.println(totReds + " have crossed the bridges with red light");
			
		} catch (InterruptedException | ParseException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		
	}

}
