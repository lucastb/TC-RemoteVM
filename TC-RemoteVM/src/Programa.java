//	feature						effort		user-loveit		revenue
//  priorizar por recurso		2			 	
//	parâmetros					1			
//	parametro exibir console	
//	parametro serv arquivos		
//	parametro lista ips			
//	

//	Objetivo => Implementar três vms com apache e conectar nelas
//	
//	https://docs.oracle.com/cd/E17802_01/webservices/webservices/docs/2.0/tutorial/doc/JAXWS3.html

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.virtualbox_6_0.CleanupMode;
import org.virtualbox_6_0.IAppliance;
import org.virtualbox_6_0.IConsole;
import org.virtualbox_6_0.IHost;
import org.virtualbox_6_0.IMachine;
import org.virtualbox_6_0.IProgress;
import org.virtualbox_6_0.ISession;
import org.virtualbox_6_0.IVirtualBox;
import org.virtualbox_6_0.IVirtualSystemDescription;
import org.virtualbox_6_0.LockType;
import org.virtualbox_6_0.VBoxException;
import org.virtualbox_6_0.VirtualBoxManager;
import org.virtualbox_6_0.VirtualSystemDescriptionType;
import org.virtualbox_6_0.VirtualSystemDescriptionValueType;

import com.hierynomus.smbj.auth.AuthenticationContext;

import ch.swaechter.smbjwrapper.SharedConnection;
import ch.swaechter.smbjwrapper.SharedDirectory;
import ch.swaechter.smbjwrapper.SharedFile;
import jcifs.netbios.NbtAddress;

public class Programa {

//	IMachine ISession
//	Unlocked =

//	The caller’s session object can then be used as a sort of remote control to the VM process that
//	was launched. It contains a “console” object (see ISession::console) with which the VM can be
//	paused, stopped, snapshotted or other things.

	// O objeto sessão tem um objeto console.

	public static String ipServidorArquivos;
	public static String pastaCompartilhada = null;
	public static String parametros;
	public static List<String> ips = new ArrayList<String>();

	public static SharedConnection conectarServidorArquivos(String ipServidorArquivos, String pastaCompartilhada) {
		// this.ipServidorArquivos = ipServidorArquivos;
		// this.pastaCompartilhada = pastaCompartilhada;

		// Conecta num servidor de arquivos que permite acesso público.
		AuthenticationContext authenticationContext = AuthenticationContext.anonymous();
		System.out.println("Conectando à pasta compartilhada \\\\" + ipServidorArquivos + "\\" + pastaCompartilhada);
		try {
			SharedConnection sharedConnection = new SharedConnection(ipServidorArquivos, pastaCompartilhada,
					authenticationContext);
			System.out.println("Conectado à pasta compartilhada \\\\" + ipServidorArquivos + "\\" + pastaCompartilhada);
			return sharedConnection;
		} catch (IOException e) {
			System.out.println("\nNão foi possível se conectar à pasta compartilhada.");
			return null;
		}
	}

	public static void listarAppliances(String ipServidorArquivos, String pastaCompartilhada) {

		// pastaCompartilhada.trim
		
		SharedConnection conexaoCompartilhada = conectarServidorArquivos(ipServidorArquivos, pastaCompartilhada);
		if (conexaoCompartilhada != null) {
			SharedDirectory diretorioRaiz = new SharedDirectory(conexaoCompartilhada);
			for (SharedFile arquivoCompartilhado : diretorioRaiz.getFiles()) {
				System.out.println("\nLista de Appliance(s) :\n" + arquivoCompartilhado.getName());
			}
		} else {
			System.out.println("Não foi possível listar os arquivos.");
		}
	}

