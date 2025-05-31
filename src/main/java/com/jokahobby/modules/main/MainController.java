package com.jokahobby.modules.main;

import com.jokahobby.modules.account.CurrentAccount;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public String searchHobby(@PageableDefault(size = 9, sort = "publishedDateTime",
           direction = Sort.Direction.DESC) Pageable pageable, String keyword, Model model){
       Page<Hobby> hobbyPage = hobbyRepository.findByKeyword(keyword, pageable);
       model.addAttribute("hobbyPage", hobbyPage);
       model.addAttribute("keyword", keyword);
       model.addAttribute("sortProperty", pageable.getSort().toString().contains("publishedDateTime") ? "publishedDateTime" : "memberCount");
       return "search";
   }
}
