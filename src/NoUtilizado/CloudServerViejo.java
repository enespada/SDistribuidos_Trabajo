package NoUtilizado;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudServerViejo {
	
	private Map<String, String> clientes;
	private String rutaNube = "./CloudServer/Nube";
	private int threadCount;

	public CloudServerViejo() {
		// TODO Auto-generated constructor stub
		this.clientes = new HashMap<String,String>();
		
		this.aniadirCliente("enespada", "xxxx");
		this.aniadirCliente("pepe", "yyyy");
		
		this.threadCount = Runtime.getRuntime().availableProcessors();
	}
	
	public CloudServerViejo(Map<String, String> clientes) {
		// TODO Auto-generated constructor stub
		this.clientes = clientes;
		this.threadCount = Runtime.getRuntime().availableProcessors();
	}
	
	public void aniadirCliente(String user, String pwd) {
		this.clientes.put(user, pwd);
		File f = new File(this.rutaNube+"/"+user);
		if(!f.exists()) {
			f.mkdirs();
		}
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
	
	public void cargar(Socket s, String user) {
//		int N = this.threadCount-1; // 8-1
//		CyclicBarrier barrera = new CyclicBarrier(N+1);
		
		DataInputStream dis = null;
		DataOutputStream dos = null;
		try {
			dis = new DataInputStream(s.getInputStream());
			dos = new DataOutputStream(s.getOutputStream());
			
			dos.flush();
			
			String dato = dis.readLine();
			String [] peticion;
			byte [] buff;
			int tam;
			int numBytesLeidos;
			int pos;
			String carpeta;
			File f;
			int cont;
			while(dato!=null) {
				System.out.println("Peticion: "+dato);
				if(dato.contains("POST")) {
					peticion = dato.split(" ");
					System.out.println(peticion[1]);
					
					// Si la peticion es crear un ARCHIVO-------------------------------------------------------------------
					if(peticion[1].contains(".")) {
						
						// Recuperamos el tamagno del archivo
						tam = Integer.parseInt(dis.readLine().trim());
						System.out.println("tam: "+tam);
						
						// Comprobamos si existe la carpeta en la que debe estar el archivo, si no es la propia del usuario
						// Si no existe (porque la peticion de crear el archivo ha llegado antes que la de crear la carpeta) la creamos
						if(peticion[1].contains("/")) {
							pos = peticion[1].lastIndexOf("/");
							System.out.println("pos: " + pos); 
							carpeta = peticion[1].substring(0,pos);
							System.out.println("carpeta: " + carpeta);
							f = new File(carpeta);
							if(!f.exists()) {
								f.mkdirs();
							}
						}
						
						// Ahora ya creamos el archivo
						try (FileOutputStream fosFich =  new FileOutputStream(this.rutaNube+"/"+user+"/"+peticion[1]);) {
//							int totalBytesLeidos = 0;
//							buff = new byte[tam];
//							numBytesLeidos = dis.read(buff);
//							totalBytesLeidos = totalBytesLeidos + numBytesLeidos;
//							fosFich.write(buff,0,numBytesLeidos);
//							while(totalBytesLeidos!=tam) {	
//								System.out.println("numbytes: "+numBytesLeidos);
//								numBytesLeidos = dis.read(buff);
//								fosFich.write(buff,0,numBytesLeidos);
//							}
//							System.out.println("total: "+totalBytesLeidos);
							
							cont = 0;
							int var;
							while(cont<=tam-1) {
//								var=dis.read();
//								fosFich.write(var);
//								System.out.println("var: "+var);
								fosFich.write(dis.read());
								cont++;
							}
							System.out.println("cont: " + cont);
	
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
				dato = dis.readLine();
			}
			dos.flush();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	

	private List<String> contenidoCarpeta(String user) {
		File f = new File(this.rutaNube+"/"+user);
		List<String> lista = Arrays.asList(f.list()); 
		return lista;
	}
	
	public static void main(String[] args) {
		ExecutorService pool = Executors.newCachedThreadPool();

		// Direccion IP del ordenador-------------------------------------------------
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

		try (ServerSocket ss = new ServerSocket(7777)) {
			Socket s = null;
			while (true) {
				try {
					s = ss.accept();
					try (DataInputStream dis = new DataInputStream(s.getInputStream());
							DataOutputStream dos = new DataOutputStream(s.getOutputStream());) {

						// COMUNICACION CLIENTE-SERVIDOR //////////////////////////////////////////
						CloudServerViejo cs = new CloudServerViejo();

						String user = dis.readLine();
						user=user.trim();
//						System.out.println(user);
						String pwd = dis.readLine();
						pwd=pwd.trim();
//						System.out.println(pwd);
						String a = "";
						if (cs.buscarCliente(user, pwd)) {
							for (String aux : cs.contenidoCarpeta("/"+user)) {
								a = a + " " + aux;
							}
							dos.writeBytes(a + "\r\n");
							dos.flush();
							
							int codop = Integer.parseInt(dis.readLine());
							switch (codop) {
							case 0: {
								// SUBIR/CARGAR------------------------------------
								cs.cargar(s,user);
								

							}
							case 1: {
								// DESCARGAR----------------------------------------
								
								
							}
							case 2: {
								// ELIMINAR----------------------------------------
								
							}
							default:

							}
						} 
						else {
							dos.writeBytes("Error. Usuario incorrecto.");
							dos.flush();
						}

					} catch (IOException ex0) {
						// TODO: handle exception
						ex0.printStackTrace();
					}

				} catch (IOException ex) {
					// TODO: handle exception
					ex.printStackTrace();
				} catch (NumberFormatException ex1) {
					// TODO: handle exception
					ex1.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			pool.shutdown();
		}
	}

}