	public static VirtualBoxManager conectarWS(String ipHost) {

		// Cada instância de VirtualBoxManager é um host com o VirtualBox
		VirtualBoxManager manager = VirtualBoxManager.createInstance(null);

		String porta = "18083";
		String url = "http://" + ipHost + ":" + porta;
		System.out.println("url = " + url);
		String usuario = null;
		String senha = null;
		// IWebsessionManager wsm = new IWebsessionManager()

		System.out.println("\nConectando em " + url + " com usuário " + usuario + " e senha " + senha + "...");
		try {
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

	public static void desconectarWS(VirtualBoxManager manager) {
		// manager.cleanup();
		try {
			manager.disconnect();
		} catch (VBoxException e) {
			System.out.println("\nNão foi possível se desconectar do webservice.");
		}
	}

	// IP - online - capacidade - (margem)
	// USO FINAL = (Memória utilizado + Memória Appliance)/Memória Total
	// {String IP, mem_utilizada, mem_total} - uso_mem_final, mem_appliance

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

		// if (a >= b && c >= d) {
		if (a >= b) {
			return true;
		} else
			return false;

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
				// for (String s : m.getVRDEServer().getVRDEProperties()) {
				// System.out.println("Propriedade: " + s);
				// Listar as propriedades do servidor VRDE
				// System.out.println("Propriedade: " + m.getVRDEServer().getVRDEProperty(s));
				// }
			} else {
				System.out.println("VRDE desabilitado.");
			}
		}

	}

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
	public class Host {
		long tamanho_memoria_disponivel;
		long tamanho_memoria_total;
		boolean status;
		String ipAddress;

		Host(String ipAddress) {

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

	public String oberIPPeloHostname(String hostname) {
		try {
			NbtAddress address = NbtAddress.getByName(hostname);
			return address.getHostAddress();
		} catch (UnknownHostException e) {
			System.out.println("Não pode resolver o endereço IP do host " + hostname);
			return null;
		}
	}

	// o worst fit, que deixa muita memória sobrando, seria, no nosso caso, o best
	// fit,
	// pois deixa mais memória pro usuário

	// o best fit, que deixa pouca memória sobrando, seria, no nosso caso, o worst
	// fit
	// pois deixa pouca memória pro usuário

	public String getParametros() {
		return parametros;
	}

	// o first fit, no nosso caso, apenas pegaria o primeiro com memória disponível,
	// sem considerar a atual utilização da máquina host
	public static void exibirTelaConvidado(String ipHost, String porta) {

		String parametros = "/v:" + ipHost + ":" + porta;
		Runtime runtime = Runtime.getRuntime();

		String[] aplicacao = new String[] { "c:\\Windows\\System32\\mstsc.exe", parametros };

		try {
			runtime.exec(aplicacao);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void limparConsole() {
		try {
			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static void exibirCabecalho() {
		limparConsole();
		System.out.println("########################################################");
		System.out.println("\t\t\t RemoteVM ");
		System.out.println("########################################################");
	}

	public static void pausa() {

		System.out.println("\nPressione Enter para continuar...");
		try {
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void importarParametros() {
		System.out.println("\nDigite o caminho completo do arquivo:");
		Scanner lerTerminal = new Scanner(System.in);
		String caminhoArquivo = lerTerminal.nextLine();
		List<String> lista = Collections.emptyList();
		try {
			lista = Files.readAllLines(Paths.get(caminhoArquivo), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int linhas = 0;
		for (String s : lista) {
			if (linhas == 0) {
				pastaCompartilhada = s;
			} else {
				ips.add(s);
			}
			linhas++;
		}
	}

	public static void exibirMenuPrincipal() {
		Scanner entrada = new Scanner(System.in);

		int opcao;
		while (true) {
			limparConsole();
			exibirCabecalho();
			System.out.println("\nSelecione uma opção:");
			System.out.println("1) Adicionar máquina(s) hóspede(s) a partir de arquivo com endereços IPs");// importar
			System.out.println("2) Listar IPs cadastrados");
			System.out.println("2) Adicionar máquina hóspede a partir de endereço IP");// adicionar endereço ip de
			System.out.println("3) Editar pool de máquinas");
			System.out.println("4) Descrever pool de máquina(s)");
			System.out.println("5) Exibir endereço do servidor de arquivos");
			System.out.println("6) Listar appliances hospedados no servidor de arquivos");
			System.out.println("7) Implantar / Excluir VM");
			System.out.println("8) Ligar/Desligar VM");
			System.out.println("9) Exibir tela remota da VM");
			System.out.println("10) Sair do programa");
			System.out.println("");
			System.out.print("Entre o número para selecionar uma opção:\r\n");

			opcao = entrada.nextInt();

			switch (opcao) {
			case 1:
				importarParametros();
				System.out.println("Pasta Compartilhada: " + pastaCompartilhada);
				System.out.println("IPs: ");
				for (String ipip : ips) {
					System.out.println(ipip);
				}

				pausa();
				break;

			case 2:
				System.out.println("IPs cadastrados: ");
				for (String s : ips) {
					System.out.println(s);
				}
				pausa();
				break;
			case 5:
				System.out.println("Pasta Compartilhada: " + pastaCompartilhada);
				pausa();
			default:
				break;
			}
		}

	}

	public static void exibirParametros() {
//		if (args.length != 1 || args[0] == "-?") {
		System.out.println("Uso: java RemoteVM <argumentos>"
				+ "\n -h <arquivo>.txt \t\t - Arquivo texto com a lista de hosts executando o webservice."
				+ "\n -i <nome appliance>\t\t - Importar Appliance" + "\n -l <nome vm) \t\t\t - Ligar VM"
				+ "\n -d <nome vm> \t\t\t - Desligar VM" + "\n -e <nome vm> \t\t\t - Excluir VM"
				+ "\n -s <nome vm> \t\t\t - Exibir tela da VM");
		System.exit(1);
	}

	public static void main(String[] args) throws java.io.IOException, InterruptedException {

		exibirMenuPrincipal();

		// objetivo = alta disponibilidade de servidores web ngix com várias VMS
		// Exibir console - conectar, ver as vms, ver se está ativona VM, e obter a
		// porta
		// Copiar pros hosts sem precisar ir um por um.

		// Se não estiver ativo, ativar. Se a porta já existir, substituir.
		//
		// listarAppliances(ipServidorArquivos, pastaCompartilhada);

		VirtualBoxManager gerente = conectarWS("10.1.1.4");

//		IVirtualBox vBoxSVC;
//		if (gerente!=null) {
//			vBoxSVC=gerente.getVBox();
//			listarVM(vBoxSVC);
//		}
		exibirTelaConvidado("10.1.1.4", "4489");

		List<String> ipsHosts = ips;
		System.out.println("\nIPs dos hosts cadastrados:" + ipsHosts);
		HashMap<String, List<String>> hosts = new HashMap<>();

		for (String s : ipsHosts) {
			VirtualBoxManager manager = conectarWS(s);

			// hashmap
			try {
				// IVirtualBox = VBoxSVC.exe = Processo que controla tudo independente da
				// interface.
				IVirtualBox vBoxSVC = manager.getVBox();

				if (vBoxSVC != null) {

					String memoriatotalhostemMB = getMemoriaTotalHostEmMB(vBoxSVC);
					String memoriadisponivelhostemMB = getMemoriaDisponivelHostEmMB(vBoxSVC);
					String[] memoria = { memoriatotalhostemMB, memoriadisponivelhostemMB };
					hosts.put(s, Arrays.asList(memoria));
					// System.out.println("VirtualBox version: " + vBoxSVC.getVersion() + "\n");
					// detalharAppliance(vBoxSVC, ipServidorArquivos, pastaCompartilhada,
					// "Ubuntu18.04.1_1.0.ova");
					detalharHost(vBoxSVC);
					listarVM(vBoxSVC);
					// System.out.println("getDiscoAppliance: " +
					// getTamanhoDiscoVirtualAppliance(vBoxSVC, "F:\\Apple.ova"));
					// verificarDisponibilidadeHost(vBoxSVC, manager, "F:\\Apple.ova");
					// desligarVM(manager, vBoxSVC, "Ubuntu18.04.1 1");
					// implantarAppliance(manager, vBoxSVC,
					// "\\\\10.1.1.4\\Teste\\Ubuntu18.04.1_1.0.ova");
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