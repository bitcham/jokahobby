package com.jokahobby.modules.account;

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
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    public void updateProfile(Account account, @Valid Profile profile) {
        modelMapper.map(profile, account);
        accountRepository.save(account);
    }

    public void updateNotifications(Account account, @Valid Notifications notifications) {
        modelMapper.map(notifications, account);
        accountRepository.save(account);
    }

    public void updateNickname(Account account, @NotBlank @Length(min = 3, max = 20) @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$") String nickname) {
        account.setNickname(nickname);
        accountRepository.save(account);
    }

    public void addTag(Account account, Tag tag) {
        accountRepository.findById(account.getId()).ifPresent(a -> a.getTags().add(tag));
    }

    public Set<Tag> getTags(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getTags();
    }

    public void removeTag(Account account, Tag tag) {
        accountRepository.findById(account.getId()).ifPresent(a -> a.getTags().remove(tag));
    }

    public Set<Zone> getZones(Account account) {
        return accountRepository.findById(account.getId()).orElseThrow().getZones();
    }

    public void addZone(Account account, Zone zone) {
        accountRepository.findById(account.getId()).ifPresent(a -> a.getZones().add(zone));
    }

    public void removeZone(Account account, Zone zone) {
        accountRepository.findById(account.getId()).ifPresent(a -> a.getZones().remove(zone));
    }

    public Account getAccount(String nickname) {
        Account byNickname = accountRepository.findByNickname(nickname);
        if (byNickname == null) {
            throw new IllegalArgumentException(nickname + " is not a valid nickname.");
        }
        return byNickname;
    }
}
