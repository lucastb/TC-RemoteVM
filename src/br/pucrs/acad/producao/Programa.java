package br.pucrs.acad.producao;

//	feature						effort		user-loveit		revenue
//  priorizar por recurso		2			 	
//	parâmetros					1			
//	parametro exibir console	
//	parametro serv arquivos		
//	parametro lista ips			
//	editar pool de máquinas
//	implantar - first fit, last fit...
//	implantar vários. do 1º da lista ao 3 ou todos
//	verificar memória disponível

//	Objetivo => Implementar três vms com apache e conectar nelas
//	
//	https://docs.oracle.com/cd/E17802_01/webservices/webservices/docs/2.0/tutorial/doc/JAXWS3.html
// a

import java.io.IOException;
import java.util.regex.*;
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

import org.slf4j.Logger;
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

public class Programa {

	//	IMachine ISession
	//	Unlocked =

	//	The callers session object can then be used as a sort of remote control to the VM process that
	//	was launched. It contains a console object (see ISession::console) with which the VM can be
	//	paused, stopped, snapshotted or other things.

	// O objeto sessão tem um objeto console.

	public static String ipServidorArquivos;
	public static String pastaCompartilhada = null;
	public static List<String> enderecosIPv4Computadores = new ArrayList<String>();
	public static List<Computador> computadores = new ArrayList<Computador>();
	public static ServidorArquivos sa;
	public static String formatodescricaovm = "%-15s%-15s%-15s%-15s%-10s%-10s%-10s%-20s%s%n";

	public static SharedConnection conectarServidorArquivos(String ipServidorArquivos, String pastaCompartilhada) {
		// this.ipServidorArquivos = ipServidorArquivos;
		// this.pastaCompartilhada = pastaCompartilhada;

		// Conecta num servidor de arquivos que permite acesso público.
		AuthenticationContext authenticationContext = AuthenticationContext.anonymous();

		// System.out.println("Conectando à pasta compartilhada \\\\" +
		// ipServidorArquivos + "\\" + pastaCompartilhada);
		try {
			SharedConnection sharedConnection = new SharedConnection(ipServidorArquivos, pastaCompartilhada,
					authenticationContext);
			// System.out.println("Conectado à pasta compartilhada \\\\" +
			// ipServidorArquivos + "\\" + pastaCompartilhada);
			return sharedConnection;
		} catch (IOException e) {
			System.out.println("\nNão foi possível se conectar à pasta compartilhada.");
			return null;
		}
	}

	public static List<Appliance> listAppliances(ServidorArquivos sa) {
		return sa.getAppliances();
	}

	public static List<String> listarArquivosNoServidor(ServidorArquivos sa) {

		List<String> arquivos = new ArrayList<String>();

		if (sa.getIpServidorArquivos() != null && sa.getPastaCompartilhada() != null) {

			SharedConnection conexaoCompartilhada = conectarServidorArquivos(sa.getIpServidorArquivos(),sa.getPastaCompartilhada());

			if (conexaoCompartilhada != null) {

				SharedDirectory diretorioraiz = new SharedDirectory(conexaoCompartilhada);

				for (SharedFile arquivoCompartilhado : diretorioraiz.getFiles()) {

					arquivos.add(arquivoCompartilhado.getName());

				}

				return arquivos;

			} else {

				System.out.println("Não foi possível estabelecer a conexão com o diretório compartilhado.");

				return arquivos;
			}

		} else {

			System.out.println(

					"Não foi possível estabelecer a conexão com o diretório compartilhado. Parâmetros incorretos.");

			return arquivos;
		}
	}

	public static void listarAppliances(ServidorArquivos sa) {
		List<String> lista = listarArquivosNoServidor(sa);
		if (!lista.isEmpty()) {
			System.out.println("\nLista de Appliances(s): \n");
			for (String appliance : lista) {
				System.out.println(appliance);
			}
		} else {
			System.out.println();
		}
	}

	public static VirtualBoxManager conectarWS(String ipHost) {

		// Cada instância de VirtualBoxManager é um host com o VirtualBox
		VirtualBoxManager manager = VirtualBoxManager.createInstance(null);

		String porta = "18083";
		String url = "http://" + ipHost + ":" + porta;
		//		System.out.println("url = " + url);
		String usuario = null;
		String senha = null;
		// IWebsessionManager wsm = new IWebsessionManager()

		// System.out.println("\nConectando em " + url + "...");
		try {
			manager.connect(url, usuario, senha);
			// System.out.println("\nCliente conectado com sucesso em " + url + ".");

		} catch (VBoxException e) {
		}
		manager.waitForEvents(0);
		return manager;
	}

