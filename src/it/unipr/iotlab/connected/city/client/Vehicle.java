package it.unipr.iotlab.connected.city.client;

import it.unipr.iotlab.connected.city.server.BridgeServer;

/**
 * @author Crispino Mattia - 320745
 */

public class Vehicle {
	
	private static int M = 10;		//  number of clients to create
	/*private static int N = 10;
	private static final int COREPOOL = 40;
	private static final int MAXPOOL = 100;
	private static final long IDLETIME = 1000;
	private static ThreadPoolExecutor pool = new ThreadPoolExecutor(COREPOOL, MAXPOOL, IDLETIME,  TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
*/
	
	public static void main(String[] args) {
		
		for (int i = 0; i < M; i++) { 
			// start the new thread 
			VehicleThread v = new VehicleThread(BridgeServer.namingService(), i);
			Thread t = new Thread(v);
			t.start();
			//pool.execute(t);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
		}
		
		
	}
}
