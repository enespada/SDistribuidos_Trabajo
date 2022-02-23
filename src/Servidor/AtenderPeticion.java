/*
 * Enrique Espada Calvo  
 */
package Servidor;

import java.io.DataInputStream;  
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Hilos.Descargador;

public class AtenderPeticion implements Runnable{

	private Socket s;
	private ConcurrentHashMap<String, String> clientes;
	private String rutaNube;
	private int threadCount;
	
	public AtenderPeticion(Socket s, ConcurrentHashMap<String, String> clientes, String rutaNube, int threadCount) {
		super();
		this.s=s;
		this.clientes = clientes;
		this.rutaNube = rutaNube;
		this.threadCount = threadCount;
	}
	
	public boolean buscarCliente(String user, String pwd) {
		if(this.clientes.containsKey(user)){
			if(this.clientes.get(user).equals(pwd)) {
//				this.rutaNube = this.rutaNube + "/" + user;
				return true;
			}
		}
		return false;
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
				if(!ruta.equals(this.rutaNube+"/"+nomcarpeta)) {
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
					// Si estamos recorriendo la carpeta del usuario, no metemos la /, ponemos el primer nombre de alguna carpeta
					if(rutainicial.equals("")) {
						rutaenviar = nomcarpeta;
					}
					// Si estamos recorriendo una carpeta situada en la carpeta del usuario o en alguna mas lejana, incluimos el /
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
							d = new Descargador(barrera, s, rutaaux, 0, tam-1);
							d.start();
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
	
	public static void vaciarCarpeta(String ruta) {
		File f = new File(ruta);
		File faux = null;
		if(f.isDirectory()) {
			String [] listaFich = f.list();
			for (String aux : listaFich) {
				faux = new File(ruta+"/"+aux);
				if(faux.isDirectory()) {
					vaciarCarpeta(ruta+"/"+aux);
				}
				faux.delete();
			}
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try (DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());) {
			
			// COMUNICACION CLIENTE-SERVIDOR //////////////////////////////////////////
			String user = dis.readLine().trim();
			user=user.trim();
			String pwd = dis.readLine();
			pwd=pwd.trim();
			if (this.buscarCliente(user, pwd)) {
				dos.flush();
				
				String dato = dis.readLine();
				String [] peticion, listaFich;
				byte [] buff;
				int tam;
				File f, faux;
				
				while(dato!=null) {
					// CARGAR ////////////////////////////////////////////////////////////////////////////////////////////////////////
					if(dato.contains("POST")) {
						peticion = dato.split(" ");
						
						// Si la peticion es crear un ARCHIVO----------------------------------------------------------------------
						if(peticion[1].contains(".")) {
							
							// Recuperamos el tamagno del archivo
							tam = Integer.parseInt(dis.readLine().trim());
							
							// Comprobamos si existe la carpeta en la que debe estar el archivo, si no es la propia del usuario
							// Si no existe (porque la peticion de crear el archivo ha llegado antes que la de crear la carpeta) la creamos
							if(peticion[1].contains("/")) {
								f = new File(this.rutaNube+"/"+user+"/"+peticion[1].substring(0, peticion[1].lastIndexOf("/")));
								if(!f.exists()) {
									f.mkdirs();
								}
							}
							
							// Ahora ya creamos el archivo
							try (FileOutputStream fosFich =  new FileOutputStream(this.rutaNube+"/"+user+"/"+peticion[1]);) {
								// Forma 1: la buena. Leemos todos los bytes que debemos directamente y los metemos en un byte [].
								buff = new byte[tam];
								dis.readFully(buff);  
								fosFich.write(buff);
								   
								// Forma 2: funciona mal porque se lee basura por medio
//								int totalBytesLeidos = 0;
//								int numBytesLeidos = 0;
//								buff = new byte[tam];
//								while(totalBytesLeidos < tam) {										
//									numBytesLeidos = dis.read(buff);
//									System.out.println(peticion[1]+" numbytes: "+numBytesLeidos);
//									if(totalBytesLeidos + numBytesLeidos > tam) {
//										fosFich.write(buff, 0, tam - totalBytesLeidos);
//										System.out.println(peticion[1]+" tam-tot: "+(tam-totalBytesLeidos));
//									}
//									else {
//										fosFich.write(buff, 0, numBytesLeidos);
//									}
//									totalBytesLeidos = totalBytesLeidos + numBytesLeidos;
//									System.out.println(peticion[1]+" totalbytes: "+totalBytesLeidos);
//								}
								
								// Forma 3: leer byte a byte. Funciona pero es muy lento
//								int cont = 0;
//								while(cont<=tam-1) {
//									fosFich.write(dis.read());
//									cont++;
//								}
		
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							} 
						}
						// Si la peticion es crear una CARPETA---------------------------------------------------------------
						else{
							f = new File(this.rutaNube+"/"+user+"/"+peticion[1]);
							if(!f.exists()) {
								f.mkdirs();
							}
						}
					}
					// DESCARGAR ////////////////////////////////////////////////////////////////////////////////////////////////////////
					if(dato.contains("GET")) {	
						this.recorrerCarpeta(s, this.rutaNube+"/"+user,"",user);
						dos.writeBytes("\r\n");
						dos.flush();
					}
					// ELIMINAR ////////////////////////////////////////////////////////////////////////////////////////////////////////
					if(dato.contains("DELETE")){
						f = new File(this.rutaNube+"/"+user);
						listaFich = f.list();
						for (String aux : listaFich) {
							faux = new File(this.rutaNube+"/"+user+"/"+aux);
							if(f.isDirectory()) {
								vaciarCarpeta(this.rutaNube+"/"+user+"/"+aux);
							}
							faux.delete();
						}
					}
					dato = dis.readLine();
				}
			} 
			else {
				dos.writeBytes("ERROR Usuario incorrecto \r\n");
				dos.flush();
			}

		} catch (IOException ex0) {
			// TODO: handle exception
			ex0.printStackTrace();
		}
	}
}

