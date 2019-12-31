module org.subshare.gui {

    requires transitive javafx.controls;
    requires transitive javafx.fxml;

//    requires transitive org.slf4j;

    requires transitive org.subshare.ls.client;
    requires transitive org.subshare.ls.server.cproc;
    requires transitive org.subshare.rest.client;

}