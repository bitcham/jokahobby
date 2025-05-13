package com.jokahobby.zone;

import com.jokahobby.domain.Zone;
import lombok.Data;

@Data
public class ZoneForm {

    private String zoneName;

    public String getCountryName() {
        return zoneName.split("/")[0];
    }

    public String getCityName() {
        String[] parts = zoneName.split("/");
        String cityPart = parts[1];
        return cityPart.contains("(") ? cityPart.substring(0, cityPart.indexOf("(")) : cityPart;
    }

    public String getLocalNameOfCity() {
        String[] parts = zoneName.split("/");
        String cityPart = parts[1];
        if (cityPart.contains("(") && cityPart.contains(")")) {
            return cityPart.substring(cityPart.indexOf("(") + 1, cityPart.indexOf(")"));
        }
        return "none";
    }

    public String getProvinceName() {
        String[] parts = zoneName.split("/");
        if (parts.length == 3) {
            return parts[2];
        }
        return "none";
    }


    public Zone getZone() {
        return Zone.builder()
                .country(this.getCountryName())
                .city(this.getCityName())
                .localNameOfCity(this.getLocalNameOfCity())
                .province(this.getProvinceName()).build();

    }
}
