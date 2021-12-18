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
	public void initialize() {
		serverTable.setItems(serverList);
		ExecutorService executorService = Executors.newFixedThreadPool(6);
		for (int i = 8080; i < 8086; i++) {
			Server server = new Server(i, serverList);
			serverList.add(server);
			server.initServerDetails();
			executorService.submit(server);
		}
	}

}