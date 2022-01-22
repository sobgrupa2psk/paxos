package pl.psk.paxos.model;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import lombok.Data;

@Data
public class Server implements Runnable {

	private int port;
	private ServerDetails serverDetails;
	private ObservableList<Server> serverList;
	private BooleanProperty iAmLeader;
	private LongProperty term;
	private IntegerProperty beatCount;
	private IntegerProperty noLeaderCount;
	private Button killButton;
	private Button ex1Button;
	private Button ex2Button;
	private Timer timer;
	private Future<?> thread;

	public Server(int port, ObservableList<Server> serverList) {
		this.port = port;
		this.serverList = serverList;
		this.iAmLeader = new SimpleBooleanProperty(false);
		this.beatCount = new SimpleIntegerProperty(0);
		this.noLeaderCount = new SimpleIntegerProperty(0);
		this.term = new SimpleLongProperty(0);

		killButton = new Button("Kill");
		ex1Button = new Button("Trigger Vote");
		ex2Button = new Button("Set beatcount 10");

		killButton.setOnAction(e -> {
			System.out.println("Zabijam serwer!");
			this.serverList.remove(this);
			thread.cancel(true);
			this.timer.cancel();
		});


		ex1Button.setOnAction(e -> {
			System.out.println("EX 1!");
			try {
				triggerVote();
			} catch (Exception exception) {
				System.out.println("Something went wrong." + exception.getMessage());
			}
		});


		ex2Button.setOnAction(e -> {
			System.out.println("EX 2!");
			try {
				this.setBeatCount(10);
			} catch (Exception exception) {
				System.out.println("Something went wrong." + exception.getMessage());
			}
		});
	}

	public boolean isiAmLeader() {
		return iAmLeader.get();
	}

	public void setiAmLeader(boolean iAmLeader) {
		this.iAmLeader.set(iAmLeader);
	}

	public BooleanProperty iAmLeaderProperty() {
		return iAmLeader;
	}

	public int getBeatCount() {
		return beatCount.get();
	}

	public void setBeatCount(int beatCount) throws Exception {
		if (beatCount > 4) {
			throw new Exception("beatCount over 4");
		}
		this.beatCount.set(beatCount);
	}

	public IntegerProperty beatCountProperty() {
		return beatCount;
	}

	public int getNoLeaderCount() {
		return noLeaderCount.get();
	}

	public void setNoLeaderCount(int noLeaderCount) throws Exception {
		this.noLeaderCount.set(noLeaderCount);
	}

	public IntegerProperty noLeaderCountProperty() {
		return noLeaderCount;
	}

	public long getTerm() {
		return term.get();
	}

	public void setTerm(long term) {
		this.term.set(term);
	}

	public LongProperty termProperty() {
		return term;
	}

	public void initServerDetails() {
		serverDetails = new ServerDetails(port, serverList);
	}

	// paxos t mod n = s == true
	private boolean mayIBeLeader() {
		return serverDetails.getIdValue() == serverDetails.getTermValue() % serverList.size();
	}

	// higher term id wins election
	private boolean iAmBetterLeader(ServerDetails otherServerID) {
		return otherServerID.getTermValue() > this.serverDetails.getTermValue();
	}



	@Override
	public void run() {
		TimerTask task = new TimerTask() {
			public void run() {

				System.out.printf("Beat!: %s, %s, %s %n ", serverDetails, beatCount, iAmLeader);
				if (iAmLeader.getValue()) {
					otherServersStream().forEach(Server::heartBeat);
					beatCount.setValue(0);
				} else {
					serverDetails.incrementTerm();
					beatCount.setValue(beatCount.getValue() - 1);
					if (beatCount.getValue() < 0) {
						try {
							triggerVote();
						} catch (Exception e) {
							System.out.println("Something went wrong." + e.getMessage());
						}
						beatCount.setValue(beatCount.getValue() + 1); // celowe zwiekszenie
					}
				}
				term.setValue(serverDetails.getTermValue());
			}
		};
		this.timer = new Timer("Timer" + serverDetails.getIdValue(), true);
		this.timer.scheduleAtFixedRate(task, 1000, 1000);

	}

	public void setThread(Future<?> executorService) {
		this.thread = executorService;
	}


	private void triggerVote() throws Exception {

		if (beatCount.getValue() > 0) {
			throw new Exception("Beat count over 0 exception");
		}

		if (mayIBeLeader()) {
			boolean iAmLeader = otherServersStream()
					.map(otherServer -> otherServer.considerCandidate(this.serverDetails))
					.allMatch(votingResult -> votingResult.equals(serverDetails));
			this.iAmLeader.setValue(iAmLeader);
			System.out.printf("Jestem serverem %s , kandydowałem na lidera. I wynik to: %s%n", serverDetails, this.iAmLeader);
			if (iAmLeader==true) {
				otherServersStream().forEach(Server::youAreNotLeaderAnyMore);
			}
		}
	}

	private void youAreNotLeaderAnyMore() {
		this.iAmLeader.setValue(false);
	}

	private boolean areYouLeader() {
		return this.iAmLeader.getValue();
	}

	private void heartBeat() {
		beatCount.setValue(4);
	}

	private Stream<Server> otherServersStream() {
		return serverList.stream().filter(otherServer -> !otherServer.equals(this));
	}

	public ServerDetails considerCandidate(ServerDetails otherServerID) {
		if(this.serverDetails.getTermValue() < otherServerID.getTermValue()){
			this.serverDetails.incrementTerm();
			return otherServerID;
		}
		if(this.serverDetails.getTermValue() == otherServerID.getTermValue()){
			return otherServerID;
		}
		return this.serverDetails;
	}
}
