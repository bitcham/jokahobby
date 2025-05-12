package com.jokahobby.hobby;

import com.jokahobby.domain.Account;
import com.jokahobby.domain.Hobby;
import com.jokahobby.hobby.form.HobbyDescriptionForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final ModelMapper modelMapper;

    public Hobby createNewHobby(Hobby hobby, Account account) {
        Hobby newHobby = hobbyRepository.save(hobby);
        newHobby.addManager(account);
        return newHobby;
    }

    public Hobby getHobby(String path) {
        Hobby hobby = hobbyRepository.findByPath(path);
        if (hobby == null) {
            throw new IllegalArgumentException("Hobby not found");
        }
        return hobby;
    }

    public Hobby getHobbyToUpdate(Account account, String path) {
        Hobby hobby = this.getHobby(path);
        if (!account.isManagerOf(hobby)) {
            throw new AccessDeniedException("You do not have permission to access this feature.");
        }
        return hobby;
    }

    public void updateHobbyDescription(Hobby hobby, @Valid HobbyDescriptionForm hobbyDescriptionForm) {
       modelMapper.map(hobbyDescriptionForm, hobby);
    }

    public void updateHobbyImage(Hobby hobby, String image) {
        hobby.setImage(image);
    }

    public void enableHobbyBanner(Hobby hobby) {
        hobby.setUseBanner(true);
    }

    public void disableHobbyBanner(Hobby hobby) {
        hobby.setUseBanner(false);
    }
}
