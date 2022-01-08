module pl.psk.paxos {
	requires javafx.controls;
	requires javafx.fxml;

	requires static lombok;

	opens pl.psk.paxos to javafx.fxml;
	exports pl.psk.paxos;
	exports pl.psk.paxos.controller;
	exports pl.psk.paxos.model;
	opens pl.psk.paxos.controller to javafx.fxml;
}