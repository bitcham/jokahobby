package com.jokahobby.modules.hobby;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HobbyServiceEventPublishTest {

    @Test
    @DisplayName("HobbyService has no ApplicationEventPublisher dependency")
    void noPublisherDependency() {
        var fields = HobbyService.class.getDeclaredFields();
        for (var field : fields) {
            assertThat(field.getType().getSimpleName())
                    .as("HobbyService should not have ApplicationEventPublisher field")
                    .isNotEqualTo("ApplicationEventPublisher");
        }
    }
}
