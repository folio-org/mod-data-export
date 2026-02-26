package org.folio.dataexp.controllers;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.folio.dataexp.controllers.RelatedUsersController;
import java.util.UUID;
import org.folio.dataexp.domain.dto.JobExecution;
import org.folio.dataexp.domain.dto.JobExecutionRunBy;
import org.folio.dataexp.domain.dto.RelatedUser;
import org.folio.dataexp.domain.dto.RelatedUserCollection;
import org.folio.dataexp.domain.entity.JobExecutionEntity;
import org.folio.dataexp.repository.JobExecutionEntityCqlRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class RelatedUsersControllerTest {

    @InjectMocks
private RelatedUsersController relatedUsersController;

    @Mock
private JobExecutionEntityCqlRepository jobExecutionEntityCqlRepository;

    @Test
void getRelatedUsersShouldReturnUniqueUsersWhenMultipleJobsRunBySameUser() {
    // TestMate-268e302a3b7b338bca67c2d79808e581
    // Given
    var userA = new JobExecutionRunBy()
        .userId("user-a-id")
        .firstName("User")
        .lastName("A");
    var userB = new JobExecutionRunBy()
        .userId("user-b-id")
        .firstName("User")
        .lastName("B");
    var jobExecution1 = new JobExecution().runBy(userA);
    var jobExecution2 = new JobExecution().runBy(userB);
    var jobExecution3 = new JobExecution().runBy(userA);
    var jobExecutionEntity1 = JobExecutionEntity.builder().jobExecution(jobExecution1).build();
    var jobExecutionEntity2 = JobExecutionEntity.builder().jobExecution(jobExecution2).build();
    var jobExecutionEntity3 = JobExecutionEntity.builder().jobExecution(jobExecution3).build();
    var jobExecutionEntities = List.of(jobExecutionEntity1, jobExecutionEntity2, jobExecutionEntity3);
    when(jobExecutionEntityCqlRepository.findAll()).thenReturn(jobExecutionEntities);
    // When
    var responseEntity = relatedUsersController.getRelatedUsers();
    // Then
    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    RelatedUserCollection relatedUserCollection = responseEntity.getBody();
    assertThat(relatedUserCollection.getTotalRecords()).isEqualTo(2);
    assertThat(relatedUserCollection.getRelatedUsers()).hasSize(2)
        .containsExactlyInAnyOrder(
            new RelatedUser().userId("user-a-id").firstName("User").lastName("A"),
            new RelatedUser().userId("user-b-id").firstName("User").lastName("B")
        );
    verify(jobExecutionEntityCqlRepository).findAll();
}

    @Test
    void getRelatedUsersShouldReturnEmptyListWhenNoJobExecutionsFound() {
        // TestMate-bb37d451fdfa8ad27832a6df3f669363
        // Given
        when(jobExecutionEntityCqlRepository.findAll()).thenReturn(Collections.emptyList());
        // When
        var responseEntity = relatedUsersController.getRelatedUsers();
        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        RelatedUserCollection relatedUserCollection = responseEntity.getBody();
        assertThat(relatedUserCollection.getTotalRecords()).isZero();
        assertThat(relatedUserCollection.getRelatedUsers()).isEmpty();
        verify(jobExecutionEntityCqlRepository).findAll();
    }

    @Test
    void getRelatedUsersShouldFilterOutJobsWithNullRunBy() {
        // TestMate-d613f91b15dac21be007b0a6671f4c46
        // Given
        var userA = new JobExecutionRunBy()
            .userId("user-a-id")
            .firstName("User")
            .lastName("A");
        var jobExecutionWithUser = new JobExecution().runBy(userA);
        var jobExecutionWithoutUser = new JobExecution().runBy(null);
        var jobExecutionEntityWithUser = JobExecutionEntity.builder().jobExecution(jobExecutionWithUser).build();
        var jobExecutionEntityWithoutUser = JobExecutionEntity.builder().jobExecution(jobExecutionWithoutUser).build();
        var jobExecutionEntities = List.of(jobExecutionEntityWithUser, jobExecutionEntityWithoutUser);
        when(jobExecutionEntityCqlRepository.findAll()).thenReturn(jobExecutionEntities);
        // When
        var responseEntity = relatedUsersController.getRelatedUsers();
        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        RelatedUserCollection relatedUserCollection = responseEntity.getBody();
        assertThat(relatedUserCollection.getTotalRecords()).isEqualTo(1);
        assertThat(relatedUserCollection.getRelatedUsers()).hasSize(1)
            .containsExactly(new RelatedUser().userId("user-a-id").firstName("User").lastName("A"));
        verify(jobExecutionEntityCqlRepository).findAll();
    }

    @Test
    void getRelatedUsersShouldCorrectlyMapAllUserFields() {
        // TestMate-476c95d1b98f1886a713ffd5342caad0
        // Given
        var runBy = new JobExecutionRunBy()
            .userId("test-user-id-123")
            .firstName("John")
            .lastName("Doe");
        var jobExecution = new JobExecution().runBy(runBy);
        var jobExecutionEntity = JobExecutionEntity.builder().jobExecution(jobExecution).build();
        when(jobExecutionEntityCqlRepository.findAll()).thenReturn(List.of(jobExecutionEntity));
        // When
        var responseEntity = relatedUsersController.getRelatedUsers();
        // Then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        RelatedUserCollection relatedUserCollection = responseEntity.getBody();
        assertThat(relatedUserCollection.getTotalRecords()).isEqualTo(1);
        assertThat(relatedUserCollection.getRelatedUsers()).hasSize(1);
        var expectedRelatedUser = new RelatedUser()
            .userId("test-user-id-123")
            .firstName("John")
            .lastName("Doe");
        assertThat(relatedUserCollection.getRelatedUsers()).containsExactly(expectedRelatedUser);
        verify(jobExecutionEntityCqlRepository).findAll();
    }

}
