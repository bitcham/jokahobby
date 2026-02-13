package com.jokahobby.api.service;

import com.jokahobby.api.dto.request.*;
import com.jokahobby.api.dto.response.*;
import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyService;
import com.jokahobby.modules.hobby.HobbySortType;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.tag.TagService;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.zone.ZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class HobbyApplicationService {

    private final HobbyService hobbyService;
    private final TagService tagService;
    private final ZoneService zoneService;

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
        List<Tag> tags = hobbyService.getTags(hobby);
        List<Zone> zones = hobbyService.getZones(hobby);
        return HobbyResponse.from(hobby, tags, zones, true, false, false);
    }

    public void joinHobby(String path, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        if (!hobbyService.isJoinable(hobby, account)) {
            throw new BusinessException(ErrorCode.HOBBY_NOT_JOINABLE);
        }
        hobbyService.addMember(hobby, account);
    }

    public void leaveHobby(String path, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        if (!hobbyService.isMember(hobby, account)) {
            throw new BusinessException(ErrorCode.HOBBY_NOT_MEMBER);
        }
        hobbyService.removeMember(hobby, account);
    }

    public void deleteHobby(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.remove(hobby);
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
    }

    public void updateBanner(String path, Account account, HobbyBannerUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.updateHobbyImage(hobby, request.image());
    }

    public void enableBanner(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.enableHobbyBanner(hobby);
    }

    public void disableBanner(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.disableHobbyBanner(hobby);
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
    }

    public void removeHobbyTag(String path, Account account, String tagTitle) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Tag tag = tagService.findByTitle(tagTitle);
        hobbyService.removeTag(hobby, tag);
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
    }

    public void removeHobbyZone(String path, Account account, String zoneName) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Zone zone = zoneService.findByZoneName(zoneName);
        hobbyService.removeZone(hobby, zone);
    }

    public void publish(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.publish(hobby);
    }

    public void close(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.close(hobby);
    }

    public void startRecruit(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.startRecruit(hobby);
    }

    public void stopRecruit(String path, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.stopRecruit(hobby);
    }

    public void updatePath(String path, Account account, HobbyPathUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.updateHobbyPath(hobby, request.newPath());
    }

    public void updateTitle(String path, Account account, HobbyTitleUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        hobbyService.updateHobbyTitle(hobby, request.newTitle());
    }
}
