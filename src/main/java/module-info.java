module com.beid.belgianeidauth {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.bouncycastle.provider;
    requires org.apache.logging.log4j;


    opens com.beid.belgianeidauth to javafx.fxml;
    exports com.beid.belgianeidauth;
    exports com.beid.belgianeidauth.util;
    opens com.beid.belgianeidauth.util to javafx.fxml;
}