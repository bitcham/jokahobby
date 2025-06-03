package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.QAccount;
import com.jokahobby.modules.tag.QTag;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.QZone;
import com.jokahobby.modules.zone.Zone;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
import java.util.Set;

public class HobbyRepositoryExtensionImpl extends QuerydslRepositorySupport implements HobbyRepositoryExtension {

    public HobbyRepositoryExtensionImpl() {
        super(Hobby.class);
    }

    @Override
    public Page<Hobby> findByKeyword(String keyword, Pageable pageable) {
        QHobby hobby = QHobby.hobby;
        JPQLQuery<Hobby> query = from(hobby).where(hobby.published.isTrue()
                        .and(hobby.title.containsIgnoreCase(keyword))
                        .or(hobby.tags.any().title.containsIgnoreCase(keyword))
                        .or(hobby.zones.any().city.containsIgnoreCase(keyword))
                        .or(hobby.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
                        .leftJoin(hobby.tags, QTag.tag).fetchJoin()
                        .leftJoin(hobby.zones, QZone.zone).fetchJoin()
                        .distinct();
        JPQLQuery<Hobby> pageableQuery = getQuerydsl().applyPagination(pageable, query);
        QueryResults<Hobby> fetchResults = pageableQuery.fetchResults();
        return new PageImpl<>(fetchResults.getResults(), pageable, fetchResults.getTotal());
    }

    @Override
    public List<Hobby> findByAccount(Set<Tag> tags, Set<Zone> zones) {
        QHobby hobby = QHobby.hobby;
        JPQLQuery<Hobby> query = from(hobby).where(hobby.published.isTrue()
                        .and(hobby.closed.isFalse())
                        .and(hobby.tags.any().in(tags))
                        .and(hobby.zones.any().in(zones)))
                .leftJoin(hobby.tags, QTag.tag).fetchJoin()
                .leftJoin(hobby.zones, QZone.zone).fetchJoin()
                .orderBy(hobby.publishedDateTime.desc())
                .distinct()
                .limit(9);
        return query.fetch();
    }
}
