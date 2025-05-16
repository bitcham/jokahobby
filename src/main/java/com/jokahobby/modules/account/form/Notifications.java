package com.jokahobby.modules.account.form;

import lombok.Data;

@Data
public class Notifications {

    private boolean hobbyCreatedByEmail;
    private boolean hobbyCreatedByWeb;
    private boolean hobbyEnrollmentResultByEmail;
    private boolean hobbyEnrollmentResultByWeb;
    private boolean hobbyUpdatedByEmail;
    private boolean hobbyUpdatedByWeb;

}
