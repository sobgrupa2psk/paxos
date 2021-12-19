package pl.psk.paxos.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import pl.psk.paxos.model.Server;

public class MainController {

	private ObservableList<Server> serverList = FXCollections.observableArrayList();
	@FXML
	private TableView<Server> serverTable;

	@FXML
	public void initialize() throws InterruptedException {

		serverTable.setItems(serverList);

		ExecutorService executorService = Executors.newFixedThreadPool(6);
		for (int port = 8080; port < 8086; port++) {
			Server server = new Server(port, serverList);
			serverList.add(server);
			server.initServerDetails();
			executorService.submit(server);
			server.setThread(executorService);
		}

	}

}