package com.jokahobby.main;

import com.jokahobby.account.CurrentAccount;
import com.jokahobby.domain.Account;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

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
}
