package learning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public class BeamSearch {
	QGenSequence sequence;
	QGenFactorGraph graph;
	QGenPotentialFunction model;
	public PriorityQueue<Beam> beams;
	int beamSize;
	
	public BeamSearch(QGenSequence sequence, QGenFactorGraph graph,
			QGenPotentialFunction model, int beamSize) {
		this.sequence = sequence;
		this.graph = graph;
		this.model = model;
		this.beamSize = beamSize;
		this.beams = new PriorityQueue<Beam>(beamSize, new BeamComparator());
		run();
	}
	
	private void run() {
		int seqLength = QGenSlots.numSlots;
		beams.add(new Beam(new int[] {0, 0}, 0.0));
		ArrayList<Beam> newBeams = new ArrayList<Beam>();
		for (int i = 0; i < seqLength; i++) {
			newBeams.clear();
			while (!beams.isEmpty()) {
				Beam beam = beams.poll();
				for (int s = 0; s < model.iterator[i][0]; s++) {
					int[] ids = Arrays.copyOf(beam.ids, beam.ids.length + 1);
					int n = ids.length - 1;
					ids[n] = s;
					int clique = model.getCliqueId(i, s, ids[n-1], ids[n-2]);
					double loglik = beam.loglik + graph.cliqueScores[i][clique];
					newBeams.add(new Beam(ids, loglik));
				}
			}
			for (Beam beam : newBeams) {
				beams.add(beam);
				if (beams.size() > beamSize) {
					beams.poll();
				}
			}
		}
	}
	
	public PriorityQueue<Beam> getTopK(int topK) {
		PriorityQueue<Beam> topBeams = new PriorityQueue<Beam>(topK,
				new BeamComparator());
		for (Beam beam : beams) {
			topBeams.add(beam);
			if (topBeams.size() > topK) {
				topBeams.poll();
			}
		}
		return topBeams;
	}
	
	class Beam {
		public int[] ids;
		public double loglik;
		Beam(int[] id, double loglik) {
			this.ids = id;
			this.loglik = loglik;
		}
	}
	
	class BeamComparator implements Comparator<Beam> {
		@Override
		public int compare(Beam x, Beam y) {
			if (x.loglik < y.loglik) {
				return -1;
			}
			return x.loglik > y.loglik ? 1 : 0;
		}
	}
}
