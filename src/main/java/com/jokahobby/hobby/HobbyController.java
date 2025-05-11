package com.jokahobby.hobby;

import com.jokahobby.account.CurrentUser;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Hobby;
import com.jokahobby.hobby.validator.HobbyFormValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.Errors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class HobbyController {

    private final HobbyService hobbyService;
    private final ModelMapper modelMapper;
    private final HobbyFormValidator hobbyFormValidator;

    @InitBinder("hobbyForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(hobbyFormValidator);
    }

    @GetMapping("/new-hobby")
    public String newHobbyForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute("hobbyForm", new HobbyForm());

        return "hobby/form";
    }

    @PostMapping("/new-hobby")
    public String newHobbySubmit(@CurrentUser Account account, @Valid HobbyForm hobbyForm, Errors errors) {
        if (errors.hasErrors()) {
            return "hobby/form";
        }
        Hobby newHobby = hobbyService.createNewHobby(modelMapper.map(hobbyForm, Hobby.class), account);
        return "redirect:/hobby/" + URLEncoder.encode(newHobby.getPath(), StandardCharsets.UTF_8);
    }
}
