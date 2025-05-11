package com.jokahobby.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokahobby.account.AccountService;
import com.jokahobby.account.CurrentUser;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Tag;
import com.jokahobby.domain.Zone;
import com.jokahobby.settings.form.*;
import com.jokahobby.settings.validator.NicknameValidator;
import com.jokahobby.settings.validator.PasswordFormValidator;
import com.jokahobby.tag.TagRepository;
import com.jokahobby.zone.ZoneRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final AccountService accountService;
    private final ModelMapper modelMapper;
    private final NicknameValidator nicknameValidator;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;
    private final ZoneRepository zoneRepository;

    static final String ROOT = "/";
    static final String SETTINGS = "settings";
    static final String PROFILE = "/profile";
    static final String PASSWORD = "/password";
    static final String NOTIFICATIONS = "/notifications";
    static final String ACCOUNT = "/account";
    static final String TAGS = "/tags";
    static final String ZONES = "/zones";



    @InitBinder("passwordForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(new PasswordFormValidator());
    }

    @InitBinder("nicknameForm")
    public void initBinderNickname(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(nicknameValidator);
    }

    @GetMapping(PROFILE)
    public String profileUpdateForm(@CurrentUser Account account, Model model){
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, Profile.class));
        return SETTINGS + PROFILE ;
    }

    @PostMapping(PROFILE)
    public String profileUpdateSubmit(@CurrentUser Account account, @Valid @ModelAttribute Profile profile, Errors errors,
                                      Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS + PROFILE;
        }
        accountService.updateProfile(account, profile);
        redirectAttributes.addFlashAttribute("message", "Your profile updated successfully.");
        return "redirect:/" + SETTINGS + PROFILE;

    }

    @GetMapping(PASSWORD)
    public String passwordUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new PasswordForm());
        return SETTINGS + PASSWORD;
    }

    @PostMapping(PASSWORD)
    public String passwordUpdate(@CurrentUser Account account, @Valid PasswordForm passwordForm, Errors errors,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS + PASSWORD;
        }

        accountService.updatePassword(account, passwordForm.getNewPasswordConfirm());
        redirectAttributes.addFlashAttribute("message", "Your password updated successfully.");
        return "redirect:/" + SETTINGS + PASSWORD;
    }

    @GetMapping(NOTIFICATIONS)
    public String notificationsForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, Notifications.class));
        return SETTINGS + NOTIFICATIONS;
    }

    @PostMapping(NOTIFICATIONS)
    public String notificationsUpdate(@CurrentUser Account account, @Valid Notifications notifications, Errors errors,
                                      Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS + NOTIFICATIONS;
        }

        accountService.updateNotifications(account, notifications);
        redirectAttributes.addFlashAttribute("message", "Your notification settings updated successfully.");
        return "redirect:/" + SETTINGS + NOTIFICATIONS;
    }

    @GetMapping(ACCOUNT)
    public String accountUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, NicknameForm.class));
        return SETTINGS + ACCOUNT;
    }

    @PostMapping(ACCOUNT)
    public String accountUpdate(@CurrentUser Account account, @Valid NicknameForm nicknameForm, Errors errors,
                                Model model, RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS + ACCOUNT;
        }

        accountService.updateNickname(account, nicknameForm.getNickname());
        redirectAttributes.addFlashAttribute("message", "Your nickname updated successfully.");
        return "redirect:/" + SETTINGS + ACCOUNT;
    }

    @PostMapping(TAGS + "/add" )
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

    @GetMapping(TAGS)
    public String updateTags(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute(account);
        Set<Tag> tags = accountService.getTags(account);
        model.addAttribute("tags", tags.stream().map(Tag::getTitle).toList());

        List<String> allTags = tagRepository.findAll().stream().map(Tag::getTitle).toList();
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allTags));

        return SETTINGS + TAGS;
    }

    @PostMapping(TAGS + "/remove")
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

    @GetMapping(ZONES)
    public String updateZones(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute(account);
        Set<Zone> zones = accountService.getZones(account);
        model.addAttribute("zones", zones.stream().map(Zone::toString).toList());

        List<String> allZones = zoneRepository.findAll().stream().map(Zone::toString).toList();
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allZones));

        return SETTINGS + ZONES;
    }

    @PostMapping(ZONES + "/add")
    @ResponseBody
    public ResponseEntity addZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
       Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName());
        if(zone == null){
            return ResponseEntity.badRequest().build();
        }

        accountService.addZone(account, zone);
        return ResponseEntity.ok().build();

    }

    @PostMapping(ZONES + "/remove")
    @ResponseBody
    public ResponseEntity removeZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName());
        if(zone == null){
            return ResponseEntity.badRequest().build();
        }

        accountService.removeZone(account, zone);
        return ResponseEntity.ok().build();
    }






}
