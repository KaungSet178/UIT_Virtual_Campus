package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.repository.AcademicGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicGroupServiceTest {

    @Mock
    private AcademicGroupRepository academicGroupRepository;

    @Test
    void yearOneDefaultsToSectionGroups() {
        AcademicGroupService service = new AcademicGroupService(academicGroupRepository);
        when(academicGroupRepository.findAllWithFilters((short) 1, AcademicGroupType.SECTION, null))
                .thenReturn(List.of(new AcademicGroup(1L, (short) 1, AcademicGroupType.SECTION, "A", null, null)));

        List<AcademicGroupResponse> response = service.getAcademicGroups((short) 1, null, null);

        assertThat(response).extracting(AcademicGroupResponse::groupType).containsExactly(AcademicGroupType.SECTION);
        verify(academicGroupRepository).findAllWithFilters((short) 1, AcademicGroupType.SECTION, null);
    }

    @Test
    void yearThreeDefaultsToDepartmentGroups() {
        AcademicGroupService service = new AcademicGroupService(academicGroupRepository);
        when(academicGroupRepository.findAllWithFilters((short) 3, AcademicGroupType.DEPARTMENT, null))
                .thenReturn(List.of(
                        new AcademicGroup(31L, (short) 3, AcademicGroupType.DEPARTMENT, null, "CS", null),
                        new AcademicGroup(32L, (short) 3, AcademicGroupType.DEPARTMENT, null, "CT", null)
                ));

        List<AcademicGroupResponse> response = service.getAcademicGroups((short) 3, null, null);

        assertThat(response).extracting(AcademicGroupResponse::groupType)
                .containsExactly(AcademicGroupType.DEPARTMENT, AcademicGroupType.DEPARTMENT);
        assertThat(response).extracting(AcademicGroupResponse::department).containsExactly("CS", "CT");
        assertThat(response).extracting(AcademicGroupResponse::major).containsOnlyNulls();
        verify(academicGroupRepository).findAllWithFilters((short) 3, AcademicGroupType.DEPARTMENT, null);
    }

    @Test
    void yearFourDefaultsToMajorGroups() {
        AcademicGroupService service = new AcademicGroupService(academicGroupRepository);
        when(academicGroupRepository.findAllWithFilters((short) 4, AcademicGroupType.MAJOR, "CS"))
                .thenReturn(List.of(new AcademicGroup(41L, (short) 4, AcademicGroupType.MAJOR, null, "CS", "SE")));

        List<AcademicGroupResponse> response = service.getAcademicGroups((short) 4, null, "CS");

        assertThat(response).extracting(AcademicGroupResponse::groupType).containsExactly(AcademicGroupType.MAJOR);
        assertThat(response).extracting(AcademicGroupResponse::department).containsExactly("CS");
        assertThat(response).extracting(AcademicGroupResponse::major).containsExactly("SE");
        verify(academicGroupRepository).findAllWithFilters((short) 4, AcademicGroupType.MAJOR, "CS");
    }
}
