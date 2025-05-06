package com.jokahobby.settings;

import com.jokahobby.account.AccountService;
import com.jokahobby.account.CurrentUser;
import com.jokahobby.domain.Account;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final AccountService accountService;

    private final String SETTINGS_PROFILE_URL = "/settings/profile";
    private final String SETTINGS_PROFILE_VIEW = "settings/profile";

    @GetMapping(SETTINGS_PROFILE_URL)
    public String profileUpdateForm(@CurrentUser Account account, Model model){
        model.addAttribute(account);
        model.addAttribute(new Profile(account));
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

}
