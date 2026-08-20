package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.entity.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampusValidationServiceTest {

    private CampusValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new CampusValidationService();
    }

    @ParameterizedTest
    @ValueSource(strings = {"111", "231", "421", "466", "166", "312", "345", "411", "126"})
    @DisplayName("Should accept valid numbered room format")
    void validNumberedRooms(String roomNumber) {
        assertThat(validationService.isValidNumberedRoom(roomNumber)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "011", // Invalid building 0
            "501", // Invalid building 5
            "721", // Invalid building 7
            "101", // Invalid floor 0
            "171", // Invalid floor 7
            "179", // Invalid floor 7 & room 9
            "237", // Invalid room 7
            "460", // Invalid room 0
            "467", // Invalid room 7
            "11",  // Too short
            "1111",// Too long
            "ABC", // Non-digits
            "12a", // Non-digits
            "",    // Empty
    })
    @DisplayName("Should reject invalid numbered rooms")
    void invalidNumberedRooms(String roomNumber) {
        assertThat(validationService.isValidNumberedRoom(roomNumber)).isFalse();
    }

    @Test
    @DisplayName("Multipurpose Hall is exempt from ABC numbering")
    void multipurposeHallIsExempt() {
        assertThat(validationService.isExemptFromNumbering("MPH", RoomType.MULTIPURPOSE_HALL)).isTrue();
        assertThat(validationService.isExemptFromNumbering("Multipurpose Hall", RoomType.MULTIPURPOSE_HALL)).isTrue();
        assertThat(validationService.isExemptFromNumbering("MPH", "CLASSROOM")).isTrue();

        assertThatCode(() -> validationService.validateRoom("MPH", RoomType.MULTIPURPOSE_HALL, 1L, 1))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Validates hierarchy consistency correctly")
    void hierarchyConsistency() {
        assertThat(validationService.isHierarchyConsistent("231", 2L, 3)).isTrue();
        assertThat(validationService.isHierarchyConsistent("466", 4L, 6)).isTrue();
        assertThat(validationService.isHierarchyConsistent("111", 1L, 1)).isTrue();

        // Mismatched building
        assertThat(validationService.isHierarchyConsistent("231", 1L, 3)).isFalse();
        // Mismatched floor
        assertThat(validationService.isHierarchyConsistent("231", 2L, 4)).isFalse();
    }

    @Test
    @DisplayName("validateRoom throws exception on invalid room format or hierarchy mismatch")
    void validateRoomExceptions() {
        assertThatThrownBy(() -> validationService.validateRoom("501", "CLASSROOM", 5L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid numbered room");

        assertThatThrownBy(() -> validationService.validateRoom("231", "CLASSROOM", 1L, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match hierarchy");
    }
}
