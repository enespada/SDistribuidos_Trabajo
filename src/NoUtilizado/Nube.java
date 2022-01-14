package NoUtilizado;

import java.awt.*;  

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.JButton;

import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class Nube extends JFrame {

	private JPanel contentPane;
	private JPanel panel;
	
	private CountDownLatch count;
	private JList<String> carpeta;
	private JTree tree;
	private Integer codop = null;
	private List<String> lista;
	private Socket s;
	
	// Establecemos 3 codigos distintos, para representar las tres operaciones posibles:
	// Subir/Cargar: 0
	// Descargar: 1
	// Eliminar: 2

	/**
	 * Launch the application.
	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					Nube frame = new Nube();
//					frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}
	
//	public static void main(String[] args) {
//		Nube n = new Nube(new ArrayList<String>());	
//	}

	/**
	 * Create the frame.
	 */
	public Nube(CountDownLatch count, List<String> lista, Socket s) {
		super();
		
		this.count=count;
		this.lista=lista;
		this.s=s;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		panel = new JPanel();
		contentPane.add(panel, BorderLayout.CENTER);

		// Carpeta del cliente---------------------------------------------------------------------
		carpeta = new JList<String>();
		carpeta.setSize(400, 500);
		panel.add(carpeta);
		carpeta.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				if (me.getClickCount() == 1) {
					JList target = (JList) me.getSource();
					int index = target.locationToIndex(me.getPoint());
					if (index >= 0) {
						Object item = target.getModel().getElementAt(index);
					}
				}
			}
		});
		
		this.actualizarVista();
		
		// Boton SUBIR/CARGAR------------------------------------------------------------------------
		JButton btn_cargar = new JButton("Subir");
		btn_cargar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manejadorCargar();
//				setCodop(0);
			}
		});
		panel.add(btn_cargar);
		
		// Boton DESCARGAR---------------------------------------------------------------------------
		JButton btn_descargar = new JButton("Descargar");
		btn_descargar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manejadorDescargar();
//				setCodop(1);
			}
		});
		panel.add(btn_descargar);
		
		// Boton ELIMINAR----------------------------------------------------------------------------
		JButton btn_eliminar = new JButton("Eliminar");
		btn_eliminar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manejadorEliminar();
//				setCodop(2);
			}
		});
		panel.add(btn_eliminar);
		
		this.setVisible(true);
	}
	
	public int getCodop() {
		return this.codop;
	}
	
	public void setCodop(int codop) {
		this.codop = codop;
	}
	
	public void manejadorCargar() {
		setCodop(0);
		count.countDown();
	}
	
	public void manejadorDescargar() {
		setCodop(1);
		count.countDown();
	}
	
	public void manejadorEliminar() {
		setCodop(2);
		count.countDown();
	}
	
	public void actualizarVista() {
//		DefaultListModel<String> modelo = new DefaultListModel<String>();
//		for (String aux : this.lista) {
//			modelo.addElement(aux);
//		}
//		this.carpeta.setModel(modelo);
		
		// TERMINAAAAAR
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Client");
        //create the child nodes
        DefaultMutableTreeNode vegetableNode = new DefaultMutableTreeNode("Vegetables");
        DefaultMutableTreeNode fruitNode = new DefaultMutableTreeNode("Fruits");
        //add the child nodes to the root node
        root.add(vegetableNode);
        root.add(fruitNode);
         
        //create the tree by passing in the root node
        JTree tree = new JTree(root);
        tree.setSize(400, 500);
        this.panel.add(tree);
	}
	
	private void pedirListaArchivos(String ruta) {
		
	}

}
