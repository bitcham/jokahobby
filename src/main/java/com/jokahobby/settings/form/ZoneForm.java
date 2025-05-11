package com.jokahobby.settings.form;

import com.jokahobby.domain.Zone;
import lombok.Data;

@Data
public class ZoneForm {

    private String zoneName;

    public String getCountryName() {
        return zoneName.substring(0, zoneName.indexOf("/"));
    }

    public String getCityName(){
        return zoneName.substring(zoneName.indexOf("/") + 1, zoneName.indexOf("("));
    }

    public String getLocalNameOfCity() {
        return zoneName.substring(zoneName.indexOf("(") + 1, zoneName.indexOf(")"));
    }

    public String getProvinceName() {
        return zoneName.substring(zoneName.lastIndexOf("/") + 1);
    }


    public Zone getZone() {
        return Zone.builder()
                .country(this.getCountryName())
                .city(this.getCityName())
                .localNameOfCity(this.getLocalNameOfCity())
                .province(this.getProvinceName()).build();

    }
}
