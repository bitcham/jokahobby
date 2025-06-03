package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Transactional(readOnly = true)
public interface HobbyRepository extends JpaRepository<Hobby, Long>, HobbyRepositoryExtension {

    boolean existsByPath(String path);

    boolean existsByTitle(String title);

    @EntityGraph(value = "Hobby.withAll", type = EntityGraph.EntityGraphType.LOAD)
    Hobby findByPath(String path);

    @EntityGraph(value = "Hobby.withTagsAndManagers", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findAccountWithTagsByPath(String path);

    @EntityGraph(value = "Hobby.withZonesAndManagers", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findAccountWithZonesByPath(String path);

    @EntityGraph(value = "Hobby.withManagers", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findHobbyWithManagersByPath(String path);

    @EntityGraph(value = "Hobby.withMembers", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findHobbyWithMembersByPath(String path);

    Hobby findHobbyOnlyByPath(String path);

    @EntityGraph(value = "Hobby.withTagsAndZones", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findHobbyWithTagsAndZonesById(Long id);

    @EntityGraph(value = "Hobby.withManagersAndMembers", type = EntityGraph.EntityGraphType.FETCH)
    Hobby findHobbyWithManagersAndMembersById(Long id);

    @EntityGraph(attributePaths = {"zones", "tags"})
    List<Hobby> findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(boolean published, boolean closed);

    List<Hobby> findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(Account account, boolean closed);

    List<Hobby> findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(Account account, boolean closed);
}
