package com.jokahobby.modules.zone;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;

    @PostConstruct
    @Transactional
    public void initZoneData() throws IOException {
        if(zoneRepository.count() == 0) {
            Resource resource = new ClassPathResource("zones_kr_fi.csv");
            List<Zone> zoneList = Files.readAllLines(resource.getFile().toPath(), StandardCharsets.UTF_8).stream()
                    .map(line -> {
                        String[] split = line.split(",");
                        return Zone.builder().country(split[0])
                                .city(split[1])
                                .localNameOfCity(split[2])
                                .province(split[3])
                                .build();
                    }).toList();
            zoneRepository.saveAll(zoneList);
        }
    }

    public Zone findByCityAndProvince(String city, String province) {
        return zoneRepository.findByCityAndProvince(city, province)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    public Zone findByZoneName(String zoneName) {
        String[] parts = zoneName.split("/");
        if (parts.length < 2) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String cityPart = parts[1];
        String city = cityPart.contains("(") ? cityPart.substring(0, cityPart.indexOf("(")) : cityPart;
        String province = parts.length >= 3 ? parts[2] : "none";
        return findByCityAndProvince(city, province);
    }
}
