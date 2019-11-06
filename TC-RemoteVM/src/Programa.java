//https://docs.oracle.com/cd/E17802_01/webservices/webservices/docs/2.0/tutorial/doc/JAXWS3.html

/* $Id: TestVBox.java 127855 2019-01-01 01:45:53Z bird $ */
/*! file
 * Small sample/testcase which demonstrates that the same source code can
 * be used to connect to the webservice and (XP)COM APIs.
 */

import org.apache.log4j.BasicConfigurator;
import org.virtualbox_6_0.*;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.security.bc.BCSecurityProvider;
import com.hierynomus.security.jce.JceSecurityProvider;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import ch.swaechter.smbjwrapper.SharedConnection;
import ch.swaechter.smbjwrapper.SharedDirectory;
import ch.swaechter.smbjwrapper.SharedFile;
import jcifs.netbios.NbtAddress;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

public class Programa {

	static void testEnumeration(VirtualBoxManager manager, IVirtualBox vBoxSVC) {

		List<IMachine> machs = vBoxSVC.getMachines();

		for (IMachine m : machs) {
			String name;
			Long ram = 0L;
			boolean hwvirtEnabled = false, hwvirtNestedPaging = false;
			boolean paeEnabled = false;
			boolean inaccessible = false;
			// System.out.println("Iterador: " + vBoxSVC.getMachines().listIterator());

			try {
				name = m.getName();
				ram = m.getMemorySize();
				List<IMediumAttachment> im = m.getMediumAttachments();
				for (IMediumAttachment ima : im) {
					System.out.println("IMa  " + ima);
				}
				hwvirtEnabled = m.getHWVirtExProperty(HWVirtExPropertyType.Enabled);
				hwvirtNestedPaging = m.getHWVirtExProperty(HWVirtExPropertyType.NestedPaging);
				paeEnabled = m.getCPUProperty(CPUPropertyType.PAE);
				String osType = m.getOSTypeId();
				IGuestOSType foo = vBoxSVC.getGuestOSType(osType);
				// System.out.println("Foo = " + foo);
			} catch (VBoxException e) {
				name = "<inaccessible>";
				inaccessible = true;
			}
			// System.out.println("VM name: " + name + "\n");
			if (!inaccessible) {
				MachineState state = m.getState();
				String estado = state.name();
				System.out.println("\nNome: " + name + "\nEstado: " + estado + "\nRAM: " + ram + "MB" + "\nHWVirt: "
						+ hwvirtEnabled + "\nPaginação aninhada: " + hwvirtNestedPaging
						+ "\nExtensão de Endereço Físico: " + paeEnabled);
			}
		}
		// process system event queue
		manager.waitForEvents(0);
	}

	// IMachine ISession
	// Unlocked =

//	The caller’s session object can then be used as a sort of remote control to the VM process that
//	was launched. It contains a “console” object (see ISession::console) with which the VM can be
//	paused, stopped, snapshotted or other things.

	// O objeto sessão tem um objeto console.