	/* * 14.10.2019 - API do WebService do VirtualBox nóo tem um mótodo que exibe o
	 * espaco livre em disco.
	 * public static long getEspacoLivreDiscoHost(IVirtualBox
	 * vBoxSVC) { // 08.10.2019 - esta pegando o espaóo em disco do host local, ao
	 * invós do remoto ISystemProperties isp = vBoxSVC.getSystemProperties();
	 * System.out.println("getDefaultMachineFolder " +
	 * isp.getDefaultMachineFolder()); String unidade =
	 * isp.getDefaultMachineFolder().substring(0, 1);
	 * 
	 * File file = new File(unidade + ":\\");
	 * 
	 * // System.out.println("Espaóo Livre: " + file.getFreeSpace()); // // double
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

	public static void describeAllAppliances(ServidorArquivos sa) {

		String formato = "%-25s%-25s%-25s%-25s%-25s%s%n";

		System.out.printf(formato, "Nº", "Arquivo", "Descricao", "S.O.", "Memoria (MB)", "Nucleos de CPU");

		// Assume-se que cada appliance contem somente uma marquina virtual

		for (Appliance a : sa.getAppliances()) {
			System.out.printf(formato, a.getId(), a.getNomeArquivo(), a.getDescricao().get(0), a.getOS().get(0),
					a.getMemoria().get(0), a.getNucleosDeCPU().get(0));
		}
	}

	public static void descreverUmAppliance(IVirtualBox vbox, String ipServidorArquivos, String pastaCompartilhada,
			String nomeAppliance) {

		String caminho = "\\\\" + ipServidorArquivos + "\\" + pastaCompartilhada + "\\" + nomeAppliance;
		System.out.println("\nDetalhes do Appliance: " + caminho);

		try {
			IAppliance appliance = vbox.createAppliance();
			appliance.read(caminho);
			appliance.interpret();

			// After calling this method
			// one can inspect the virtualSystemDescriptions[] array attribute,
			// which will then contain one IVirtualSystemDescription
			// for each virtual machine found in the appliance.

			List<IVirtualSystemDescription> listavsd = appliance.getVirtualSystemDescriptions();

			for (IVirtualSystemDescription ivsd : listavsd) {
				// System.out.println("getCount: " + ivsd.getCount()); // 13
				// https://github.com/OpenCyberChallengePlatform/OccpAdmin/blob/master/occp/edu/uri/dfcsc/occp/OccpVBoxHV.java

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
						+ "\nNócleos de CPU: "
						+ ivsd.getValuesByType(VirtualSystemDescriptionType.CPU, VirtualSystemDescriptionValueType.Auto)
						+ "\nImagem de Disco: " + ivsd.getValuesByType(VirtualSystemDescriptionType.HardDiskImage,
								VirtualSystemDescriptionValueType.Auto));
			}

			List<String> avisos = appliance.getWarnings();
			for (String s : avisos) {
				System.out.println("Aviso: " + s);
			}

			// List<String> lista = appliance.getDisks();
			//
			// for (String s : lista) {
			// String[] arrOfStr = s.split("\t\t", 0);
			// int i = 0;
			// for (String a : arrOfStr) {
			// i++;
			// if (i == 2) {
			// System.out.println("Capacidade Maxima do disco (Bytes): " + a);
			// }
			// }
			// }

		}

		catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}
	}

	public static boolean cadastrarAppliancesnoServidor(ServidorArquivos sa, Computador c) {
		// lista os arquivos disponibilizados na pasta compartilhada

		List<String> arquivos = listarArquivosNoServidor(sa);

		for (String arquivo : arquivos) {
			System.out.println("Arquivo: " + arquivo);
		}

		System.out.println("c.getipv4 = " + c.getIpv4());
		VirtualBoxManager temp = conectarWS(c.getIpv4());

		IVirtualBox vBoxSVC = temp.getVBox();

		if (temp != null) {

			for (String arquivo : arquivos) {

				String caminho = "\\\\" + ipServidorArquivos + "\\" + pastaCompartilhada + "\\" + arquivo;
				try {
					IAppliance appliance = vBoxSVC.createAppliance();
					appliance.read(caminho);
					appliance.interpret();

					// After calling this method
					// one can inspect the virtualSystemDescriptions[] array attribute,
					// which will then contain one IVirtualSystemDescription
					// for each virtual machine found in the appliance.

					List<IVirtualSystemDescription> listavsd = appliance.getVirtualSystemDescriptions();
					List<String> nome;
					List<String> os;
					List<String> memoria;
					List<String> nucleosdecpu;

					for (IVirtualSystemDescription ivsd : listavsd) {
						// System.out.println("getCount: " + ivsd.getCount()); // 13
						// https://github.com/OpenCyberChallengePlatform/OccpAdmin/blob/master/occp/edu/uri/dfcsc/occp/OccpVBoxHV.java
						nome = ivsd.getValuesByType(VirtualSystemDescriptionType.Name,
								VirtualSystemDescriptionValueType.Original);
						os = ivsd.getValuesByType(VirtualSystemDescriptionType.OS,
								VirtualSystemDescriptionValueType.Auto);
						memoria = ivsd.getValuesByType(VirtualSystemDescriptionType.Memory,
								VirtualSystemDescriptionValueType.Auto);
						nucleosdecpu = ivsd.getValuesByType(VirtualSystemDescriptionType.CPU,
								VirtualSystemDescriptionValueType.Auto);

						sa.adicionarAppliance(new Appliance(arquivo, nome, os, memoria, nucleosdecpu, sa));
					}
				} catch (VBoxException e) {
					System.out.println("Erro: " + e);
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public static boolean implantarAppliance(IVirtualBox vbox, String caminho) {

		// https://crunchify.com/why-and-for-what-should-i-use-enum-java-enum-examples/

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
					System.out.println("Percentual pronto: " + p.getPercent());
				} catch (InterruptedException e) {
					System.out.println("Erro: " + e);
					return false;
				}
			}
			System.out.println("Appliance importado com sucesso.");
			return p.getCompleted();
		} catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}
		return false;

		//		 progressBar(manager, p, 10000);

	}

	public static boolean verificarDisponibilidadeHost(IVirtualBox vBoxSVC, String ipServidorArquivos,
			String pastaCompartilhada, String caminho) {

		int a, b = 0;
		a = Integer.parseInt(getMemoriaDisponivelHostEmMB(vBoxSVC));
		b = Integer.parseInt(
				getMemoriaAppliance(vBoxSVC, ipServidorArquivos, pastaCompartilhada, "Ubuntu18.04.1_1.0.ova"));

		/*
		 * 
		 * 14.10.2019 - API do WebService do VirtualBox nóo tem um mótodo que exibe o
		 * espaóo livre em disco.
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
		System.out.println("Quantidade de VMs = " + lista.size());

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
				System.out.println("VM " + name + " já está no estado" + m.getState().name() + ".");
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

	static void desligarVM(VirtualBoxManager manager, IVirtualBox vBoxSVC, String machine) {

		try {
			IMachine m = vBoxSVC.findMachine(machine);

			String name = m.getName();
			// 12.03.2020 - No futuro, alterar para verificar por outros estados da VM, antes de desligá-la.
			if (m.getState().name() != "PoweredOff") {

				System.out.println("\nDesligando VM '" + name + "' ...");

				ISession session = manager.getSessionObject();

				vBoxSVC.findMachine(machine).lockMachine(session, LockType.Shared);

				IConsole iConsole = session.getConsole();

				IProgress p = iConsole.powerDown();

				progressBar(manager, p, 10000);

				if (p.getCompleted()) {
					System.out.println("VM '" + name + "' desligada.");
				}
			}
		}

		catch (VBoxException e) {
			System.out.println("Erro: " + e);
		}

	}

	public static boolean removerVM(IVirtualBox vBoxSVC, String machine) {

		// if session not locked
		try {
			IMachine m = vBoxSVC.findMachine(machine);

			CleanupMode cm = CleanupMode.Full;
			// Full deleta os HDS que estavam anexadas a VM, bem como as ISOs

			m.deleteConfig(m.unregister(cm));
			System.out.println("VM '" + machine +"' removida com sucesso.");
			return true;
		} catch (VBoxException e) {
			System.out.println("Erro: " + e);
			return false;
		}

	}

	public static boolean progressBar(VirtualBoxManager manager, IProgress p, long waitMillis) {
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

	public static void descreverTodosComputadores() {
		limparConsole();
		exibirCabecalho();
		if (!enderecosIPv4Computadores.isEmpty()) {
			String linha1 = "%-15s%-12s%-12s%-12s%-12s%-12s%-12s%-12s%s%n";
			String linha2 = "%-15s%-12s%-12s%-12s%-12s%-12s%s%n";
			String linha3 = "%-15s%-12s%-12s%-12s%-12s%-12s%s%n";
			System.out.printf(linha1, "IP", "Status", "Memoria", "Memoria", "Memoria", "Uso de ", "Sistema", "Nucleos",
					"Threads");
			System.out.printf(linha2, "", "", "total", "utilizada", "disponível", "memoria", "Operacional", "Nucleos",
					"Threads");
			System.out.printf(linha3, "", "", "(MB)", "(MB)", "(MB)", "(%)", "");
			for (String ip : enderecosIPv4Computadores) {

				VirtualBoxManager manager = conectarWS(ip);
				try {
					IVirtualBox vBoxSVC = manager.getVBox();
					if (vBoxSVC != null) {
						IHost h = vBoxSVC.getHost();

						long tamanho_memoria_disponivel = h.getMemoryAvailable();

						long tamanho_memoria_total = h.getMemorySize();

						double c = (((double) tamanho_memoria_disponivel / tamanho_memoria_total) * -100.0) + 100.0;

						System.out.printf(linha1, ip, "Online", h.getMemorySize().toString(),
								(h.getMemorySize() - h.getMemoryAvailable()), h.getMemoryAvailable(), Math.round(c),
								h.getOperatingSystem(), h.getProcessorOnlineCoreCount(), h.getProcessorOnlineCount());

						desconectarWS(manager);
					} else {
						System.out.printf(linha1, ip, "Offline", "", "", "", "", "", "", "");
					}
				} catch (Exception e) {
					System.out.printf(linha1, ip, "Erro", "", "", "", "", "", "", "");
				}
			}
		} else {
			System.out.println("Lista de IPs está vazia.");
		}
	}

	static void descreverComputador(IVirtualBox vBoxSVC) {

		IHost h = vBoxSVC.getHost();

		long tamanho_memoria_disponivel = h.getMemoryAvailable();

		long tamanho_memoria_total = h.getMemorySize();

		double c = (((double) tamanho_memoria_disponivel / tamanho_memoria_total) * -100.0) + 100.0;

		System.out.println(

				"\nMemória total: " + h.getMemorySize() + " MB" + "\nMemória utilizada: "
						+ (h.getMemorySize() - h.getMemoryAvailable()) + " MB" + "\nMemória disponível: "
						+ h.getMemoryAvailable() + " MB" + "\nUso de Memória: " + Math.round(c) + " %"
						+ "\nSistema Operacional: " + h.getOperatingSystem() + "\nVersão: " + h.getOSVersion() +
						//				"\nNócleos de Processamento: " + h.getProcessorCoreCount() +
						//				"\nProcessadores Lógicos: " + h.getProcessorCount() +
						//				"\nNócleos de Processamento Online:" + h.getProcessorOnlineCoreCount() +
						//				"\nProcessadores Lógicos Online: " + h.getProcessorOnlineCount()
						"\nNúcleos de Processamento: " + h.getProcessorOnlineCoreCount() + "\nProcessadores Lógicos: "
						+ h.getProcessorOnlineCount());
	}

	public static String getMemoriaTotalHostEmMB(IVirtualBox vBoxSVC) {
		IHost h = vBoxSVC.getHost();
		return h.getMemorySize().toString();
	}

	public static String getMemoriaDisponivelHostEmMB(IVirtualBox vBoxSVC) {
		IHost h = vBoxSVC.getHost();
		return h.getMemoryAvailable().toString();
	}

	public static void descreverTodosAppliances(IVirtualBox vbox, String ipServidorArquivos,
			String pastaCompartilhada) {

		String formato = "%-25s%-25s%-25s%-25s%s%n";

		System.out.printf(formato, "Arquivo", "Nome", "S.O.", "Memoria (MB)", "Nucleos de CPU");

		// getAppliances retorna uma lista de nomes de Arquivos na pasta Compartilhada
		List<String> listaAppliances = listarArquivosNoServidor(sa);

		// armazena a quantidade de appliances para exibir no console pro usuário
		// selecionar

		for (String appliancefor : listaAppliances) {
			// ...
			// incrementa o contador de appliances pra mostrar ao usuário, pra ele
			// selecionar

			String caminho = "\\\\" + ipServidorArquivos + "\\" + pastaCompartilhada + "\\" + appliancefor;
			while (true) {
				try {
					IAppliance appliance = vbox.createAppliance();
					appliance.read(caminho);
					appliance.interpret();

					//					After calling this method
					//					one can inspect the virtualSystemDescriptions[] array attribute,
					//					which will then contain	one IVirtualSystemDescription
					//					for each virtual machine found in the appliance.

					List<IVirtualSystemDescription> listavsd = appliance.getVirtualSystemDescriptions();
					List<String> nome;
					List<String> os;
					List<String> memoria;
					List<String> nucleosdecpu;

					for (IVirtualSystemDescription ivsd : listavsd) {
						//						System.out.println("getCount: " + ivsd.getCount()); // 13
						//			            https://github.com/OpenCyberChallengePlatform/OccpAdmin/blob/master/occp/edu/uri/dfcsc/occp/OccpVBoxHV.java
						nome = ivsd.getValuesByType(VirtualSystemDescriptionType.Name,
								VirtualSystemDescriptionValueType.Original);
						os = ivsd.getValuesByType(VirtualSystemDescriptionType.OS,
								VirtualSystemDescriptionValueType.Auto);
						memoria = ivsd.getValuesByType(VirtualSystemDescriptionType.Memory,
								VirtualSystemDescriptionValueType.Auto);
						nucleosdecpu = ivsd.getValuesByType(VirtualSystemDescriptionType.CPU,
								VirtualSystemDescriptionValueType.Auto);

						System.out.printf(formato, appliancefor, nome.get(0), os.get(0), memoria.get(0), nucleosdecpu.get(0));

						/*
						 * + "\nMemória (B)  : " +
						 * ivsd.getValuesByType(VirtualSystemDescriptionType.Memory,
						 * VirtualSystemDescriptionValueType.Original)
						 */
						//							  + "\nImagem de Disco: " +
						//							  ivsd.getValuesByType(VirtualSystemDescriptionType.HardDiskImage,
						//							  VirtualSystemDescriptionValueType.Auto)

