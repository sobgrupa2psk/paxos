package pl.psk.paxos.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javafx.collections.ObservableList;
import lombok.Data;
import lombok.ToString;

@Data
public class ServerDetails {

	private final AtomicInteger id = new AtomicInteger(0);
	private final AtomicLong term = new AtomicLong(0);
	private final AtomicLong commitIndex = new AtomicLong(0);

	private final int serverPort;

	@ToString.Exclude()
	private ObservableList<Server> serverList;

	public ServerDetails(int serverPort, ObservableList<Server> serverList) {
		this.serverPort = serverPort;
		this.serverList = serverList;

		int serverId = calculateServerId(serverPort);
		id.set(serverId);
		incrementTerm();
	}

	public void incrementTerm() {
		long nextTerm = calculateNextTerm();
		System.out.printf("New term: %d%n", nextTerm);
		term.set(nextTerm);
	}

	private long calculateNextTerm() {
		long currentTerm = getTermValue();
		currentTerm++;

		while (currentTerm % countServers() != getIdValue()) {
			currentTerm++;
		}
		return currentTerm;
	}

	public int calculateServerId(int port) {
		return port % countServers();
	}

	private int countServers() {
		return serverList.size();
	}

	public long getIdValue() {
		return getId().get();
	}

	public long getTermValue() {
		return getTerm().get();
	}

	public long getCommitIndexValue() {
		return getCommitIndex().get();
	}
}

