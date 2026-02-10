package com.jokahobby.modules.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoftDeletableEntityTest {

    private static class TestEntity extends SoftDeletableEntity {}

    @Test
    @DisplayName("new entity is not deleted by default")
    void newEntityIsNotDeleted() {
        TestEntity entity = new TestEntity();
        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("softDelete sets deletedAt and isDeleted returns true")
    void softDeleteSetsDeletedAt() {
        TestEntity entity = new TestEntity();
        entity.softDelete();

        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("restore clears deletedAt and isDeleted returns false")
    void restoreClearsDeletedAt() {
        TestEntity entity = new TestEntity();
        entity.softDelete();
        entity.restore();

        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
    }
}
