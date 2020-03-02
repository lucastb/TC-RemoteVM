import java.util.ArrayList;
import java.util.List;

import org.virtualbox_6_0.IVirtualBox;

public class ServidorArquivos {
	private static String ipServidorArquivos;
	private static String pastaCompartilhada = null;
	
	private static ArrayList<Appliance> appliances;
	
	public ServidorArquivos(String ipServidorArquivos, String pastaCompartilhada){
		this.appliances = new ArrayList<>();
		this.ipServidorArquivos = ipServidorArquivos;
		this.pastaCompartilhada = pastaCompartilhada;
	}

	public static String getIpServidorArquivos() {
		return ipServidorArquivos;
	}

	public static void setIpServidorArquivos(String ipServidorArquivos) {
		ServidorArquivos.ipServidorArquivos = ipServidorArquivos;
	}

	public static String getPastaCompartilhada() {
		return pastaCompartilhada;
	}

	public static void setPastaCompartilhada(String pastaCompartilhada) {
		ServidorArquivos.pastaCompartilhada = pastaCompartilhada;
	}

	public List<Appliance> getAppliances() {
		return appliances;
	}

	public static void adicionarAppliance(Appliance a) {
		appliances.add(a);
	}
	
}