						//					List<String> lista = appliance.getDisks();
						//							for (String s : lista) {
						//								String[] arrOfStr = s.split("\t\t", 0);
						//								int i = 0;
						//								for (String a : arrOfStr) {
						//									i++;
						//									if (i == 2) {
						//										System.out.println("Capacidade Móxima do disco (Bytes): " + a);
						//									}
						//								}
						//							}
					}

					List<String> avisos = appliance.getWarnings();
					for (String s : avisos) {
						System.out.println("Aviso: " + s);
					}

					// Tendo em vista que a API não me deixa ver o tamanho do espaço em disco
					// disponível
					// na máquina hóspede, a informação do tamanho do disco é um pouco irrelvante.
					break;
				}

				catch (VBoxException e) {
					// System.out.println("Erro: " + e);
					continue;
				}
			}
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

	public static void descreverVM(IVirtualBox vBoxSVC, String IPv4) {

		for (IMachine m : vBoxSVC.getMachines()) {
			System.out.printf(formatodescricaovm, IPv4, m.getName(), m.getState().toString(), m.getOSTypeId(),
					m.getMemorySize(), m.getCPUCount(), m.getVRDEServer().getEnabled(),
					IPv4 + ":" + m.getVRDEServer().getVRDEProperty("TCP/Ports"), m.getVRDEServer().getVRDEExtPack());
			// m.canShowConsoleWindow() // precisa sessão
		}
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
				String[] arrOfStr = s.split("\t\t", 0);
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

	// o worst fit, que deixa muita memória sobrando, seria, no nosso caso, o best
	// fit,
	// pois deixa mais memória pro usuório

	// o best fit, que deixa pouca memória sobrando, seria, no nosso caso, o worst
	// fit
	// pois deixa pouca memória pro usuório

	// o first fit, no nosso caso, apenas pegaria o primeiro com memória disponóvel,
	// sem considerar a atual utilizaóóo da móquina host
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
		System.out.println(
				"############################################################################################################");
		System.out.println("\t\t\t\t\t\t RemoteVM ");
		System.out.println(
				"############################################################################################################");
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
		System.out.println("\nInsira o caminho do arquivo de configuração:");

		Scanner lerTerminal = new Scanner(System.in);

		// Le o caminho completo e elimina possiveis aspas
		String caminhoArquivo = lerTerminal.nextLine().replace("\"", "");

		// String caminhoArquivo = lerTerminal.nextLine();
		List<String> lista = Collections.emptyList();

		try {
			lista = Files.readAllLines(Paths.get(caminhoArquivo), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int linhas = 0;
		for (String s : lista) {
			if (linhas == 0) {
				// Acha o ip do Servidor de Arquivos
				String pattern1 = "\\\\";
				String pattern2 = "\\";
				Pattern p = Pattern.compile(Pattern.quote(pattern1) + "(.*?)" + Pattern.quote(pattern2));
				Matcher m = p.matcher(s);
				while (m.find()) {
					ipServidorArquivos = m.group(1);
				}
				p = Pattern.compile(Pattern.quote(pattern2) + "(.*?)" + Pattern.quote(pattern2));
				m = p.matcher(s);
				while (m.find()) {
					pastaCompartilhada = m.group(1);
				}
				pastaCompartilhada = s.substring(s.lastIndexOf("\\") + 1);

			} else {
				enderecosIPv4Computadores.add(s);
			}
			linhas++;
		}
	}

	public static void exibirMenu() {
		Scanner entrada = new Scanner(System.in);

		int opcao;
		while (true) {
			limparConsole();
			exibirCabecalho();
			System.out.println("1) Importar arquivo de configuração"); // OK
			System.out.println("2) Listar pool de máquinas"); // OK
			System.out.println("3) Editar pool de máquinas");
			System.out.println("4) Descrever pool de máquinas");
			System.out.println("5) Exibir endereço do servidor de arquivos");// OK
			System.out.println("6) Descrever appliances hospedados no servidor de arquivos");
			System.out.println("7) Implantar VM");
			System.out.println("8) Excluir VM");
			System.out.println("9) Descrever VMs");
			System.out.println("10) Ligar VM");
			System.out.println("11) Pausar VM"); // Falta implementar
			System.out.println("12) Desligar VM");
			System.out.println("13) Exibir tela remota da VM");
			System.out.println("14) Sair do programa");
			System.out.println("");
			System.out.print("Entre o número para selecionar uma opção:\r\n");

			opcao = entrada.nextInt();

			switch (opcao) {
			case 1:
				importarParametros();
				System.out.println("Endereço IPv4 do Servidor de Arquivos: " + ipServidorArquivos);
				System.out.println("Diretório Compartilhado: " + pastaCompartilhada);

				sa = new ServidorArquivos(ipServidorArquivos, pastaCompartilhada);

				System.out.println("IPs: ");
				for (String ip : enderecosIPv4Computadores) {
					System.out.println(ip);
					computadores.add(new Computador(ip));
				}
				pausa();
				break;

			case 2:
				System.out.println("IPs cadastrados: ");
				for (Computador c : computadores) {
					System.out.println(c.getIpv4());
				}
				pausa();
				break;
			case 4:
				descreverTodosComputadores();
				pausa();
				break;

			case 5:
				System.out.println("Diretório Compartilhado:\n\\\\" + ipServidorArquivos + "\\" + pastaCompartilhada);
				//				
				pausa();
				break;
			case 6:
				// Tenta pegar uma instancia que esteja rodando do VirtualBox pra detalhar os
				// appliances
				// Percorre a lista dos ips em busca de appliances

				for (String s : enderecosIPv4Computadores) {
					VirtualBoxManager temp = conectarWS(s);
					IVirtualBox vBoxSVC = temp.getVBox();

					if (temp != null) {
						try {
							descreverTodosAppliances(vBoxSVC, ipServidorArquivos, pastaCompartilhada);
						} catch (Exception e) {
							System.out.println("Erro.");
						}
						pausa();
						break;
					} else {
						System.out.println("Não foi possível conectar à maquina com endereço IPv4: " + s);
					}
				}

				pausa();
				break;
				// Implantar Appliance
			case 7:
				descreverTodosComputadores();
				// lista os appliances disponíveis
				// Tenta pegar uma instancia que esteja rodando do VirtualBox pra detalhar os
				// appliances
				// Percorre a lista dos ips em busca de appliances

				Scanner scanner = new Scanner(System.in);
				System.out.println("Digite o endereço IPv4 do computador:");
				String ip = scanner.nextLine();
				VirtualBoxManager temp = conectarWS(ip);
				IVirtualBox vBoxSVC = temp.getVBox();

				if (temp != null) {
					try {
						descreverTodosAppliances(vBoxSVC, ipServidorArquivos, pastaCompartilhada);
						// receber nome VM que será copiada
						scanner = new Scanner(System.in);
						System.out.println("Digite o nome do arquivo:");
						String arquivo = scanner.nextLine();
						arquivo = "\\\\" + ipServidorArquivos + "\\" + pastaCompartilhada + "\\" + arquivo;
						System.out.println("Arquivo = " + arquivo);
						implantarAppliance(vBoxSVC, arquivo);
					} catch (Exception e) {
						System.out.println("Erro.");
					}
					pausa();
					break;
				} else {
					System.out.println("Não foi possível conectar à maquina com endereço IPv4: " + ip);
				}

				pausa();
				break;
				// Remover VM
			case 8:
				System.out.printf(formatodescricaovm, "Host", "Nome", "Estado", "S.O.", "Memoria", "Nucleos", "Desktop",
						"Endereco Desktop", "Pacote de");
				System.out.printf(formatodescricaovm, "", "", "", "", "(MB)", "de CPU", "Remoto", "Remoto", "extensao");

				for (String iptmp : enderecosIPv4Computadores) {
					VirtualBoxManager vbm = conectarWS(iptmp);
					IVirtualBox ivb = vbm.getVBox();
					descreverVM(ivb, iptmp);
				}

				Scanner lerTerminal = new Scanner(System.in);
				System.out.println("Digite o endereço IPv4 do computador hóspede:");
				String host = lerTerminal.nextLine();
				VirtualBoxManager vbmtemp = conectarWS(host);
				if (vbmtemp != null) {
					try {
						System.out.println("Digite o nome da VM:");
						String vmname = lerTerminal.nextLine();
						IVirtualBox vBoxSVCtmp = vbmtemp.getVBox();

						// Se a sessão estiver ativa, não será possível remover a VM.
						ISession session = vbmtemp.getSessionObject();

						IMachine im = vBoxSVCtmp.findMachine(vmname);
						if ( im.getState().equals(im.getState().Running)) {
							desligarVM(vbmtemp, vBoxSVCtmp, vmname);
							// Espera 5 segundos para liberar a sessão da VM
							Thread.sleep(5000);							
						}
						removerVM(vBoxSVCtmp, vmname);
					} catch (Exception e) {
						System.out.println("Erro.");
					}
					pausa();
					break;
				} else {
					System.out.println("Não foi possível conectar ao computador com endereço IPv4: " + host);
				}
				pausa();
				break;
				// Listar VM
			case 9:
				System.out.printf(formatodescricaovm, "Host", "Nome", "Estado", "S.O.", "Memoria", "Nucleos", "Desktop",
						"Endereco Desktop", "Pacote de");
				System.out.printf(formatodescricaovm, "", "", "", "", "(MB)", "de CPU", "Remoto", "Remoto", "extensao");

				for (String iptmp : enderecosIPv4Computadores) {
					VirtualBoxManager vbm = conectarWS(iptmp);
					IVirtualBox ivb = vbm.getVBox();
					descreverVM(ivb, iptmp);
				}
				pausa();
				break;

				// Ligar VM
			case 10:
				System.out.printf(formatodescricaovm, "Host", "Nome", "Estado", "S.O.", "Memoria", "Nucleos", "Desktop",
						"Endereco Desktop", "Pacote de");
				System.out.printf(formatodescricaovm, "", "", "", "", "(MB)", "de CPU", "Remoto", "Remoto", "extensao");

				for (String iptmp : enderecosIPv4Computadores) {
					VirtualBoxManager vbm = conectarWS(iptmp);
					IVirtualBox ivb = vbm.getVBox();
					descreverVM(ivb, iptmp);
				}

				lerTerminal = new Scanner(System.in);
				System.out.println("Digite o endereço IPv4 do computador hóspede:");
				host = lerTerminal.nextLine();

				VirtualBoxManager vbm = conectarWS(host);
				IVirtualBox ivb = vbm.getVBox();

				if (vbm != null) {
					try {
						System.out.println("Digite o nome da VM:");
						String vmname = lerTerminal.nextLine();

						ligarVM(vbm, ivb, vmname);
						//							System.out.println("Deseja exibir a tela da VM? (S/N)");
						//
						//							maquinavirtual = lerTerminal.nextLine();
						//
						//							if (maquinavirtual == "S") {
						////								 exibirTelaConvidado(ipHost, porta);
						//							}
					} catch (Exception e) {
						System.out.println("Erro.");
					}
					pausa();
					break;
				} else {
					System.out.println("Não foi possível conectar ao computador com endereço IPv4: " + host);
				}
				pausa();
				break;

				// Desligar VM
			case 12:
				System.out.printf(formatodescricaovm, "Host", "Nome", "Estado", "S.O.", "Memoria", "Nucleos", "Desktop",
						"Endereco Desktop", "Pacote de");
				System.out.printf(formatodescricaovm, "", "", "", "", "(MB)", "de CPU", "Remoto", "Remoto", "extensao");

				for (String iptmp : enderecosIPv4Computadores) {
					VirtualBoxManager vbmtemp1 = conectarWS(iptmp);
					IVirtualBox vBoxSVCtmp1 = vbmtemp1.getVBox();
					descreverVM(vBoxSVCtmp1, iptmp);
				}

				System.out.println("Digite o endereço IPv4 do computador hóspede:");
				lerTerminal = new Scanner(System.in);
				host = lerTerminal.nextLine();

				VirtualBoxManager vbmtemp1 = conectarWS(host);

				if (vbmtemp1 != null) {
					try {
						System.out.println("Digite o nome da VM:");
						String vm = lerTerminal.nextLine();

						IVirtualBox vBoxSVCtmp1 = vbmtemp1.getVBox();
						desligarVM(vbmtemp1, vBoxSVCtmp1, vm);
					} catch (Exception e) {
						System.out.println("Erro.");
					}
					pausa();
					break;
				} else {
					System.out.println("Não foi possível conectar ao computador com endereço IPv4: " + host);
				}
				pausa();
				break;
			case 13:
				Scanner scanner1 = new Scanner(System.in);
				System.out.println("Digite o endereço IPv4 da máquina remota:");
				String IPv4 = scanner1.nextLine();
				System.out.println("Digite a porta remota:");
				String porta = scanner1.nextLine();
				exibirTelaConvidado(IPv4, porta);
				pausa();
				break;
			case 14:
				System.exit(0);
				pausa();
				break;
			}
		}
	}

	public static void main(String[] args) throws java.io.IOException, InterruptedException {

		exibirMenu();

		// objetivo = alta disponibilidade de servidores web ngix com várias VMS
		// Exibir console - conectar, ver as vms, ver se está ativona VM, e obter a
		// porta
		// Copiar pros hosts sem precisar ir um por um.

		// Se não estiver ativo, ativar. Se a porta ja existir, substituir.
		//
		// listarAppliances(ipServidorArquivos, pastaCompartilhada);

		//		VirtualBoxManager gerente = conectarWS("10.1.1.4");

		//		IVirtualBox vBoxSVC;
		//		if (gerente!=null) {
		//			vBoxSVC=gerente.getVBox();
		//			listarVM(vBoxSVC);
		//		}
		//		exibirTelaConvidado("10.1.1.4", "4489");

		System.out.println("\nIPs dos hosts cadastrados:" + enderecosIPv4Computadores);
		HashMap<String, List<String>> hosts = new HashMap<>();

		for (String s : enderecosIPv4Computadores) {
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
					descreverComputador(vBoxSVC);
					listarVM(vBoxSVC);
					// System.out.println("getDiscoAppliance: " +
					// getTamanhoDiscoVirtualAppliance(vBoxSVC, "F:\\Apple.ova"));
					// verificarDisponibilidadeHost(vBoxSVC, manager, "F:\\Apple.ova");
					// desligarVM(manager, vBoxSVC, "Ubuntu18.04.1 1");
					// implantarAppliance(manager, vBoxSVC,
					// "\\\\10.1.1.4\\teste\\Ubuntu18.04.1_1.0.ova");
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