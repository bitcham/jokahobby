package com.jokahobby.modules.account;

import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import com.querydsl.core.types.Predicate;

import java.util.Set;

public class AccountPredicates {

    public static Predicate findByTagsAndZones(Set<Tag> tags, Set<Zone> zones) {
        QAccount account = QAccount.account;
        return QAccount.account.zones.any().in(zones).and(account.tags.any().in(tags));
    }

}
