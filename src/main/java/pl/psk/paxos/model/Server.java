package pl.psk.paxos.model;

import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import lombok.Data;

@Data
public class Server implements Runnable {

	private int port;
	private ServerDetails serverDetails;
	private ObservableList<Server> serverList;
	private boolean iAmLeader;
	private int beatCount;
	private Button killButton;

	public Server(int port, ObservableList<Server> serverList) {
		this.port = port;
		this.serverList = serverList;
		killButton = new Button("Kill");
		killButton.setOnAction(e -> {
			System.out.println("Zabijam serwer!");
		});
	}

	public void initServerDetails() {
		serverDetails = new ServerDetails(port, serverList);
	}

	private boolean mayIBeLeader() {
		return serverDetails.getIdValue() == serverDetails.getTermValue() % serverList.size();
	}

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
				System.out.printf("Beat!: %s, %s %n ", serverDetails, beatCount);
				if (iAmLeader) {
					otherServersStream().forEach(otherServer -> otherServer.hearthBeat());
				} else {
					if (beatCount-- < 0) {
						triggerVote();
						beatCount = beatCount + 2;
					}
				}
			}
		};
		Timer timer = new Timer("Timer" + serverDetails.getIdValue(), true);
		timer.scheduleAtFixedRate(task, 10000, 10000);
		//todo Zabijanie serwera
		//todo wprowadzenia błędu (3)
		// Jakis UI troche wiecej
	}

	private void triggerVote() {
		if (mayIBeLeader()) {
			iAmLeader = otherServersStream().map(otherServer -> otherServer.considerCandidate(this.serverDetails)).allMatch(votingResult -> votingResult.equals(serverDetails));
			System.out.printf("Jestem serverem %s , kandydowałem na lidera. I wynik to: %s%n", serverDetails, iAmLeader);
		}
	}

	private void hearthBeat() {
		beatCount = 4;
	}

	private Stream<Server> otherServersStream() {
		return serverList.stream().filter(otherServer -> !otherServer.equals(this));
	}

	public ServerDetails considerCandidate(ServerDetails otherServerID) {
		if (mayIBeLeader() && iAmBetterLeader(otherServerID)) {
			return this.serverDetails;
		}
		return otherServerID;
	}
}
