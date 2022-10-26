package it.unipr.iotlab.connected.city.server;

/**
 * @author Crispino Mattia - 320745
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.CoAP.Type;

import it.unipr.iotlab.connected.city.resources.TrafficLight;

public class BridgeServer extends CoapServer {
	
	private static final int N = 10;												// number of bridges
	private static final int T  = 10000;											// swap period (ms)
	private static List<TrafficLight> tfList = new ArrayList<>();

	public static void main(String[] args) throws InterruptedException {
		BridgeServer server = new BridgeServer();
		
		// register N resources
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < 2; j++) {
				String name = "tf"+Integer.toString(i)+"_";
				name += j%2 == 0 ? "1" : "2";
				int status = j%2 != 0 ? 1 : 0;
				System.out.println(name + " status: " + status);
				TrafficLight tf = new TrafficLight(name, status);
				tf.setLightStatus(status);
				// make the resource observable
				tf.setObservable(true);
				tf.getAttributes().setObservable();
				server.add(tf);
								
				// add resource to the list
				tfList.add(tf);
			}
			System.out.println();
		}
		
		server.start();
		
		// task to update the status of the traffic lights
		while(true) {
			System.out.println();
			System.out.println("----------------- SERVER UPDATE ------------------");
			for (TrafficLight t : tfList) {
				Request req = Request.newPut();
				if(t.getLightStatus() == 1) {
					req.setPayload(Integer.toString(0));
				} else {
					req.setPayload(Integer.toString(1));
				}
				req.setType(Type.CON);
				req.setURI("coap://127.0.0.1:5683/" + t.getName());
				req.send();
				t.logID();
			}
			try {
				Thread.sleep(T);
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
			
		}
	
	}

	/**
	 * Method used to randomly select a resource and build its URI
	 * @return	the URI to connect with
	 */
	public static String namingService() {
		Random random = new Random();
		String URI = "coap://127.0.0.1:5683/tf";
		int tfBlock = random.nextInt(0, N-1);					// select the trafficLight "pair" to connect with
		int tfSide = random.nextInt(1,3);						// select the side from [1,2] where 1=left_side	2=right_side
		URI += Integer.toString(tfBlock) + "_" + Integer.toString(tfSide);
		return URI;
	}
	
	/**
	 * Build the complete URI of a given resource 
	 * @param tfName	the resource name
	 * @return			the complete URI of the specified resource
	 */
	public static String buildURI(String tfName) {
		String URI = "coap://127.0.0.1:5683/" + tfName;
		return URI;
	}
}
