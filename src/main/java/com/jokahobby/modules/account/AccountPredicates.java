package com.jokahobby.modules.account;

import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import com.querydsl.core.types.Predicate;

import java.util.Set;

import static com.querydsl.jpa.JPAExpressions.select;

public class AccountPredicates {

    public static Predicate findByTagsAndZones(Set<Tag> tags, Set<Zone> zones) {
        QAccountTag accountTag = QAccountTag.accountTag;
        QAccountZone accountZone = QAccountZone.accountZone;
        QAccount account = QAccount.account;

        return account.id.in(
                select(accountZone.account.id)
                        .from(accountZone)
                        .where(accountZone.zone.in(zones))
        ).and(account.id.in(
                select(accountTag.account.id)
                        .from(accountTag)
                        .where(accountTag.tag.in(tags))
        ));
    }

}
