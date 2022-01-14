/*
 * Enrique Espada Calvo 
 */
package Hilos;

import java.io.DataInputStream; 
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.concurrent.CyclicBarrier;

public class Descargador extends Thread {
	
	// Para leer el fichero:
	private RandomAccessFile raf;
	
	private final CyclicBarrier barrera;
	private Socket s;
	private String ruta;
	private int ini;
	private int fin;
	

	public Descargador(CyclicBarrier barrera, Socket s, String ruta, int ini, int fin) {
		// TODO Auto-generated constructor stub
		this.barrera=barrera;
		this.s=s;
		this.ruta=ruta;
		this.ini=ini;
		this.fin=fin;
	}

	public void run() {
		// TODO Auto-generated method stub
		
		// Para leer del Socket:
//		DataInputStream dis = null;
		// Para escribir en el Socket:
		DataOutputStream dos = null;
		try {
//			dis = new DataInputStream(this.s.getInputStream());
			dos = new DataOutputStream(this.s.getOutputStream());
			
			this.raf = new RandomAccessFile(this.ruta, "r");
//			System.out.println(byteIni);
//			System.out.println(byteFin);
			raf.seek(ini);
			byte [] buff = new byte[fin-ini+1];
			raf.readFully(buff);
			dos.write(buff);
//			dos.flush();
			
			barrera.await();

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		finally {
			// Cerramos el raf
			if(raf!=null) {
				try {
					raf.close();
				} catch (Exception e2) {
					// TODO: handle exception
					e2.printStackTrace();
				}
			}
		}
		
	}
	
}
