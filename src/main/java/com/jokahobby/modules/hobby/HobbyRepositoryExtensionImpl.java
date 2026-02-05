package com.jokahobby.modules.hobby;

import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import com.querydsl.jpa.JPAExpressions;
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
        QHobbyTag hobbyTag = QHobbyTag.hobbyTag;
        QHobbyZone hobbyZone = QHobbyZone.hobbyZone;

        JPQLQuery<Hobby> query = from(hobby).where(hobby.published.isTrue()
                        .and(hobby.title.containsIgnoreCase(keyword))
                        .or(hobby.id.in(
                                JPAExpressions.select(hobbyTag.hobby.id)
                                        .from(hobbyTag)
                                        .where(hobbyTag.tag.title.containsIgnoreCase(keyword))))
                        .or(hobby.id.in(
                                JPAExpressions.select(hobbyZone.hobby.id)
                                        .from(hobbyZone)
                                        .where(hobbyZone.zone.city.containsIgnoreCase(keyword))))
                        .or(hobby.id.in(
                                JPAExpressions.select(hobbyZone.hobby.id)
                                        .from(hobbyZone)
                                        .where(hobbyZone.zone.localNameOfCity.containsIgnoreCase(keyword)))))
                .distinct();
        long total = query.fetchCount();
        JPQLQuery<Hobby> pageableQuery = getQuerydsl().applyPagination(pageable, query);
        List<Hobby> content = pageableQuery.fetch();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Hobby> findByAccount(Set<Tag> tags, Set<Zone> zones) {
        QHobby hobby = QHobby.hobby;
        QHobbyTag hobbyTag = QHobbyTag.hobbyTag;
        QHobbyZone hobbyZone = QHobbyZone.hobbyZone;

        JPQLQuery<Hobby> query = from(hobby).where(hobby.published.isTrue()
                        .and(hobby.closed.isFalse())
                        .and(hobby.id.in(
                                JPAExpressions.select(hobbyTag.hobby.id)
                                        .from(hobbyTag)
                                        .where(hobbyTag.tag.in(tags))))
                        .and(hobby.id.in(
                                JPAExpressions.select(hobbyZone.hobby.id)
                                        .from(hobbyZone)
                                        .where(hobbyZone.zone.in(zones)))))
                .orderBy(hobby.publishedDateTime.desc())
                .distinct()
                .limit(9);
        return query.fetch();
    }
}
