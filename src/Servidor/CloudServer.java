/*
 * Enrique Espada Calvo 
 */
package Servidor;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudServer {
	
	private ConcurrentHashMap<String, String> clientes;
	private String rutaNube = "./CloudServer/Nube";
	private int threadCount;

	public CloudServer() {
		// TODO Auto-generated constructor stub
		this.clientes = new ConcurrentHashMap<String,String>();
		this.threadCount = Runtime.getRuntime().availableProcessors();
	}
	
	public CloudServer(ConcurrentHashMap<String, String> clientes) {
		this.clientes = new ConcurrentHashMap<String,String>();
		for (String user : clientes.keySet()) {
			this.aniadirCliente(user, clientes.get(user));
		}
		this.threadCount = Runtime.getRuntime().availableProcessors();
		ExecutorService pool = Executors.newCachedThreadPool();
		
		// Direccion IP del ordenador nube-------------------------------------------------
		try {
			InetAddress address = InetAddress.getLocalHost();
			System.out.println("Bienvenido a la nube de almacenamiento");
			System.out.println("La direccion para conectarte es: " + address.getHostAddress() + "\r\n");
		} catch (UnknownHostException e) {
			System.out.println(e);
		}
		
		// Numero de cores del procesador---------------------------------------------
//		this.threadCount = Runtime.getRuntime().availableProcessors();
//		System.out.println(this.threadCount);
		
		try(ServerSocket ss = new ServerSocket(7777)) {
			Socket s = null;
			while (true) {
				try {
					s = ss.accept();
					pool.execute(new AtenderPeticion(s,this.clientes, this.rutaNube, this.threadCount));
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			} 
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			pool.shutdown();
		}
						
	}
	
	public void aniadirCliente(String user, String pwd) {
		this.clientes.put(user, pwd);
		File f = new File(this.rutaNube+"/"+user);
		if(!f.exists()) {
			f.mkdirs();
		}
	}
	
	public static void main(String[] args) {
		ConcurrentHashMap<String, String> clientes = new ConcurrentHashMap<String,String>();
		clientes.put("enespada", "xxxx");
		clientes.put("peperez", "yyyy");
		clientes.put("anamoreno", "zzzz");
		CloudServer cs  = new CloudServer(clientes);
	}

}
