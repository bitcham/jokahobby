package com.jokahobby.hobby;

import com.jokahobby.account.CurrentAccount;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Hobby;
import com.jokahobby.hobby.form.HobbyDescriptionForm;
import com.jokahobby.hobby.form.HobbyForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import static java.nio.charset.StandardCharsets.*;

@Controller
@RequestMapping("/hobby/{path}/settings/")
@RequiredArgsConstructor
public class HobbySettingsController {

    private final HobbyService hobbyService;
    private final HobbyRepository hobbyRepository;
    private final ModelMapper modelMapper;

    static final String DESCRIPTION = "description";
    static final String ROOT = "/";
    static final String SETTINGS = "settings";
    static final String HOBBY = "hobby";

    @GetMapping(DESCRIPTION)
    public String viewHobbySetting(@CurrentAccount Account account, @PathVariable String path, Model model) {
        Hobby hobby = hobbyService.getHobbyToUpdate(account,path);
        model.addAttribute(account);
        model.addAttribute(hobby);
        model.addAttribute(modelMapper.map(hobby, HobbyDescriptionForm.class));
        return HOBBY + ROOT + SETTINGS  + ROOT + DESCRIPTION;
    }

    @PostMapping(DESCRIPTION)
    public String updateHobbyInfo(
            @CurrentAccount Account account, @PathVariable String path,
            @Valid HobbyDescriptionForm hobbyDescriptionForm, Errors errors,
            Model model, RedirectAttributes redirectAttributes) {

        Hobby hobby = hobbyService.getHobbyToUpdate(account, path);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(hobby);
            return HOBBY + ROOT + SETTINGS + ROOT + DESCRIPTION;
        }
        hobbyService.updateHobbyDescription(hobby, hobbyDescriptionForm);
        redirectAttributes.addFlashAttribute("message", "Hobby information updated successfully.");
        return "redirect:/" + HOBBY + "/" + getEncodedPath(path) + ROOT + SETTINGS + ROOT + DESCRIPTION;
    }

    private String getEncodedPath(String path) {
        return URLEncoder.encode(path, UTF_8);
    }


}
