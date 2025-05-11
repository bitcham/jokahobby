package com.jokahobby.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @EqualsAndHashCode(of = "id")
@Builder @AllArgsConstructor @NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"city", "province"}))
public class Zone {

    @Id @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String city;

    @Column(nullable = true)
    private String localNameOfCity;

    @Column(nullable = true)
    private String province;

    @Override
    public String toString() {
        if (localNameOfCity.equals("none") || localNameOfCity.isBlank()) {
            if(province.equals("none") || province.isBlank()) {
                return String.format("%s/%s", country, city);
            }
            return String.format("%s/%s/%s", country ,city, province);
        }

        if(province.equals("none") || province.isBlank()) {
            return String.format("%s/%s(%s)", country, city, localNameOfCity);
        }

        return String.format("%s/%s(%s)/%s", country, city, localNameOfCity, province);
    }
}
