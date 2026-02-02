package com.jokahobby.modules.account.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class Profile {

    @Length(max = 50)
    private String bio;

    @Length(max = 50)
    private String url;

    private String location;

    private String profileImage;

}
