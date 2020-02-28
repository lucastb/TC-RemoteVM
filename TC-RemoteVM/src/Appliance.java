import java.util.List;

public class Appliance {
	private ServidorArquivos servidorarquivos;
	
	public ServidorArquivos getServidorarquivos() {
		return servidorarquivos;
	}

	public void setServidorarquivos(ServidorArquivos servidorarquivos) {
		this.servidorarquivos = servidorarquivos;
	}

	private static int count = 1;
	private int id;
	private String nomearquivo;
	private List<String> descricao;
	private List<String> os;
	private List<String> memoria;
	private List<String> nucleosdecpu;
	
	Appliance(String nomearquivo,List<String> nome, List<String> os, List<String> memoria, List<String> nucleosdecpu,ServidorArquivos sa) {
		id=count++;
		this.descricao = nome;
		this.os=os;
		this.memoria=memoria;
		this.nucleosdecpu=nucleosdecpu;
		this.servidorarquivos=sa;
	}
	
	public List<String> getNucleosDeCPU(){
		return nucleosdecpu;
	}
	
	public List<String> getMemoria(){
		return memoria;
	}
	public int getId() {
		return id;
	}
	
	public List<String> getDescricao() {
		return descricao;
	}
	
	public List<String> getOS() {
		return os;
	}
	
	public String getNomeArquivo() {
		return nomearquivo;
	}

	public void setNome(List<String> nome) {
		this.descricao = nome;
	}
	
	
}
