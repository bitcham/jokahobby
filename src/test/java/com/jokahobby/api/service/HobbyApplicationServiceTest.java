package com.jokahobby.api.service;

import com.jokahobby.api.dto.response.HobbyListResponse;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyService;
import com.jokahobby.modules.hobby.HobbySortType;
import com.jokahobby.modules.hobby.event.HobbyCreatedEvent;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.tag.TagService;
import com.jokahobby.modules.zone.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HobbyApplicationServiceTest {

    @InjectMocks
    private HobbyApplicationService hobbyApplicationService;

    @Mock
    private HobbyService hobbyService;

    @Mock
    private TagService tagService;

    @Mock
    private ZoneService zoneService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Hobby hobby;
    private Account manager;

    @BeforeEach
    void setUp() {
        manager = Account.builder()
                .id(UUID.randomUUID())
                .email("manager@test.com")
                .nickname("manager")
                .build();

        hobby = Hobby.builder()
                .id(1L)
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("desc")
                .build();
    }

    @Test
    @DisplayName("publish publishes HobbyCreatedEvent")
    void publish_publishesHobbyCreatedEvent() {
        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);

        hobbyApplicationService.publish("test-hobby", manager);

        verify(eventPublisher).publishEvent(any(HobbyCreatedEvent.class));
    }

    @Test
    @DisplayName("close publishes HobbyUpdateEvent")
    void close_publishesHobbyUpdateEvent() {
        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);

        hobbyApplicationService.close("test-hobby", manager);

        verify(eventPublisher).publishEvent(any(HobbyUpdateEvent.class));
    }

    @Test
    @DisplayName("updateDescription publishes HobbyUpdateEvent")
    void updateDescription_publishesHobbyUpdateEvent() {
        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);

        hobbyApplicationService.updateDescription("test-hobby", manager,
                new com.jokahobby.api.dto.request.HobbyDescriptionUpdateRequest("short", "full"));

        verify(eventPublisher).publishEvent(any(HobbyUpdateEvent.class));
    }

    @Test
    @DisplayName("startRecruit publishes HobbyUpdateEvent")
    void startRecruit_publishesHobbyUpdateEvent() {
        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);

        hobbyApplicationService.startRecruit("test-hobby", manager);

        verify(eventPublisher).publishEvent(any(HobbyUpdateEvent.class));
    }

    @Test
    @DisplayName("stopRecruit publishes HobbyUpdateEvent")
    void stopRecruit_publishesHobbyUpdateEvent() {
        given(hobbyService.getHobbyWithManagerCheck(manager, "test-hobby")).willReturn(hobby);

        hobbyApplicationService.stopRecruit("test-hobby", manager);

        verify(eventPublisher).publishEvent(any(HobbyUpdateEvent.class));
    }

    @Nested
    @DisplayName("Batch tag loading (N+1 fix)")
    class BatchTagLoading {

        private Hobby hobby2;
        private Pageable pageable;

        @BeforeEach
        void setUp() {
            hobby2 = Hobby.builder()
                    .id(2L)
                    .path("hobby-2")
                    .title("Hobby 2")
                    .shortDescription("desc2")
                    .build();
            pageable = PageRequest.of(0, 16);
        }

        @Test
        @DisplayName("getPublishedHobbies uses batch tag loading instead of per-hobby getTags")
        void getPublishedHobbies_usesBatchTagLoading() {
            Page<Hobby> hobbyPage = new PageImpl<>(List.of(hobby, hobby2), pageable, 2);
            Tag tag1 = Tag.builder().id(1L).title("spring").build();
            Tag tag2 = Tag.builder().id(2L).title("java").build();
            Map<Long, List<Tag>> tagMap = Map.of(
                    1L, List.of(tag1),
                    2L, List.of(tag2)
            );

            given(hobbyService.findPublished(null, null, null, pageable)).willReturn(hobbyPage);
            given(hobbyService.getTagsByHobbyIds(List.of(1L, 2L))).willReturn(tagMap);

            Page<HobbyListResponse> result = hobbyApplicationService.getPublishedHobbies(null, null, null, pageable);

            assertThat(result.getContent()).hasSize(2);
            verify(hobbyService).getTagsByHobbyIds(List.of(1L, 2L));
            verify(hobbyService, never()).getTags(any(Hobby.class));
        }

        @Test
        @DisplayName("searchHobbies uses batch tag loading instead of per-hobby getTags")
        void searchHobbies_usesBatchTagLoading() {
            Page<Hobby> hobbyPage = new PageImpl<>(List.of(hobby, hobby2), pageable, 2);
            Tag tag1 = Tag.builder().id(1L).title("spring").build();
            Map<Long, List<Tag>> tagMap = Map.of(1L, List.of(tag1));

            given(hobbyService.findByKeyword("spring", pageable)).willReturn(hobbyPage);
            given(hobbyService.getTagsByHobbyIds(List.of(1L, 2L))).willReturn(tagMap);

            Page<HobbyListResponse> result = hobbyApplicationService.searchHobbies("spring", pageable);

            assertThat(result.getContent()).hasSize(2);
            verify(hobbyService).getTagsByHobbyIds(List.of(1L, 2L));
            verify(hobbyService, never()).getTags(any(Hobby.class));
        }

        @Test
        @DisplayName("getPublishedHobbies handles empty page without unnecessary queries")
        void getPublishedHobbies_handlesEmptyPage() {
            Page<Hobby> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(hobbyService.findPublished(null, null, null, pageable)).willReturn(emptyPage);
            given(hobbyService.getTagsByHobbyIds(List.of())).willReturn(Map.of());

            Page<HobbyListResponse> result = hobbyApplicationService.getPublishedHobbies(null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(hobbyService).getTagsByHobbyIds(List.of());
            verify(hobbyService, never()).getTags(any(Hobby.class));
        }
    }
}
