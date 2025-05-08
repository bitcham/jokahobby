package com.jokahobby.settings;

import com.jokahobby.domain.Account;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Notifications {

    private boolean hobbyCreatedByEmail;
    private boolean hobbyCreatedByWeb;
    private boolean hobbyEnrollmentResultByEmail;
    private boolean hobbyEnrollmentResultByWeb;
    private boolean hobbyUpdatedByEmail;
    private boolean hobbyUpdatedByWeb;

    public Notifications(Account account){
        this.hobbyCreatedByEmail = account.isHobbyCreatedByEmail();
        this.hobbyCreatedByWeb = account.isHobbyCreatedByWeb();
        this.hobbyEnrollmentResultByEmail = account.isHobbyEnrollmentResultByEmail();
        this.hobbyEnrollmentResultByWeb = account.isHobbyEnrollmentResultByWeb();
        this.hobbyUpdatedByEmail = account.isHobbyUpdatedByEmail();
        this.hobbyUpdatedByWeb = account.isHobbyUpdatedByWeb();
    }
}