	static void ligarVM(VirtualBoxManager manager, IVirtualBox vbox, String machine) {

		try {
			IMachine m = vbox.findMachine(machine);

			String name = m.getName();

			if (m.getState().name() != "Running" || m.getState().name() != "Starting") {

				System.out.println("\nLigando VM " + name + ".");

				ISession session = manager.getSessionObject();

				IProgress p = m.launchVMProcess(session, "headless", "");

				progressBar(manager, p, 10000);

				if (p.getCompleted()) {
					System.out.println("VM " + name + " ligada.");
				}

				session.unlockMachine();

				// process system event queue
				manager.waitForEvents(0);

			} else {
				System.out.println("VM " + name + " já esta no estado " + m.getState().name() + ".");
			}

		} catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}
		// System.out.println("ISession getState: " + session.getState());
//		System.out.println("IMachine getState: " + m.getState());
//		System.out.println("IMachine getSessionState: " + m.getSessionState() + "\n");

//		try {
//			TimeUnit.SECONDS.sleep(30);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}

//		ISession getState: Unlocked
//		IMachine getState: PoweredOff
//		IMachine getSessionState: Unlocked
//
//		ISession getState: Spawning
//		IMachine getState: PoweredOff
//		IMachine getSessionState: Spawning
//
//		ISession getState: Unlocked
//		IMachine getState: Running
//		IMachine getSessionState: Locked		

	}

	static boolean progressBar(VirtualBoxManager manager, IProgress p, long waitMillis) {
		long end = System.currentTimeMillis() + waitMillis;
		while (!p.getCompleted()) {
			// process system event queue
			manager.waitForEvents(0);
			// wait for completion of the task, but at most 200 msecs
			p.waitForCompletion(200);
			if (System.currentTimeMillis() >= end)
				return false;
		}
		return true;
	}

	static void desligarVM(VirtualBoxManager manager, IVirtualBox vBoxSVC, String machine) {

		try {
			IMachine m = vBoxSVC.findMachine(machine);

			String name = m.getName();

			if (m.getState().name() != "PoweredOff") {

				System.out.println("\nDesligando VM " + name + ".");

				ISession session = manager.getSessionObject();

				vBoxSVC.findMachine(machine).lockMachine(session, LockType.Shared);

				IConsole iConsole = session.getConsole();

				IProgress p = iConsole.powerDown();

				progressBar(manager, p, 10000);

				if (p.getCompleted()) {
					System.out.println("VM " + name + " desligada.");
				}
			}
		}

		catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}

	}

	static void detalharHost(IVirtualBox vBoxSVC) {

		IHost h = vBoxSVC.getHost();

		long tamanho_memoria_disponivel = h.getMemoryAvailable();

		long tamanho_memoria_total = h.getMemorySize();

		double c = (((double) tamanho_memoria_disponivel / tamanho_memoria_total) * -100.0) + 100.0;

		System.out.println(

				"\nMemória total: " + h.getMemorySize() + " MB" + "\nMemória utilizada: "
						+ (h.getMemorySize() - h.getMemoryAvailable()) + " MB" + "\nMemória disponível: "
						+ h.getMemoryAvailable() + " MB" + "\nUso de Memória: " + Math.round(c) + " %"
						+ "\nSistema Operacional: " + h.getOperatingSystem() + "\nVersão: " + h.getOSVersion() +
//				"\nNúcleos de Processamento: " + h.getProcessorCoreCount() +
//				"\nProcessadores Lógicos: " + h.getProcessorCount() +
//				"\nNúcleos de Processamento Online:" + h.getProcessorOnlineCoreCount() +
//				"\nProcessadores Lógicos Online: " + h.getProcessorOnlineCount()
						"\nNúcleos de Processamento: " + h.getProcessorOnlineCoreCount() + "\nProcessadores Lógicos: "
						+ h.getProcessorOnlineCount());
	}
	
	// {IP, online_offline, mem_utilizada, mem_total} - uso_mem_final, mem_appliance
	// 
	public class Host{
		long tamanho_memoria_disponivel;
		long tamanho_memoria_total;
		boolean status;
		String ipAddress;
		Host(String ipAddress){
			
		}
	}
	public static String getMemoriaTotalHostEmMB(IVirtualBox vBoxSVC) {
		IHost h = vBoxSVC.getHost();
		return h.getMemorySize().toString();
	}
	
	public static String getMemoriaDisponivelHostEmMB(IVirtualBox vBoxSVC) {
		IHost h = vBoxSVC.getHost();
		return h.getMemoryAvailable().toString();
	}

	public static void listarAppliances(String ipServidorArquivos, String pastaCompartilhada) {
		AuthenticationContext authenticationContext = AuthenticationContext.anonymous();
		System.out.println("Conectando à pasta compartilhada \\\\" + ipServidorArquivos + "\\" + pastaCompartilhada);
		try (SharedConnection sharedConnection = new SharedConnection(ipServidorArquivos, pastaCompartilhada,
				authenticationContext)

		) {
			System.out.println("Conectado à pasta compartilhada \\\\" + ipServidorArquivos + "\\" + pastaCompartilhada);
			SharedDirectory rootDirectory = new SharedDirectory(sharedConnection);

			for (SharedFile sharedFile : rootDirectory.getFiles()) {
				System.out.println("\nLista de Appliance(s) :\n" + sharedFile.getName());
			}

		} catch (IOException e) {
			System.out.println("\nNão foi possível se conectar à pasta compartilhada.");
		}
	}

	public static void detalharAppliance(IVirtualBox vbox, String ipServidorArquivos, String pastaCompartilhada,
			String nomeAppliance) {
		String caminho = "\\\\" + ipServidorArquivos + "\\" + pastaCompartilhada + "\\" + nomeAppliance;
		System.out.println("\nDetalhes do Appliance: " + caminho);
		try {
			IAppliance appliance = vbox.createAppliance();
			appliance.read(caminho);
			appliance.interpret();

//			After calling this method
//			one can inspect the virtualSystemDescriptions[] array attribute,
//			which will then contain	one IVirtualSystemDescription
//			for each virtual machine found in the appliance.

			List<IVirtualSystemDescription> listavsd = appliance.getVirtualSystemDescriptions();

			for (IVirtualSystemDescription ivsd : listavsd) {
//				System.out.println("getCount: " + ivsd.getCount()); // 13
//	            https://github.com/OpenCyberChallengePlatform/OccpAdmin/blob/master/occp/edu/uri/dfcsc/occp/OccpVBoxHV.java

				System.out.println("\nNome: "
						+ ivsd.getValuesByType(VirtualSystemDescriptionType.Name,
								VirtualSystemDescriptionValueType.Original)
						+ "\nS.O.: "
						+ ivsd.getValuesByType(VirtualSystemDescriptionType.OS, VirtualSystemDescriptionValueType.Auto)
						+ "\nMemória (MB) : "
						+ ivsd.getValuesByType(
								VirtualSystemDescriptionType.Memory, VirtualSystemDescriptionValueType.Auto)
						+ "\nMemória (B)  : "
						+ ivsd.getValuesByType(VirtualSystemDescriptionType.Memory,
								VirtualSystemDescriptionValueType.Original)
						+ "\nNúcleos de CPU: "
						+ ivsd.getValuesByType(VirtualSystemDescriptionType.CPU, VirtualSystemDescriptionValueType.Auto)
						+ "\nImagem de Disco: " + ivsd.getValuesByType(VirtualSystemDescriptionType.HardDiskImage,
								VirtualSystemDescriptionValueType.Auto));

			}

			List<String> avisos = appliance.getWarnings();
			for (String s : avisos) {
				System.out.println("Aviso: " + s);
			}

			List<String> lista = appliance.getDisks();

			for (String s : lista) {
				String[] arrOfStr = s.split("\t", 0);
				int i = 0;
				for (String a : arrOfStr) {
					i++;
					if (i == 2) {
						System.out.println("Capacidade Máxima do disco (Bytes): " + a);
					}
				}
			}

		}

		catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}
	}

	public static void listarVM(IVirtualBox vBoxSVC) {

		// IVirtualBox
		// To enumerate all the virtual machines on the host, use the machines[]
		// attribute.
		List<IMachine> lista = vBoxSVC.getMachines();
		System.out.println("Quantidade de Maquinas Virtuais = " + lista.size());

		for (IMachine m : lista) {
			System.out.println("\nVM Nome: " + m.getName());
			System.out.println("RAM: " + m.getMemorySize());
			System.out.println("Tipo OS: " + m.getOSTypeId());
			System.out.println("Estado: " + m.getState());

			if (m.getVRDEServer().getEnabled()) {
				System.out.println("VRDE habilitado.");
				System.out.println("Porta VRDE: " + m.getVRDEServer().getVRDEProperty("TCP/Ports"));
//				for (String s : m.getVRDEServer().getVRDEProperties()) {
//					System.out.println("Propriedade: " + s);
//					Listar as propriedades do servidor VRDE
//					System.out.println("Propriedade: " + m.getVRDEServer().getVRDEProperty(s));
//				}
			} else {
				System.out.println("VRDE desabilitado.");
			}
		}

	}

	public static void removerVM(VirtualBoxManager manager, IVirtualBox vBoxSVC, String machine) {

		// if session not locked
		try {
			IMachine m = vBoxSVC.findMachine(machine);

			CleanupMode cm = CleanupMode.Full;
			// Full deleta os HDS que estavam anexadas a VM, bem como as ISOs

			m.deleteConfig(m.unregister(cm));
		} catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}

	}

	public static String getMemoriaAppliance(IVirtualBox vbox, String ipServidorArquivos, String pastaCompartilhada,
			String nomeAppliance) {
		String caminho = "\\\\" + ipServidorArquivos + "\\" + pastaCompartilhada + "\\" + nomeAppliance;

		try {
			IAppliance appliance = vbox.createAppliance();
			appliance.read(caminho);
			appliance.interpret();

//			After calling this method
//			one can inspect the virtualSystemDescriptions[] array attribute,
//			which will then contain	one IVirtualSystemDescription
//			for each virtual machine found in the appliance.

			List<IVirtualSystemDescription> listavsd = appliance.getVirtualSystemDescriptions();

			for (IVirtualSystemDescription ivsd : listavsd) {
				List<String> str = ivsd.getValuesByType(VirtualSystemDescriptionType.Memory,
						VirtualSystemDescriptionValueType.Auto);
				for (String s : str) {
//					System.out.println("Memoria Appliance: " + s);
					return s;
				}
				;
			}
		}

		catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}
		return null;

	}

	public static String getTamanhoDiscoVirtualAppliance(IVirtualBox vbox, String caminho) {
		try {

			IAppliance appliance = vbox.createAppliance();

			appliance.read(caminho);

			appliance.interpret();

			List<String> avisos = appliance.getWarnings();

			for (String s : avisos) {
				System.out.println("Aviso: " + s);
			}

			List<String> lista = appliance.getDisks();

			for (String s : lista) {
				String[] arrOfStr = s.split("\t", 0);
				int i = 0;
				for (String a : arrOfStr) {
					i++;
					if (i == 2) {
						return a;
					}
				}
			}

		}

		catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}
		return null;
	}

	public static void implantarAppliance(VirtualBoxManager manager, IVirtualBox vbox, String caminho) {

//		se nao der pra transferir, muda a arquitetura pra assumir que os arquivos já estão em todas as máquinas

//		https://crunchify.com/why-and-for-what-should-i-use-enum-java-enum-examples/

		try {

			IAppliance appliance = vbox.createAppliance();

			appliance.read(caminho);

			appliance.interpret();

			List<String> avisos = appliance.getWarnings();
			for (String s : avisos) {
				System.out.println("Aviso: " + s);
			}

			IProgress p = appliance.importMachines(null);

			while (!p.getCompleted()) {
				try {
					TimeUnit.SECONDS.sleep(5); //
					// System.out.println("getOperationPercent: " + p.getOperationPercent());
					System.out.println("getPercent: " + p.getPercent());

				} catch (InterruptedException e) {
					System.out.println("Erro: " + e);
				}

			}
		}

		catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}

		// progressBar(manager, p, 10000);

