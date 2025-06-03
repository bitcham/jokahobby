package com.jokahobby.modules.main;

import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.account.CurrentAccount;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.EnrollmentRepository;
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
    private final EnrollmentRepository enrollmentRepository;
    private final AccountRepository accountRepository;

    @GetMapping("/")
    public String home(@CurrentAccount Account account, Model model) {
        if (account != null) {
            Account accountLoaded = accountRepository.findAccountWithTagsAndZonesById(account.getId());
            model.addAttribute(accountLoaded);
            model.addAttribute("enrollmentList", enrollmentRepository.findByAccountAndAcceptedOrderByEnrolledAtDesc(accountLoaded, true));
            model.addAttribute("hobbyList", hobbyRepository.findByAccount(
                    accountLoaded.getTags(),
                    accountLoaded.getZones()));
            model.addAttribute("hobbyManagerOf",
                    hobbyRepository.findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(account, false));
            model.addAttribute("hobbyMemberOf",
                    hobbyRepository.findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(account, false));
            return "index-after-login";
        }

        model.addAttribute("hobbyList", hobbyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
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
