package com.jokahobby.modules.account;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.form.Notifications;
import com.jokahobby.modules.account.form.Profile;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTagRepository accountTagRepository;
    private final AccountZoneRepository accountZoneRepository;

    @Transactional
    public void updateProfile(Account account, @Valid Profile profile) {
        account.updateProfile(profile.getBio(), profile.getUrl(), profile.getLocation(), profile.getProfileImage());
        accountRepository.save(account);
    }

    @Transactional
    public Account updateProfile(Account account, String bio, String url, String location, String profileImage) {
        account.updateProfile(bio, url, location, profileImage);
        return accountRepository.save(account);
    }

    @Transactional
    public void updateNotifications(Account account, @Valid Notifications notifications) {
        account.updateNotificationPreferences(
                notifications.isHobbyCreatedByEmail(), notifications.isHobbyCreatedByWeb(),
                notifications.isHobbyEnrollmentResultByEmail(), notifications.isHobbyEnrollmentResultByWeb(),
                notifications.isHobbyUpdatedByEmail(), notifications.isHobbyUpdatedByWeb()
        );
        accountRepository.save(account);
    }

    @Transactional
    public void updateNotifications(Account account, boolean hobbyCreatedByEmail, boolean hobbyCreatedByWeb,
                                    boolean hobbyEnrollmentResultByEmail, boolean hobbyEnrollmentResultByWeb,
                                    boolean hobbyUpdatedByEmail, boolean hobbyUpdatedByWeb) {
        account.updateNotificationPreferences(hobbyCreatedByEmail, hobbyCreatedByWeb,
                hobbyEnrollmentResultByEmail, hobbyEnrollmentResultByWeb,
                hobbyUpdatedByEmail, hobbyUpdatedByWeb);
        accountRepository.save(account);
    }

    @Transactional
    public void updateNickname(Account account, @NotBlank @Length(min = 3, max = 20) @Pattern(regexp = "^[a-zA-Z0-9가-힣äöåÄÖÅ]{3,20}$") String nickname) {
        account.updateNickname(nickname);
        accountRepository.save(account);
    }

    @Transactional
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

    @Transactional
    public void removeTag(Account account, Tag tag) {
        accountTagRepository.deleteByAccountAndTag(account, tag);
    }

    public List<Zone> getZones(Account account) {
        return accountZoneRepository.findAllByAccountId(account.getId()).stream()
                .map(AccountZone::getZone)
                .toList();
    }

    @Transactional
    public void addZone(Account account, Zone zone) {
        if (accountZoneRepository.existsByAccountAndZone(account, zone)) {
            return;
        }
        accountZoneRepository.save(AccountZone.builder()
                .account(account).zone(zone).build());
    }

    @Transactional
    public void removeZone(Account account, Zone zone) {
        accountZoneRepository.deleteByAccountAndZone(account, zone);
    }

    public Account getAccount(String nickname) {
        Account byNickname = accountRepository.findByNickname(nickname);
        if (byNickname == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return byNickname;
    }

    @Transactional
    public Account updateNicknameWithDuplicateCheck(Account account, String nickname) {
        if (accountRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        account.updateNickname(nickname);
        return accountRepository.save(account);
    }
}
