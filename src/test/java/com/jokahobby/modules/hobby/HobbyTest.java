package com.jokahobby.modules.hobby;


import com.jokahobby.modules.account.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HobbyTest {

    Hobby hobby;
    Account account;

    @BeforeEach
    void beforeEach() {
        hobby = new Hobby();
        account = new Account();
        account.setNickname("cutedog");
    }

    @DisplayName("Can join the hobby if it's public, recruiting members, and the user is neither a member nor a manager")
    @Test
    void isJoinable() {
        hobby.setPublished(true);
        hobby.setRecruiting(true);

        assertTrue(hobby.isJoinable(account));
    }

    @DisplayName("Hobby managers don't need to join the hobby even if it's public and recruiting")
    @Test
    void isJoinable_false_for_manager() {
        hobby.setPublished(true);
        hobby.setRecruiting(true);
        hobby.addManager(account);

        assertFalse(hobby.isJoinable(account));
    }

    @DisplayName("Hobby members don't need to rejoin the hobby even if it's public and recruiting")
    @Test
    void isJoinable_false_for_member() {
        hobby.setPublished(true);
        hobby.setRecruiting(true);
        hobby.addMember(account);

        assertFalse(hobby.isJoinable(account));
    }

    @DisplayName("Cannot join the hobby if it's not public or not recruiting")
    @Test
    void isJoinable_false_for_non_recruiting_hobby() {
        hobby.setPublished(true);
        hobby.setRecruiting(false);

        assertFalse(hobby.isJoinable(account));

        hobby.setPublished(false);
        hobby.setRecruiting(true);

        assertFalse(hobby.isJoinable(account));
    }

    @DisplayName("Check if user is a hobby manager")
    @Test
    void isManager() {
        hobby.addManager(account);
        assertTrue(hobby.isManager(account));
    }

    @DisplayName("Check if user is a hobby member")
    @Test
    void isMember() {
        hobby.addMember(account);
        assertTrue(hobby.isMember(account));
    }

}