//			if (p.getCompleted()) {
//				return true;
//			}else {
//				return false;
//			}
	}

	/*
	 * 14.10.2019 - API do WebService do VirtualBox não tem um método que exibe o
	 * espaço livre em disco. public static long getEspacoLivreDiscoHost(IVirtualBox
	 * vBoxSVC) { // 08.10.2019 - esta pegando o espaço em disco do host local, ao
	 * invés do remoto ISystemProperties isp = vBoxSVC.getSystemProperties();
	 * System.out.println("getDefaultMachineFolder " +
	 * isp.getDefaultMachineFolder()); String unidade =
	 * isp.getDefaultMachineFolder().substring(0, 1);
	 * 
	 * File file = new File(unidade + ":\\");
	 * 
	 * // System.out.println("Espaço Livre: " + file.getFreeSpace()); // // double
	 * size = file.getFreeSpace() / (1024.0 * 1024 * 1024); // //
	 * System.out.printf("%.3f GB\n", size);
	 * 
	 * return file.getFreeSpace(); }
	 */

	public static boolean verificarDisponibilidadeHost(IVirtualBox vBoxSVC, String ipServidorArquivos,
			String pastaCompartilhada, String caminho) {

		int a, b = 0;
		a = Integer.parseInt(getMemoriaDisponivelHostEmMB(vBoxSVC));
		b = Integer.parseInt(
				getMemoriaAppliance(vBoxSVC, ipServidorArquivos, pastaCompartilhada, "Ubuntu18.04.1_1.0.ova"));

		/*
		 * 
		 * 14.10.2019 - API do WebService do VirtualBox não tem um método que exibe o
		 * espaço livre em disco.
		 * 
		 * long c, d = 0; c = getEspacoLivreDiscoHost(vBoxSVC); d =
		 * Long.parseLong(getTamanhoDiscoVirtualAppliance(vBoxSVC, caminho));
		 * 
		 */

//		if (a >= b && c >= d) {
		if (a >= b) {
			return true;
		} else
			return false;

	}

	public String oberIPPeloHostname(String hostname) {
		try {
			NbtAddress address = NbtAddress.getByName(hostname);
			return address.getHostAddress();
		} catch (UnknownHostException e) {
			System.out.println("Não pode resolver o endereço IP do host " + hostname);
			return null;
		}
	}

	public static VirtualBoxManager conectarWS(String ipHost) {
		// Cada instância de VirtualBoxManager é um host com o VirtualBox
		VirtualBoxManager manager = VirtualBoxManager.createInstance(null);
		boolean webserver = true;
		String porta="18083";
		String url = "http://" + ipHost + ":" + porta;
		System.out.println("url = " + url);
		String usuario = null;
		String senha = null;
//		IWebsessionManager wsm = new IWebsessionManager()

		System.out.println("\nConectando em " + url + " com usuário " + usuario + " e senha " + senha + "...");
		try {
//			manager.connect("http://10.1.1.26:18083", null, null);
			manager.connect(url, usuario, senha);
			System.out.println("\nCliente conectado com sucesso em " + url + " com usuário " + usuario + " e senha "
					+ senha + ".");

		} catch (VBoxException e) {
			System.out.println("\nO cliente nao pôde conectar ao webserver " + url + " com usuário " + usuario
					+ " e senha " + senha + ".");
		}
		manager.waitForEvents(0);
		return manager;
	}

	public static void desconectarWS(VirtualBoxManager manager) {
		// manager.cleanup();
		try {
			manager.disconnect();
		} catch (VBoxException e) {
			System.out.println("\nNão foi possível se desconectar do webservice.");
		}
	}

	public static List<String> lerIPs() {
		BufferedReader in;
		String str;
		List<String> list = new ArrayList<String>();
		try {
			in = new BufferedReader(
					new FileReader("C:\\Users\\Administrator\\eclipse-workspace\\TC-RemoteVM\\src\\Hosts.txt"));
			while ((str = in.readLine()) != null) {
				list.add(str);
			}
			return list;
		} catch (IOException e) {
			System.out.println("\nArquivo não encontrado.");
			return list;
		}

	}
	// o worst fit, que deixa muita memória sobrando, seria, no nosso caso, o best fit,
	// pois deixa mais memória pro usuário
	
	// o best fit, que deixa pouca memória sobrando, seria, no nosso caso, o worst fit
	// pois deixa pouca memória pro usuário
	
	// o first fit, no nosso caso, apenas pegaria o primeiro com memória disponível,
	// sem considerar a atual utilização da máquina host
	
	
	public static void main(String[] args) throws java.io.IOException {
		// objetivo = alta disponibilidade de servidores web ngix com várias VMS
		
		String ipServidorArquivos = "10.1.1.4";
		String pastaCompartilhada = "Teste";
		listarAppliances(ipServidorArquivos, pastaCompartilhada);

		
		// IP - online - capacidade - (margem)
		
		// USO FINAL = (Memória utilizado + Memória Appliance)/Memória Total
		

		// {String IP, mem_utilizada, mem_total} - uso_mem_final, mem_appliance 
		
		List<String> ipsHosts = lerIPs();
		System.out.println("\nIPs dos hosts cadastrados:" + ipsHosts);
		HashMap<String, List<String>> hosts = new HashMap<>();

		
		for (String s: ipsHosts) {
			System.out.println(s);
			
			VirtualBoxManager manager = conectarWS(s);
			
			// hashmap
			try {
				// IVirtualBox = VBoxSVC.exe = Processo que controla tudo independente da
				// interface.
				IVirtualBox vBoxSVC = manager.getVBox();

				if (vBoxSVC != null) {
					
					String memoriatotalhostemMB = getMemoriaTotalHostEmMB(vBoxSVC);
					String memoriadisponivelhostemMB = getMemoriaDisponivelHostEmMB(vBoxSVC);
					String[] memoria = {memoriatotalhostemMB, memoriadisponivelhostemMB};
					hosts.put(s, Arrays.asList(memoria));
					// System.out.println("VirtualBox version: " + vBoxSVC.getVersion() + "\n");
					// detalharAppliance(vBoxSVC, ipServidorArquivos, pastaCompartilhada,
					// "Ubuntu18.04.1_1.0.ova");
					detalharHost(vBoxSVC);
					listarVM(vBoxSVC);
					// System.out.println("getDiscoAppliance: " + getTamanhoDiscoVirtualAppliance(vBoxSVC, "F:\\Apple.ova"));
					// verificarDisponibilidadeHost(vBoxSVC, manager, "F:\\Apple.ova");
					// desligarVM(manager, vBoxSVC, "Ubuntu18.04.1 1");
					// implantarAppliance(manager, vBoxSVC, "\\\\10.1.1.4\\Teste\\Ubuntu18.04.1_1.0.ova");
					// ligarVM(manager, vBoxSVC,"Ubuntu18.04.1 1");
					// removerVM(manager, vBoxSVC, "Ubuntu18.04.1 1");
					// testEnumeration(manager, vBoxSVC);
				}
			} catch (VBoxException e) {
				System.out.println("Erro: " + e);
			}
			System.out.println("hosts :" + hosts);
			
		}

		
	}
}