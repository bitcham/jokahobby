package com.jokahobby.api.service;

import com.jokahobby.api.dto.request.*;
import com.jokahobby.api.dto.response.*;
import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyService;
import com.jokahobby.modules.hobby.HobbySortType;
import com.jokahobby.modules.hobby.event.HobbyCreatedEvent;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.tag.TagService;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.zone.ZoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class HobbyApplicationService {

    private final HobbyService hobbyService;
    private final TagService tagService;
    private final ZoneService zoneService;
    private final ApplicationEventPublisher eventPublisher;

    // ===== Public endpoints =====

    @Transactional(readOnly = true)
    public Page<HobbyListResponse> getPublishedHobbies(String country, String city, HobbySortType sort, Pageable pageable) {
        Page<Hobby> hobbies = hobbyService.findPublished(country, city, sort, pageable);
        return hobbies.map(hobby -> {
            List<Tag> tags = hobbyService.getTags(hobby);
            return HobbyListResponse.from(hobby, tags);
        });
    }

    @Transactional(readOnly = true)
    public Page<HobbyListResponse> searchHobbies(String keyword, Pageable pageable) {
        Page<Hobby> hobbies = hobbyService.findByKeyword(keyword, pageable);
        return hobbies.map(hobby -> {
            List<Tag> tags = hobbyService.getTags(hobby);
            return HobbyListResponse.from(hobby, tags);
        });
    }

    @Transactional(readOnly = true)
    public HobbyResponse getHobbyDetail(String path, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        List<Tag> tags = hobbyService.getTags(hobby);
        List<Zone> zones = hobbyService.getZones(hobby);

        boolean isManager = account != null && hobbyService.isManager(hobby, account);
        boolean isMember = account != null && hobbyService.isMember(hobby, account);
        boolean isJoinable = account != null && hobbyService.isJoinable(hobby, account);

        return HobbyResponse.from(hobby, tags, zones, isManager, isMember, isJoinable);
    }

    @Transactional(readOnly = true)
    public HobbyMembersResponse getHobbyMembers(String path) {
        Hobby hobby = hobbyService.getHobby(path);
        List<Account> managers = hobbyService.getManagers(hobby);
        List<Account> members = hobbyService.getMembers(hobby);
        return HobbyMembersResponse.from(managers, members);
    }

    public HobbyResponse createHobby(HobbyCreateRequest request, Account account) {
        Hobby hobby = hobbyService.createNewHobby(request.toEntity(), account);
        log.info("Hobby created path={}", hobby.getPath());
        List<Tag> tags = hobbyService.getTags(hobby);
        List<Zone> zones = hobbyService.getZones(hobby);
        return HobbyResponse.from(hobby, tags, zones, true, false, false);
    }

    public void joinHobby(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyForUpdate(path);
        if (!hobbyService.isJoinable(hobby, account)) {
            throw new BusinessException(ErrorCode.HOBBY_NOT_JOINABLE);
        }
        hobbyService.addMember(hobby, account);
        log.info("Member joined hobby path={}", path);
    }

    public void leaveHobby(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyForUpdate(path);
        if (!hobbyService.isMember(hobby, account)) {
            throw new BusinessException(ErrorCode.HOBBY_NOT_MEMBER);
        }
        hobbyService.removeMember(hobby, account);
        log.info("Member left hobby path={}", path);
    }

    public void deleteHobby(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.remove(hobby);
        log.info("Hobby deleted path={}", path);
    }

    // ===== Settings endpoints (manager only) =====

    @Transactional(readOnly = true)
    public HobbySettingsResponse getHobbySettings(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        List<Tag> tags = hobbyService.getTags(hobby);
        List<Zone> zones = hobbyService.getZones(hobby);
        List<Account> managers = hobbyService.getManagers(hobby);
        List<Account> members = hobbyService.getMembers(hobby);
        return HobbySettingsResponse.from(hobby, tags, zones, managers, members);
    }

    public void updateDescription(String path, Account account, HobbyDescriptionUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.updateHobbyDescription(hobby, request.shortDescription(), request.fullDescription());
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby description updated"));
        log.info("Hobby description updated path={}", path);
    }

    public void updateBanner(String path, Account account, HobbyBannerUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.updateHobbyImage(hobby, request.image());
        log.info("Hobby banner updated path={}", path);
    }

    public void enableBanner(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.enableHobbyBanner(hobby);
        log.info("Hobby banner enabled path={}", path);
    }

    public void disableBanner(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.disableHobbyBanner(hobby);
        log.info("Hobby banner disabled path={}", path);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getHobbyTags(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        return hobbyService.getTags(hobby).stream().map(TagResponse::from).toList();
    }

    public void addHobbyTag(String path, Account account, String tagTitle) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Tag tag = tagService.findOrCreateNew(tagTitle);
        hobbyService.addTag(hobby, tag);
        log.info("Hobby tag added path={} tag={}", path, tagTitle);
    }

    public void removeHobbyTag(String path, Account account, String tagTitle) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Tag tag = tagService.findByTitle(tagTitle);
        hobbyService.removeTag(hobby, tag);
        log.info("Hobby tag removed path={} tag={}", path, tagTitle);
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getHobbyZones(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        return hobbyService.getZones(hobby).stream().map(ZoneResponse::from).toList();
    }

    public void addHobbyZone(String path, Account account, String zoneName) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Zone zone = zoneService.findByZoneName(zoneName);
        hobbyService.addZone(hobby, zone);
        log.info("Hobby zone added path={} zone={}", path, zoneName);
    }

    public void removeHobbyZone(String path, Account account, String zoneName) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Zone zone = zoneService.findByZoneName(zoneName);
        hobbyService.removeZone(hobby, zone);
        log.info("Hobby zone removed path={} zone={}", path, zoneName);
    }

    public void publish(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.publish(hobby);
        eventPublisher.publishEvent(new HobbyCreatedEvent(hobby));
        log.info("Hobby published path={}", path);
    }

    public void close(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.close(hobby);
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby closed"));
        log.info("Hobby closed path={}", path);
    }

    public void startRecruit(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.startRecruit(hobby);
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby recruitment started"));
        log.info("Hobby recruit started path={}", path);
    }

    public void stopRecruit(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.stopRecruit(hobby);
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby recruitment stopped"));
        log.info("Hobby recruit stopped path={}", path);
    }

    public void updatePath(String path, Account account, HobbyPathUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.updateHobbyPath(hobby, request.newPath());
        log.info("Hobby path updated oldPath={}, newPath={}", path, request.newPath());
    }

    public void updateTitle(String path, Account account, HobbyTitleUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.updateHobbyTitle(hobby, request.newTitle());
        log.info("Hobby title updated path={}", path);
    }
}
