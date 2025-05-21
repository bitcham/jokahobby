package com.jokahobby.modules.hobby;

import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class HobbyRepositoryExtensionImpl extends QuerydslRepositorySupport implements HobbyRepositoryExtension {

    public HobbyRepositoryExtensionImpl() {
        super(Hobby.class);
    }

    @Override
    public List<Hobby> findByKeyword(String keyword) {
        QHobby hobby = QHobby.hobby;
        JPQLQuery<Hobby> query = from(hobby)
                .where(hobby.published.isTrue()
                        .and(hobby.title.containsIgnoreCase(keyword))
                        .or(hobby.tags.any().title.containsIgnoreCase(keyword))
                        .or(hobby.zones.any().city.containsIgnoreCase(keyword))
                        .or(hobby.zones.any().localNameOfCity.containsIgnoreCase(keyword)));
        return query.fetch();
    }
}
