package it.unipr.iotlab.connected.city.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Crispino Mattia - 320745
 */

public class TrafficLight extends CoapResource {
	
	private static final int MAX_VEHICLE = 3;							// max number of vehicle that are able to cross the bridge when the light is GREEN
	private static final int THRESHOLD = 2*MAX_VEHICLE;					// value used for comparison to redirect a client into a different queue
	private int lightStatus;											// 1 = green light    0 = red light
	private int nClientExtracted;		
	private int clientRED;												// #clients that crossed the bridge with RED light
	private int clientCounter;											// #clients that crossed the bridge
	private ArrayList<String> clientRegistered;
	private static ConcurrentHashMap<String, Integer> tfQueue  = new ConcurrentHashMap<String, Integer>();		// hashmap to log all the clients in the queue of every traffic light
	private static ConcurrentHashMap<String, Integer> tfStatus = new ConcurrentHashMap<String, Integer>();		// hashmap to log the status of every traffic light
	
	public TrafficLight(String name, int status) {
		super(name);
		this.lightStatus = status;
		this.clientRegistered = new ArrayList<>();
		this.clientRED = 0;
		this.clientCounter = 0;
		this.nClientExtracted = MAX_VEHICLE;							// initially set to the max value so that it can change over time
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleGET(CoapExchange exchange) {
		/*
		 * Accept the request, get the payload and check the client (police, vehicle or citizen).
		 * If the client is the POLICE  => sent back a json payload with the number of client that crossed with RED (in this resource) and the total number of client that crossed 
		 * If the client is the CITIZE  => sent back a json payload with the status of the resource and the number of client in its queue
		 * if the client is the VEHICLE => check the light status, this resource's queue and sent back the json payload with either the #client extracted (if the light is GREEN) or
		 * 								   the RED status
		 * 
		 * The resource's queue is managed with a FIFO policy
		 */
		JSONObject j = new JSONObject();
		switch (exchange.getRequestText()) {
		case "POLICE":
			System.out.println("[Resource " + getName() + "] GET from POLICE");
			j.put("counter", getCounter());
			j.put("red", getClientRED());
			exchange.respond(ResponseCode.CONTENT, j.toString().getBytes(), MediaTypeRegistry.APPLICATION_JSON);
			break;
		case "CITIZEN":
			exchange.respond(ResponseCode.CONTENT, "status:"+Integer.toString(getLightStatus()), MediaTypeRegistry.TEXT_PLAIN);
			System.out.println("[Resource " + this.getName() + "] --> #clientQueue: " + tfQueue.get(getName()));
			break;
		default:
			if(this.getLightStatus() == 1)	// light is GREEN
			{	
				// build the payload and select N random clients that are able to cross the bridge				
				JSONArray listID = new JSONArray();
				System.out.println("[Resource " + this.getName() + "] Light is GREEN. Extract max " + this.getMaxVehicle() + " clients");
				// check if there are some clients in the queue
				if(this.clientRegistered.size() > 0) {
					if(this.clientRegistered.size() < this.nClientExtracted) {
						// extract all the client in the queue
						for (int i = 0; i < this.clientRegistered.size(); i++) {
							String id = this.clientRegistered.get(i);
							listID.add(id);
						}
					} else {
						// extract only the FIRST getMaxVehicle clients (FIFO)
						for (int i = 0; i < this.getMaxVehicle(); i++) {
							String id = this.clientRegistered.get(i);
							listID.add(id);
						}
					}
					j.put("id", listID);
					// sent back the payload
					exchange.respond(ResponseCode.VALID,
							j.toString().getBytes(),
				           MediaTypeRegistry.APPLICATION_JSON);
				}
				// log
				System.out.println("[Resource " + this.getName() + "] --> Extracted client " + j.toString());
			}
			else {
				System.out.println("[Resource " + this.getName() + "] Light is RED");
				j.put("status", this.getLightStatus());
				exchange.respond(ResponseCode.VALID,
						j.toString().getBytes(),
			           MediaTypeRegistry.APPLICATION_JSON);
			}
			break;
		}		
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) { // [REQUEST FROM THE SERVER]
		/* Get the client's ID from the payload by accessing the "id" field in the json.
		 * Then, check the length of the queue and if it's > THRESHOLD then try to redirect the clien to another 
		 * resource otherwise keep the client in this queue.
		 */
		
		try {
			Object obj = new JSONParser().parse(exchange.getRequestText());
			JSONObject jo = (JSONObject) obj;
			String clientID = (String) jo.get("id");
			
			System.out.println("[Resource " + this.getName() + "] POST from " + clientID);
			//logQueue();
			JSONObject response = new JSONObject();
			
			// check if the client needs to be redirected
			if(tfQueue.get(this.getName()) > THRESHOLD) {
				String resName = redirectClient(clientID);
				// check if a resource has been found (if the name != "")
				if(!resName.equals("")) {
					System.out.println("[Resource " + this.getName() + "] Redirect client " + clientID + " to resource " + resName);
					response.put("redirect", resName);
				} else {
					response.put("ID_saved", true);
					// if no resource has been found keep the client in this queue
					tfQueue.put(getName(), tfQueue.get(getName())+1);
					this.clientRegistered.add(clientID);
				}
			} else {
				// update the map
				tfQueue.put(getName(), tfQueue.get(getName())+1);
				this.clientRegistered.add(clientID);
				System.out.println("*** Client " + clientID + " registered in the queue of " + this.getName());
				response.put("ID_saved", true);
				
			}
			
			exchange.respond(ResponseCode.CREATED,
					response.toString().getBytes(),
			         MediaTypeRegistry.APPLICATION_JSON);
			
			
			
		} catch (ParseException e) {
			
			e.printStackTrace();
		}
		
			
		
	}
	
	@Override
	public void handlePUT(CoapExchange exchange) { // [REQUEST FROM THE SERVER]
		/*
		 * Since both maps "tfQueue" and "tfStatus" will have the same length, the inizialiation can be done
		 * checking only if a map is null and then setting an initial value for both the maps
		 */
		if(tfQueue.get(this.getName()) == null) {
			tfQueue.put(this.getName(), 0);
			tfStatus.put(this.getName(), 0);
		}
				
		this.setLightStatus(Integer.valueOf(exchange.getRequestText()));
		
		/* every time the server updates the resource's state, a random value N in [0, this.MAX_VEHICLE+1] is extracted  
		 * and only N clients are then able to cross the bridge (this is done in order to simulate e real case scenario in 
		 * which every vehicle can take different time to cross the bridge )
		 */
		Random r = new Random();
		this.setMaxVehicle(r.nextInt(MAX_VEHICLE) + 1);  // at least one client is selected

		// update the status in the map
		tfStatus.put(this.getName(), this.getLightStatus());
		
		exchange.respond(ResponseCode.VALID, "ok", MediaTypeRegistry.TEXT_PLAIN);
		
		// notify all the clients registered on this resource that it is changed
		this.changed();
	}
	
	@Override
	public void handleDELETE(CoapExchange exchange) {
		/**
		 * When a client has crossed the bridge it sends a DELETE request to the resource so,
		 * after receiving this request, the resource check if the client has crossed with RED 
		 * or GREEN light and then it removes that client's ID from its queue
		 */
		try {
			Object obj = new JSONParser().parse(exchange.getRequestText());
	        JSONObject jo = (JSONObject) obj;
			if(jo.containsKey("green")) {
				// client crossed with GREEN light. Delete the clientID from the list
				this.clientRegistered.remove(jo.get("green"));
				System.out.println("[Resource " + this.getName() +"] Client " + jo.get("green") + " crossed the bridge with GREEN light.");
			} else if(jo.containsKey("red")) {
				System.out.println("[Resource " + this.getName() +"] Client " + jo.get("red") + " crossed the bridge with RED light.");
				// remove the client's ID from the list
				this.clientRegistered.remove(jo.get("red"));
				this.clientRED += 1;
				
			}
			if(tfQueue.get(getName()) != null) {
				tfQueue.put(getName(), tfQueue.get(getName())-1);
			}
			this.clientCounter += 1;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public void setLightStatus(int s) {
		this.lightStatus = s;
	}
	
	public int getLightStatus() {
		return this.lightStatus;
	}
	
	public void setMaxVehicle(int n) {
		this.nClientExtracted = n;
	}
	
	public int getMaxVehicle() {
		return this.nClientExtracted;
	}
	
	
	public int getClientRED() {
		return clientRED;
	}

	
	public int getCounter() {
		return clientCounter;
	}

	public void logID() {
		String s = "";
		for (String id : clientRegistered) {
			s += id + " ";
		}
		System.out.println("[Resource " + this.getName() + "] clients registered: " + s);
	}
	
	private void logQueue() {
		Iterator<Map.Entry<String, Integer>> iterator = tfQueue.entrySet().iterator();
	    while (iterator.hasNext()) {
	        Map.Entry<String, Integer> entry = iterator.next();
	        System.out.println("Resource " + entry.getKey() + " has " + entry.getValue() + " clients in queue"); 
	    }
		
	}
	
	/** [ADDITIONAL FEAUTURE]
	 * Check if the client needs to be redirected to another resource
	 * @param id		client's ID
	 * @return name 	resource's name to be redirected (if it is "" then no resources were found)
	 */
	private String redirectClient(String id) {
		// find the resource with min client in its queue
		int min = Integer.MAX_VALUE;
		String name = "";
		for (Map.Entry<String, Integer> entry : tfQueue.entrySet()) {
			if(!entry.getKey().equals(this.getName()) && entry.getValue() < (THRESHOLD/2)) {
				min = (entry.getValue() < min) ? entry.getValue() : min;
				// save resource's name
				name = entry.getKey();				
			}
		}
		
		return name;
		
	}

	


}
