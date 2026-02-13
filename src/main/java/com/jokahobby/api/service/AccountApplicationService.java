package com.jokahobby.api.service;

import com.jokahobby.api.dto.request.NotificationUpdateRequest;
import com.jokahobby.api.dto.request.ProfileUpdateRequest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountService;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.tag.TagService;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.zone.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountApplicationService {

    private final AccountService accountService;
    private final TagService tagService;
    private final ZoneService zoneService;

    @Transactional(readOnly = true)
    public Account getPublicProfile(String nickname) {
        return accountService.getAccount(nickname);
    }

    public Account updateProfile(Account account, ProfileUpdateRequest request) {
        return accountService.updateProfile(account, request.bio(), request.url(), request.location(), request.profileImage());
    }

    public void updateNotifications(Account account, NotificationUpdateRequest request) {
        accountService.updateNotifications(account,
                request.hobbyCreatedByEmail(), request.hobbyCreatedByWeb(),
                request.hobbyEnrollmentResultByEmail(), request.hobbyEnrollmentResultByWeb(),
                request.hobbyUpdatedByEmail(), request.hobbyUpdatedByWeb());
    }

    public Account updateNickname(Account account, String nickname) {
        return accountService.updateNicknameWithDuplicateCheck(account, nickname);
    }

    @Transactional(readOnly = true)
    public List<Tag> getTags(Account account) {
        return accountService.getTags(account);
    }

    public void addTag(Account account, String tagTitle) {
        Tag tag = tagService.findOrCreateNew(tagTitle);
        accountService.addTag(account, tag);
    }

    public void removeTag(Account account, String tagTitle) {
        Tag tag = tagService.findByTitle(tagTitle);
        accountService.removeTag(account, tag);
    }

    @Transactional(readOnly = true)
    public List<Zone> getZones(Account account) {
        return accountService.getZones(account);
    }

    public void addZone(Account account, String zoneName) {
        Zone zone = zoneService.findByZoneName(zoneName);
        accountService.addZone(account, zone);
    }

    public void removeZone(Account account, String zoneName) {
        Zone zone = zoneService.findByZoneName(zoneName);
        accountService.removeZone(account, zone);
    }
}
