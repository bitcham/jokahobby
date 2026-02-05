package com.jokahobby.modules.account;

import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import com.querydsl.core.types.Predicate;

import java.util.Set;

public class AccountPredicates {

    public static Predicate findByTagsAndZones(Set<Tag> tags, Set<Zone> zones) {
        QAccountTag accountTag = QAccountTag.accountTag;
        QAccountZone accountZone = QAccountZone.accountZone;
        QAccount account = QAccount.account;

        return account.id.in(
                com.querydsl.jpa.JPAExpressions.select(accountZone.account.id)
                        .from(accountZone)
                        .where(accountZone.zone.in(zones))
        ).and(account.id.in(
                com.querydsl.jpa.JPAExpressions.select(accountTag.account.id)
                        .from(accountTag)
                        .where(accountTag.tag.in(tags))
        ));
    }

}
