module org.subshare.updater.gui {

    requires transitive javafx.controls;
    requires transitive javafx.fxml;

//    requires transitive org.slf4j;

    requires transitive org.subshare.updater;

    exports org.subshare.updater.gui;
    exports org.subshare.updater.gui.console;
}