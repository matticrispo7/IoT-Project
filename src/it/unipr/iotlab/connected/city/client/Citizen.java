package it.unipr.iotlab.connected.city.client;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.CoAP.Code;

import it.unipr.iotlab.connected.city.server.BridgeServer;

/**
 * @author Crispino Mattia - 320745
 */

public class Citizen {
	public static void main(String[] args) {
		try {
			Thread.sleep(10);
			CoapClient p = new CoapClient(BridgeServer.namingService());  	// select a random resource
			Request req = new Request(Code.GET);
			req.setPayload("CITIZEN");
			CoapResponse resp = p.advanced(req);
			System.out.println(Utils.prettyPrint(resp));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

}
