package it.unipr.iotlab.connected.city.client;

import java.io.UnsupportedEncodingException;

import java.util.Iterator;
import java.util.Random;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import it.unipr.iotlab.connected.city.server.BridgeServer;

/**
 * @author Crispino Mattia - 320745
 */

public class VehicleThread implements Runnable {
	
	private static final int RED = 1;				// value used to compare with random int to check if the client cross the bridge with RED light
	private static final int UB = 10;				// upper bound while extracting the random int to check if a vehicle cross the bridge with RED light
	private String URI;
	private int clientID;
	private Boolean execFinished;					// flag used to check if this client has finished its execution
	private long timeStart;							// time when the client starts
	
	public VehicleThread(String uri, int id) {
		this.URI = uri;
		this.clientID = id;
		this.execFinished = false;
	}

	@Override
	public void run() {
		this.setTimeStart(System.currentTimeMillis());
		// 1st registration to the resource
		CoapClient c = new CoapClient(this.getURI());
		Request request = buildRequestPOST();
		// Synchronously send the message
		CoapResponse coapResp = c.advanced(request);
		System.out.println();
		System.out.println(Utils.prettyPrint(coapResp));		
		
		CoapResponse response = coapResp;
		// if the client needs to be redirected then it will start a new registration sending a POST request to the new resource with this client's ID
		try {
			while(true)
			{
				Object obj = new JSONParser().parse(response.getResponseText());
				JSONObject jo = (JSONObject) obj;
				if(jo.containsKey("redirect")) {
					System.out.println("CLIENT " + this.getID() + " is redirected to " + jo.get("redirect") + " ==> newURI: " + BridgeServer.buildURI(jo.get("redirect").toString()));
					// "build" the URI from the server's method and change it				
					this.setURI(BridgeServer.buildURI(jo.get("redirect").toString()));
					c = new CoapClient(BridgeServer.buildURI(jo.get("redirect").toString()));
					Request req = buildRequestPOST();
					CoapResponse resp = c.advanced(req);
					System.out.println();
					System.out.println(Utils.prettyPrint(resp));	
					response = resp;
				} else {
					break;
				}
			} 
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		// create a relation with the resource to observe
		CoapObserveRelation relation = c.observe(new CoapHandler() {
			
			@Override
			public void onLoad(CoapResponse response) {
				System.out.println();
				System.out.println(Utils.prettyPrint(response));
				
				try {
					// parsing the json received
					Object json_obj = new JSONParser().parse(new String(response.getPayload(), "UTF-8"));
					// typecasting obj to JSONObject
			        JSONObject jo = (JSONObject) json_obj;
			        // check if the response contains the list of IDs (in this case the light is GREEN and this client may be able to cross the street)
			        if(jo.containsKey("id")) {
			        	// check if there's this client's ID in the array received 
						JSONArray listID = (JSONArray) jo.get("id");
						Iterator<String> iterator = listID.iterator();
			            while (iterator.hasNext()) {
			            	String id = iterator.next();
			                //System.out.println(" ID: " + id);
			                if(id.equals(Integer.toString(getID()))) {
								Request del = new Request(Code.DELETE);
								del.setConfirmable(true);
								// PAYLOAD
								JSONObject obj = new JSONObject();
								obj.put("green", Integer.toString(getID()));
								del.setPayload(obj.toString().getBytes());
								del.setURI(getURI());
								del.send();	
								setExecFinished(true);
								break;
							} 
			            }
					} else {
						// the light is RED so check if this client cross the bridge
						System.out.println("[Client " + getID() + "] Received: " + response.getResponseText());
						// extract a random number and if it = to a given value so this client cross the bridge even if the light is RED
						Random r = new Random();
						if(r.nextInt(UB) == RED) {
							System.out.println("[Client " + getID() + "] Cross the bridge with RED light. Sending a DELETE request to the server");
							Request del = new Request(Code.DELETE);
							del.setConfirmable(true);
							jo.put("red", Integer.toString(getID()));
							del.setPayload(jo.toString().getBytes());
							del.setURI(getURI());
							del.send();
							setExecFinished(true);
						}
					}
				} catch (UnsupportedEncodingException | ParseException e) {
					e.printStackTrace();
				} 
				
				
			}
			
			@Override
			public void onError() {
				System.err.println("Error from client " + getID() + " while observing the resource");
				
			}
		});
		
		while(!this.getExecFinished()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// cancel the observe relation and yield
		relation.proactiveCancel();
		Thread.yield();

	}

	public String getURI() {
		return this.URI;
	}
	
	public int getID() {
		return this.clientID;
	}
	
	public void setURI(String uri) {
		this.URI = uri;
	}
	
	public void setTimeStart(long start) {
		this.timeStart = start;
	}
	
	public long getTimeStart() {
		return this.timeStart;
	}
	
	public void setExecFinished(Boolean e) {
		this.execFinished = e;
	}
	
	public Boolean getExecFinished() {
		return this.execFinished;
	}
	
	
	private Request buildRequestPOST() {
		Request request = new Request(Code.POST);
		request.setConfirmable(true);
		JSONObject j = new JSONObject();
		j.put("id", Integer.toString(this.getID()));
		request.setPayload(j.toString().getBytes());
		return request;
	}
}
