package com.jokahobby.settings;

import com.jokahobby.account.AccountService;
import com.jokahobby.account.CurrentUser;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Tag;
import com.jokahobby.settings.form.*;
import com.jokahobby.settings.validator.NicknameValidator;
import com.jokahobby.settings.validator.PasswordFormValidator;
import com.jokahobby.tag.TagRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final AccountService accountService;
    private final ModelMapper modelMapper;
    private final NicknameValidator nicknameValidator;
    private final TagRepository tagRepository;

    static final String SETTINGS_PROFILE_URL = "/settings/profile";
    static final String SETTINGS_PROFILE_VIEW = "settings/profile";
    static final String SETTINGS_PASSWORD_URL = "/settings/password";
    static final String SETTINGS_PASSWORD_VIEW = "settings/password";
    static final String SETTINGS_NOTIFICATIONS_URL = "/settings/notifications";
    static final String SETTINGS_NOTIFICATIONS_VIEW = "settings/notifications";
    static final String SETTINGS_ACCOUNT_URL = "/settings/account";
    static final String SETTINGS_ACCOUNT_VIEW = "settings/account";
    static final String SETTINGS_TAGS_URL = "/settings/tags";
    static final String SETTINGS_TAGS_VIEW = "settings/tags";



    @InitBinder("passwordForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(new PasswordFormValidator());
    }

    @InitBinder("nicknameForm")
    public void initBinderNickname(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(nicknameValidator);
    }

    @GetMapping(SETTINGS_PROFILE_URL)
    public String profileUpdateForm(@CurrentUser Account account, Model model){
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, Profile.class));
        return SETTINGS_PROFILE_VIEW ;
    }

    @PostMapping(SETTINGS_PROFILE_URL)
    public String profileUpdateSubmit(@CurrentUser Account account, @Valid @ModelAttribute Profile profile, Errors errors,
                                      Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PROFILE_VIEW;
        }
        accountService.updateProfile(account, profile);
        redirectAttributes.addFlashAttribute("message", "Your profile updated successfully.");
        return "redirect:" + SETTINGS_PROFILE_URL;

    }

    @GetMapping(SETTINGS_PASSWORD_URL)
    public String passwordUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new PasswordForm());
        return SETTINGS_PASSWORD_VIEW;
    }

    @PostMapping(SETTINGS_PASSWORD_URL)
    public String passwordUpdate(@CurrentUser Account account, @Valid PasswordForm passwordForm, Errors errors,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PASSWORD_VIEW;
        }

        accountService.updatePassword(account, passwordForm.getNewPasswordConfirm());
        redirectAttributes.addFlashAttribute("message", "Your password updated successfully.");
        return "redirect:" + SETTINGS_PASSWORD_URL;
    }

    @GetMapping(SETTINGS_NOTIFICATIONS_URL)
    public String notificationsForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, Notifications.class));
        return SETTINGS_NOTIFICATIONS_VIEW;
    }

    @PostMapping(SETTINGS_NOTIFICATIONS_URL)
    public String notificationsUpdate(@CurrentUser Account account, @Valid Notifications notifications, Errors errors,
                                      Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_NOTIFICATIONS_VIEW;
        }

        accountService.updateNotifications(account, notifications);
        redirectAttributes.addFlashAttribute("message", "Your notification settings updated successfully.");
        return "redirect:" + SETTINGS_NOTIFICATIONS_URL;
    }

    @GetMapping(SETTINGS_ACCOUNT_URL)
    public String accountUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, NicknameForm.class));
        return SETTINGS_ACCOUNT_VIEW;
    }

    @PostMapping(SETTINGS_ACCOUNT_URL)
    public String accountUpdate(@CurrentUser Account account, @Valid NicknameForm nicknameForm, Errors errors,
                                Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_ACCOUNT_VIEW;
        }

        accountService.updateNickname(account, nicknameForm.getNickname());
        redirectAttributes.addFlashAttribute("message", "Your nickname updated successfully.");
        return "redirect:" + SETTINGS_ACCOUNT_URL;
    }

    @PostMapping(SETTINGS_TAGS_URL + "/add" )
    @ResponseBody
    public ResponseEntity addTag(@CurrentUser Account account, @RequestBody TagForm tagForm){
        String title = tagForm.getTagTitle();
        Tag tag =  tagRepository.findByTitle(title).orElseGet(() -> tagRepository
                .save(Tag.builder()
                        .title(title)
                        .build()) );

        accountService.addTag(account, tag);

        return ResponseEntity.ok().build();
    }

    @GetMapping(SETTINGS_TAGS_URL)
    public String updateTags(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        Set<Tag> tags = accountService.getTags(account);
        model.addAttribute("tags", tags.stream().map(Tag::getTitle).collect(Collectors.toList()));
        return SETTINGS_TAGS_VIEW;
    }

    @PostMapping(SETTINGS_TAGS_URL + "/remove")
    @ResponseBody
    public ResponseEntity removeTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Optional<Tag> tag = tagRepository.findByTitle(title);
        if(tag.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        accountService.removeTag(account, tag.get());
        return ResponseEntity.ok().build();
    }






}
