package com.jokahobby.modules.hobby;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

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
;
        long total = query.fetchCount();
        JPQLQuery<Hobby> pageableQuery = getQuerydsl().applyPagination(pageable, query);
        List<Hobby> content = pageableQuery.fetch();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<Hobby> findPublished(String country, String city, HobbySortType sort, Pageable pageable) {
        QHobby hobby = QHobby.hobby;
        QHobbyZone hobbyZone = QHobbyZone.hobbyZone;

        BooleanExpression predicate = hobby.published.isTrue().and(hobby.closed.isFalse());

        if (country != null && !country.isBlank()) {
            predicate = predicate.and(hobby.id.in(
                    JPAExpressions.select(hobbyZone.hobby.id)
                            .from(hobbyZone)
                            .where(hobbyZone.zone.country.eq(country))));
        }

        if (city != null && !city.isBlank()) {
            predicate = predicate.and(hobby.id.in(
                    JPAExpressions.select(hobbyZone.hobby.id)
                            .from(hobbyZone)
                            .where(hobbyZone.zone.city.eq(city))));
        }

        OrderSpecifier<?> orderSpecifier = switch (sort != null ? sort : HobbySortType.LATEST) {
            case POPULAR -> hobby.memberCount.desc();
            case OLDEST -> hobby.publishedDateTime.asc();
            default -> hobby.publishedDateTime.desc();
        };

        JPQLQuery<Hobby> query = from(hobby).where(predicate).orderBy(orderSpecifier);
        long total = query.fetchCount();
        JPQLQuery<Hobby> pageableQuery = getQuerydsl().applyPagination(pageable, query);
        List<Hobby> content = pageableQuery.fetch();
        return new PageImpl<>(content, pageable, total);
    }
}
