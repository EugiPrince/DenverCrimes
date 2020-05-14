package it.polito.tdp.crimes.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import it.polito.tdp.crimes.db.EventsDao;

public class Model {
	
	private EventsDao dao;
	private Graph<String, DefaultWeightedEdge> graph;
	//NON USO IDENTITYMAP PERCHE' I VERTICI SONO DELLE STRINGHE, CHE HANNO GIA' UN LORO HASHCODE  EQUALS, QUINDI
	//NON C'E' IL RISCHIO DI CREARE OGGETTI PIU' VOLTI SENZA CHE VENGANO CONTROLLATI, NON SUCCEDE PERCHE' LE STRINGHE
	//RISOLVONO 'DA SOLE' QUESTI PROBLEMI
	private List<String> best;
	
	public Model() {
		this.dao = new EventsDao();
	}
	
	public List<Integer> getMesi() {
		return dao.getMesi();
	}
	
	public List<String> getCategorie() {
		return dao.getCategorie();
	}
	
	public void creaGrafo(String categoria, Integer mese) {
		this.graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		
		List<Adiacenza> adiacenze = this.dao.getAdiacenze(categoria, mese);
		for(Adiacenza a : adiacenze) {
			if(!this.graph.containsVertex(a.getV1()))
				this.graph.addVertex(a.getV1());
			if(!this.graph.containsVertex(a.getV2()))
				this.graph.addVertex(a.getV2());
			
			//E' non orientato, quindi controlliamo che se c'e' gia' l'arco v1 v2, allora non creo v2, v1
			//Creo l'arco solo se ancora non c'e'
			if(this.graph.getEdge(a.getV1(), a.getV2())==null)
				Graphs.addEdgeWithVertices(this.graph, a.getV1(), a.getV2(), a.getPeso());
			
		}
		
		System.out.println(String.format("Grafo creato con %d vertici e %d archi", this.graph.vertexSet().size(),
				this.graph.edgeSet().size()));
	}
	
	public List<Arco> getArchi() {
		//Prima di tutto calcolo il peso medio, perche' stampo solo gli archi con peso maggiore di quello medio
		double pesoMedio = 0;
		for(DefaultWeightedEdge e : this.graph.edgeSet())
			pesoMedio += this.graph.getEdgeWeight(e);
		pesoMedio = pesoMedio / this.graph.edgeSet().size();
		
		//Lista che contiene solo quelli che soddisfano il vincolo
		List<Arco> archi = new ArrayList<>();
		for(DefaultWeightedEdge e : this.graph.edgeSet())
			if(this.graph.getEdgeWeight(e) > pesoMedio)
				archi.add(new Arco(this.graph.getEdgeSource(e), this.graph.getEdgeTarget(e), this.graph.getEdgeWeight(e)));
		
		Collections.sort(archi);
		return archi;
	}
	
	/**
	 * Metodo che imposta la ricorsione
	 * @param sorgente
	 * @param destinazione
	 * @return
	 */
	public List<String> trovaPercorso(String sorgente, String destinazione) {
		//Inizio della ricorsione, parziale per tenere traccia della soluzione parziale e best sara' la soluzione
		List<String> parziale = new ArrayList<>();
		this.best = new ArrayList<>();
		
		parziale.add(sorgente); //Mettiamo il nodo di partenza, da qua iniziera' la ricorsione
		
		trovaRicorsivo(destinazione, parziale, 0);
		return this.best;
	}

	/**
	 * Metodo ricorsivo
	 * @param destinazione
	 * @param parziale
	 * @param L (livello che in realta' non serve!!!)
	 */
	private void trovaRicorsivo(String destinazione, List<String> parziale, int L) {
		//Caso terminale?
		//Quando l'ultimo vertice inserito in parziale coincide con la destinazione, non devo proseguire oltre,
		//controllero' solo se il percorso trovato e' migliore del best che attualmente si ha
		if(parziale.get(parziale.size()-1).equals(destinazione)) {
			//Se e' migliore di best allora la sovrascrivo
			if(parziale.size() > this.best.size())
				this.best = new ArrayList<>(parziale);
			
			return; //Termino la ricorsione
		}
		
		//Se non sono nel caso terminale, scorro i vicini dell'ultimo vertice inserito in parziale, e per ogni vicino
		//provo a metterlo nel percorso, lancio la procedura ricorsiva e faccio backtracking
		for(String vicino : Graphs.neighborListOf(this.graph, parziale.get(parziale.size()-1))) {
			//Siccome stiamo cercando cammini ACICLICI, dobbiamo fare attenzione di non visitare lo stesso nodo piu' volte
			//quindi controllo che il nodo non sia gia' in parziale
			if(!parziale.contains(vicino)) {
				//provo ad aggiungere
				parziale.add(vicino);
				this.trovaRicorsivo(destinazione, parziale, L+1);
				
				//backtracking
				parziale.remove(parziale.size()-1);
			}
		}
	}
}
