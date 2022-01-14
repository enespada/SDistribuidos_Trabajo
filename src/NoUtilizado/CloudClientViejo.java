package NoUtilizado;

import java.io.DataInputStream; 
import java.io.DataOutputStream;
import java.io.File;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Hilos.Descargador;

public class CloudClientViejo {
	
	private String rutaCargar;
	private String nomcarpetaCargar = "Cargar";
	private int threadCount;
	
	public CloudClientViejo() {		
		this.rutaCargar = "./CloudClient/" + this.nomcarpetaCargar;
		this.threadCount = Runtime.getRuntime().availableProcessors();
	}
	
	public void cargar(Socket s) {
//		try {
//			File f = new File(this.rutaCargar);
//			String [] lista = f.list();
//			File faux;	
//			String ruta;
//			for (String aux : lista) {
//				ruta = this.rutaCargar + "/" + aux;
//				faux = new File(ruta);
//				if(faux.isDirectory()) {
//					this.recorrerCarpeta(s,ruta,aux);
//				}
//			}
//			
//		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}
		
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(s.getOutputStream());
			this.recorrerCarpeta(s, this.rutaCargar,"",this.nomcarpetaCargar);
			dos.flush();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public void recorrerCarpeta(Socket s, String ruta, String rutainicial, String nomcarpeta) {
		// La ruta incluye el nombre de la carpeta
		// La rutaenviar incluye el nombre de la carpeta
		DataOutputStream dos = null;
		try {
			File f = new File(ruta);
			dos = new DataOutputStream(s.getOutputStream());
			if(f.isDirectory()) {
				// NOTA: escribimos solo el nombre de las carpetas y archivos contenidos en la carpeta que contiene 
				//los archivos a cargar
				if(!ruta.equals(this.rutaCargar)) {
					dos.writeBytes("POST "+rutainicial + " \r\n");
				}
				
				// Recorremos todos los elementos de la carpeta
				String [] lista = f.list();
				File faux;
				String rutaaux = "";
				String rutaenviar = "";
				Descargador d = null;
				for (String aux : lista) {
					// Actualizamos la ruta del archivo en el equipo del cliente--------------------------------------------------
					rutaaux = ruta + "/" + aux;
					
					// Actualizamos el nombre de la carpeta-----------------------------------------------------------------------
					nomcarpeta = aux;
					
					// Actualizamos la ruta para la peticion-----------------------------------------------------------------------
					// Si estamos recorriendo la carpeta Cargar, no metemos la /, ponemos el primer nombre de alguna carpeta
					if(rutainicial.equals("")) {
						rutaenviar = nomcarpeta;
					}
					// Si estamos recorriendo una carpeta situada en Cargar o en alguna mas lejana, incluimos el /
					else {
						rutaenviar = rutainicial + "/" + nomcarpeta;
					}
					
					// Construimos el fichero actual
					faux = new File(rutaaux);
					System.out.println("Nombre: "+nomcarpeta+" rutaaux: "+rutaaux+" rutaenviar: "+rutaenviar+" bool "+faux.isDirectory());
					// Si es una CARPETA, la recorremos recursivamente-----------------------------------------------------------
					if(faux.isDirectory()) {
						recorrerCarpeta(s, rutaaux, rutaenviar, nomcarpeta);
					}
					
					// Si es un ARCHIVO, LO ENVIAMOS------------------------------------------------------------------------------
					else{
						// Enviamos la peticion para que se incluya el archivo en la nube
						dos.writeBytes("POST " + rutaenviar + " \r\n");
						Integer tam = (int) faux.length();
						// La segunda linea que mandamos es el tamagno del archivo, para que el servidor sepa cuanto debe leer
						dos.writeBytes(tam + "\r\n");
						// Si el archivo es GRANDE (1KB=1024000B o mas), creamos varios hilos para mandarlo a trozos---------------
						if(tam>=1000000) { 
							int N = Runtime.getRuntime().availableProcessors()-1; // 8-1
							CyclicBarrier barrera = new CyclicBarrier(N+1);
							int trozos = tam/N;
							
							ExecutorService pool = Executors.newFixedThreadPool(N);
							int ini=0;
							int fin=trozos;
							for(int i=1;i<=N;i++) {
								if(i==N){
									fin=tam-1;
								}
//								System.out.println("ini: "+ini+" fin: "+fin);
								pool.execute(new Descargador(barrera, s, rutaaux, ini, fin));
								ini = fin + 1;
								fin = trozos * (i+1);
							}
							
							barrera.await();
						}
						// Si el archivo NO ES GRANDE, utilizamos un solo hilo para mandarlo----------------------------------------
						else {
							int N = 1; // 8-1
							CyclicBarrier barrera = new CyclicBarrier(N+1);
							
							// Descargador hereda de Thread--------------------------------------------------------------------------
							// Forma 1:----------------------------------------------------------------------------------------------
							d = new Descargador(barrera, s, rutaaux, 0, tam-1);
							d.start();

							// Forma 2:-----------------------------------------------------------------------------------------------
//							(new Descargador(barrera, s, rutaaux, 0, tam-1)).start();
							
							// Descargador implementa Runnable-----------------------------------------------------------------------
							// Forma 1:----------------------------------------------------------------------------------------------
//							d = new Descargador(barrera, s, rutaaux, 0, tam-1);
//							Thread h1 = new Thread(d);
//							h1.start();
							
							// Forma 2:-----------------------------------------------------------------------------------------------
//							(new Thread((new Descargador(barrera, s, rutaaux, 0, tam-1)))).start();
							// Forma 3:-----------------------------------------------------------------------------------------------
//							d = new Descargador(barrera, s, rutaaux, 0, tam-1);
//							ExecutorService hilo = Executors.newSingleThreadExecutor();
//							hilo.execute(d);
//							hilo.shutdown();
							
							barrera.await();
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		final CountDownLatch count = new CountDownLatch(1);
		try (Socket s = new Socket("192.168.56.1",7777);
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());){
			
			// COMUNICACION CLIENTE-SERVIDOR //////////////////////////////////////////
			
			CloudClientViejo cc = new CloudClientViejo();
			
			String user = "enespada";
			String pwd = "xxxx";
			
			dos.writeBytes(user+"\r\n");
			dos.writeBytes(pwd+"\r\n");
			dos.flush();
			
			String a = dis.readLine();
			String [] cachos = a.split(" ");
			List<String> lista = Arrays.asList(cachos); 
		
			Nube nube = new Nube(count, lista, s);
			
			count.await();
			int codop = nube.getCodop();
			dos.writeBytes(codop + "\r\n");
			dos.flush();
			
			switch (codop) {
			case 0: {
				// SUBIR/CARGAR------------------------------------
				cc.cargar(s);
				
				
//				yield type;
			}
			case 1: {
				// DESCARGAR----------------------------------------				
				
			}
			case 2: {
				// ELIMINAR----------------------------------------
						
			}
			default:
//				dos.writeBytes("Error. Operacion incorrecta.");
//				dos.flush();
			}
			
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
}
