/*
 * Enrique Espada Calvo 
 */
package Cliente;

import java.io.DataInputStream;   
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Hilos.Descargador;

public class CloudClient {
	
	private String rutaCargar;
	private String rutaDescargar;
	private String nomcarpetaCargar = "Cargar";
	private String nomcarpetaDescargar = "Descargar";
	private int threadCount;
	
	public CloudClient(String host, String user, String pwd) {		
		this.rutaCargar = "./CloudClient/" + this.nomcarpetaCargar;
		this.rutaDescargar = "./CloudClient/" + this.nomcarpetaDescargar;
		this.threadCount = Runtime.getRuntime().availableProcessors();
		
		try (Socket s = new Socket(host,7777);
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());){
			
			// COMUNICACION CLIENTE-SERVIDOR //////////////////////////////////////////
			
			dos.writeBytes(user+"\r\n");
			dos.writeBytes(pwd+"\r\n");
			dos.flush();
			
			// Recogemos la operacion que el cliente quiere realizar sobre sus datos en la nube
			Scanner entrada = new Scanner(System.in);
			System.out.println("Elige que operacion quieres realizar sobre los datos introduciendo un numero entre 0 y 2:");
			System.out.println("0. Subir el contenido de la carpeta cargar a la nube");
			System.out.println("1. Descargar TODO el contenido de la nube en la carpeta descargar");
			System.out.println("2. Eliminar TODO el contenido de la nube");
			int codop = entrada.nextInt();
			
			switch (codop) {
			case 0: {
				// SUBIR/CARGAR------------------------------------
				this.cargar(s);
				break;
			}
			case 1: {
				// DESCARGAR----------------------------------------	
				this.descargar(s,"");	
				break;
			}
			case 2: {
				// ELIMINAR----------------------------------------
				this.eliminar(s, "");
				break;
			}
			default:
				
			}	
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public void cargar(Socket s) {
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
//					System.out.println("Nombre: "+nomcarpeta+" rutaaux: "+rutaaux+" rutaenviar: "+rutaenviar+" bool "+faux.isDirectory());
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
							int N = this.threadCount-1;
							final CyclicBarrier barrera = new CyclicBarrier(N+1);
							int trozos = tam/N;
							ExecutorService pool = null;
							try {
								pool = Executors.newFixedThreadPool(N);
								int ini=0;
								int fin=trozos;
								for(int i=1;i<=N;i++) {
									if(i==N){
										fin=tam-1;
									}
									pool.execute(new Descargador(barrera, s, rutaaux, ini, fin));
									ini = fin + 1;
									fin = trozos * (i+1);
								}
								barrera.await();
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							} finally {
								if(pool!=null) {
									pool.shutdown();
								}
							}
						}
						// Si el archivo NO ES GRANDE, utilizamos un solo hilo para mandarlo----------------------------------------
						else {
							int N = 1; 
							final CyclicBarrier barrera = new CyclicBarrier(N+1);
							
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
	
	public void descargar(Socket s, String ruta) {
		DataInputStream dis = null;
		DataOutputStream dos = null;
		try {
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());
			
			dos.writeBytes("GET "+ruta+ " \r\n");
			dos.flush();
			
			String dato = dis.readLine();
			String [] peticion;
			byte [] buff;
			int tam;
			File f;
			
			while(!dato.equals("")) {
				if(dato.contains("POST")) {
					peticion = dato.split(" ");
					
					// Si la peticion es crear un ARCHIVO---------------------------------------------------------------------
					if(peticion[1].contains(".")) {
						// Recuperamos el tamagno del archivo
						tam = Integer.parseInt(dis.readLine().trim());
						
						// Comprobamos si existe la carpeta en la que debe estar el archivo, si no es la propia del usuario
						// Si no existe (porque la peticion de crear el archivo ha llegado antes que la de crear la carpeta) la creamos
						if(peticion[1].contains("/")) {
							f = new File(this.rutaDescargar+"/"+peticion[1].substring(0,peticion[1].lastIndexOf("/")));
							if(!f.exists()) {
								f.mkdirs();
							}
						}
						
						// Ahora ya creamos el archivo
						try (FileOutputStream fosFich =  new FileOutputStream(this.rutaDescargar+"/"+peticion[1])) {
							buff = new byte[tam];
							dis.readFully(buff);
							fosFich.write(buff);
	
						} catch (Exception e) {
							// TODO: handle exception
							e.printStackTrace();
						} 
					}
					// Si la peticion es crear una CARPETA---------------------------------------------------------------
					else{
						f = new File(this.rutaDescargar+"/"+peticion[1]);
						if(!f.exists()) {
							f.mkdirs();
						}
					}
				}
				dato = dis.readLine();
			}
			dos.flush();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public void eliminar(Socket s, String ruta) {
		DataInputStream dis = null;
		DataOutputStream dos = null;
		try {
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());
			
			dos.writeBytes("DELETE "+ruta+ " \r\n");
			dos.flush();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} 
	}

	public static void main(String[] args) {
		String host = "192.168.56.1";
//		host = "192.168.1.80";
		CloudClient cc = new CloudClient(host,"enespada","xxxx");
//		CloudClient cc = new CloudClient(host,"peperez","yyyy");
	}
	
}
