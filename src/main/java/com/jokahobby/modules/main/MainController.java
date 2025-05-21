package com.jokahobby.modules.main;

import com.jokahobby.modules.account.CurrentAccount;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final HobbyRepository hobbyRepository;

    @GetMapping("/")
    public String home(@CurrentAccount Account account, Model model) {
        if(account != null) {
            model.addAttribute("account", account);
        }

        return "index";
    }

   @GetMapping("/login")
    public String login(){
        return "login";
   }

   @GetMapping("/search/hobby")
    public String searchHobby(String keyword, Model model){
       List<Hobby> hobbyList = hobbyRepository.findByKeyword(keyword);
       model.addAttribute("hobbyList", hobbyList);
       model.addAttribute("keyword", keyword);
       return "search";
   }
}
