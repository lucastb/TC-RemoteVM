
public class Programa2 {

	public static void main(String[] args) {
		ServidorArquivos sa = new ServidorArquivos("127.0.0.1", "Appliances");
		Appliance a = new Appliance("Demo.ova", null, null, null, null,sa);
		
	}

}
