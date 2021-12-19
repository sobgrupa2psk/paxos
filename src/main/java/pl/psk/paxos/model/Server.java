package pl.psk.paxos.model;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import lombok.Data;
import lombok.Getter;

@Data
public class Server implements Runnable {

	private int port;
	private ServerDetails serverDetails;
	private ObservableList<Server> serverList;
	private BooleanProperty iAmLeader;
	private LongProperty term;
	private IntegerProperty beatCount;
	private Button killButton;
	private ExecutorService executorService;
	private Timer timer;

	public Server(int port, ObservableList<Server> serverList) {
		this.port = port;
		this.serverList = serverList;
		this.iAmLeader = new SimpleBooleanProperty(false);
		this.beatCount = new SimpleIntegerProperty(0);
		this.term = new SimpleLongProperty(0);

		killButton = new Button("Kill");
		killButton.setOnAction(e -> {
			System.out.println("Zabijam serwer!");
			this.executorService.shutdown();
			this.serverList.remove(this);
			this.timer.cancel();
		});
	}

	public boolean isiAmLeader() {
		return iAmLeader.get();
	}

	public BooleanProperty iAmLeaderProperty() {
		return iAmLeader;
	}

	public void setiAmLeader(boolean iAmLeader) {
		this.iAmLeader.set(iAmLeader);
	}

	public int getBeatCount() {
		return beatCount.get();
	}

	public IntegerProperty beatCountProperty() {
		return beatCount;
	}

	public void setBeatCount(int beatCount) {
		this.beatCount.set(beatCount);
	}

	public long getTerm() {
		return term.get();
	}

	public LongProperty termProperty() {
		return term;
	}

	public void setTerm(long term) {
		this.term.set(term);
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
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			e.printStackTrace(); //todo
		}

		triggerVote();

		TimerTask task = new TimerTask() {
			public void run() {
				System.out.printf("Beat!: %s, %s, %s %n ", serverDetails, beatCount, iAmLeader);
				if (iAmLeader.getValue()) {
					otherServersStream().forEach(otherServer -> otherServer.hearthBeat());
					beatCount.setValue(0);
				} else {
					beatCount.setValue(beatCount.getValue()-1);
					if (beatCount.getValue() < 0) {
						triggerVote();
						beatCount.setValue(beatCount.getValue()+2); // celowe zwiekszenie
					}
				}
				term.setValue(serverDetails.getTermValue());
			}
		};
		this.timer = new Timer("Timer" + serverDetails.getIdValue(), true);
		this.timer.scheduleAtFixedRate(task, 5000, 5000);

		//todo Zabijanie serwera
		//todo wprowadzenia błędu (3)
		// Jakis UI troche wiecej
	}

	public void setThread(ExecutorService executorService) {
		this.executorService = executorService;
	}

	private void triggerVote() {
		if (mayIBeLeader()) {
			iAmLeader.setValue(otherServersStream().map(otherServer -> otherServer.considerCandidate(this.serverDetails)).allMatch(votingResult -> votingResult.equals(serverDetails)));
			System.out.printf("Jestem serverem %s , kandydowałem na lidera. I wynik to: %s%n", serverDetails, iAmLeader);
		}
	}

	private void hearthBeat() {
		beatCount.setValue(4);
	}

	private Stream<Server> otherServersStream() {
		return serverList.stream().filter(otherServer -> !otherServer.equals(this));
	}

	public ServerDetails considerCandidate(ServerDetails otherServerID) {
		if (mayIBeLeader() && iAmBetterLeader(otherServerID)) {
			System.out.println("A am better leader than you");
			return this.serverDetails;
		}
		return otherServerID;
	}
}
