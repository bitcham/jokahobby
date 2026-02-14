package com.jokahobby.api.service;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyService;
import com.jokahobby.modules.hobby.event.HobbyCreatedEvent;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import com.jokahobby.modules.tag.TagService;
import com.jokahobby.modules.zone.ZoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
}
