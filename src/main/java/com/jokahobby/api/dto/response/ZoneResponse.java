package com.jokahobby.api.dto.response;

import com.jokahobby.modules.zone.Zone;

public record ZoneResponse(
        Long id,
        String country,
        String city,
        String localNameOfCity,
        String province
) {
    public static ZoneResponse from(Zone zone) {
        return new ZoneResponse(
                zone.getId(),
                zone.getCountry(),
                zone.getCity(),
                zone.getLocalNameOfCity(),
                zone.getProvince()
        );
    }
}
