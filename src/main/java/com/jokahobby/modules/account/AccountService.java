package com.jokahobby.modules.account;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTagRepository accountTagRepository;
    private final AccountZoneRepository accountZoneRepository;

    public Account updateProfile(Account account, String bio, String url, String location, String profileImage) {
        account.updateProfile(bio, url, location, profileImage);
        return accountRepository.save(account);
    }

    public void updateNotifications(Account account, boolean hobbyCreatedByEmail, boolean hobbyCreatedByWeb,
                                    boolean hobbyEnrollmentResultByEmail, boolean hobbyEnrollmentResultByWeb,
                                    boolean hobbyUpdatedByEmail, boolean hobbyUpdatedByWeb) {
        account.updateNotificationPreferences(hobbyCreatedByEmail, hobbyCreatedByWeb,
                hobbyEnrollmentResultByEmail, hobbyEnrollmentResultByWeb,
                hobbyUpdatedByEmail, hobbyUpdatedByWeb);
        accountRepository.save(account);
    }

    public void addTag(Account account, Tag tag) {
        if (accountTagRepository.existsByAccountAndTag(account, tag)) {
            return;
        }
        accountTagRepository.save(AccountTag.builder()
                .account(account).tag(tag).build());
    }

    public List<Tag> getTags(Account account) {
        return accountTagRepository.findAllByAccountId(account.getId()).stream()
                .map(AccountTag::getTag)
                .toList();
    }

    public void removeTag(Account account, Tag tag) {
        accountTagRepository.deleteByAccountAndTag(account, tag);
    }

    public List<Zone> getZones(Account account) {
        return accountZoneRepository.findAllByAccountId(account.getId()).stream()
                .map(AccountZone::getZone)
                .toList();
    }

    public void addZone(Account account, Zone zone) {
        if (accountZoneRepository.existsByAccountAndZone(account, zone)) {
            return;
        }
        accountZoneRepository.save(AccountZone.builder()
                .account(account).zone(zone).build());
    }

    public void removeZone(Account account, Zone zone) {
        accountZoneRepository.deleteByAccountAndZone(account, zone);
    }

    public Account getAccount(String nickname) {
        return accountRepository.findByNickname(nickname)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    public Account updateNicknameWithDuplicateCheck(Account account, String nickname) {
        if (accountRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        account.updateNickname(nickname);
        return accountRepository.save(account);
    }
}
