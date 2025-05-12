package com.jokahobby.hobby;

import com.jokahobby.account.CurrentUser;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Hobby;
import com.jokahobby.hobby.validator.HobbyFormValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class HobbyController {

    private final HobbyRepository hobbyRepository;
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
    public String newHobbySubmit(@CurrentUser Account account, @Valid HobbyForm hobbyForm, Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return "hobby/form";
        }
        Hobby newHobby = hobbyService.createNewHobby(modelMapper.map(hobbyForm, Hobby.class), account);
        return "redirect:/hobby/" + URLEncoder.encode(newHobby.getPath(), StandardCharsets.UTF_8);
    }

    @GetMapping("/hobby/{path}")
    public String viewHobby(@CurrentUser Account account, @PathVariable String path, Model model) {
        Hobby hobby = hobbyService.getHobby(path);
        model.addAttribute(account);
        model.addAttribute(hobbyRepository.findByPath(path));
        return "hobby/view";
    }

    @GetMapping("/hobby/{path}/members")
    public String viewHobbyMembers(@CurrentUser Account account, @PathVariable String path, Model model){
        model.addAttribute(account);
        model.addAttribute(hobbyRepository.findByPath(path));
        return "hobby/members";
    }
}
