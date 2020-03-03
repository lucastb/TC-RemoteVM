package br.pucrs.acad.producao;
import java.util.ArrayList;
import java.util.List;

public class Computador {
	private static ArrayList<VM> appliances;
	private String status;
	private String ipv4;
	private long memoriatotal;
	private long memoriautilizada;
	private long memoriadisponivel;
	private double c = (((double) memoriadisponivel / memoriatotal) * -100.0) + 100.0;
	private String os;
	private String osversao;
	private long nucleos;
	private long threads;

	public Computador(String ipv4) {
		this.appliances = new ArrayList<>();	
		this.ipv4=ipv4;
	}

	public String getIpv4() {
		return ipv4;
	}

	public void setIpv4(String ipv4) {
		this.ipv4 = ipv4;
	}
	
	
//	System.out.printf(linha1, ip, "Online", h.getMemorySize().toString(),
//			(h.getMemorySize() - h.getMemoryAvailable()), h.getMemoryAvailable(), Math.round(c),
//			h.getOperatingSystem(), h.getProcessorOnlineCoreCount(),
//			h.getProcessorOnlineCount());
	
	
}
