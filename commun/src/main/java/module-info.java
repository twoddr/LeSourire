module lesourire.commun {
    exports com.lesourire.commun;
    exports com.lesourire.commun.dto;

    // Jackson (client) désérialise les DTO par réflexion
    opens com.lesourire.commun.dto to com.fasterxml.jackson.databind;
}
