package com.chutneytesting.design.infra.storage.dataset;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chutneytesting.design.domain.dataset.DataSet;
import com.chutneytesting.design.domain.dataset.DataSetNotFoundException;
import com.chutneytesting.design.infra.storage.scenario.compose.orient.OrientComponentDB;
import com.chutneytesting.tests.OrientDatabaseHelperTest;
import com.orientechnologies.common.log.OLogManager;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.groovy.util.Maps;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OrientDataSetHistoryRepositoryTest {

    private static final String DATABASE_NAME = "orient_dataset_history_test";
    private static final OrientDatabaseHelperTest orientDatabaseHelperTest = new OrientDatabaseHelperTest(DATABASE_NAME);

    private static OrientDataSetHistoryRepository sut;

    private static DataSet originalDataSet;

    @BeforeAll
    public static void setUp() {
        sut = new OrientDataSetHistoryRepository(orientDatabaseHelperTest.orientComponentDB);
        OLogManager.instance().setWarnEnabled(false);

        OrientDataSetRepository orientDataSetRepository = new OrientDataSetRepository(orientDatabaseHelperTest.orientComponentDB);
        originalDataSet = orientDataSetRepository.findById(orientDataSetRepository.save(dataSet()));
    }

    @AfterEach
    public void after() {
        orientDatabaseHelperTest.truncateCollection(OrientComponentDB.DATASET_HISTORY_CLASS);
    }

    @AfterAll
    public static void tearDown() {
        orientDatabaseHelperTest.destroyDB();
    }

    @Test
    public void should_add_versions() {
        addVersionsAndAssert();
    }

    @Test
    public void should_not_add_version_for_identical_datasets() {
        addFirstVersion();
        DataSet originalCopy = DataSet.builder().fromDataSet(originalDataSet).build();
        Optional<Pair<String, Integer>> versionId = sut.addVersion(originalCopy);
        assertThat(versionId).isEmpty();
    }

    @Test
    public void should_not_work() {
        DataSet premier = DataSet.builder()
            .fromDataSet(originalDataSet)
            .withDatatable(Collections.emptyList())
            .withConstants(Collections.emptyMap())
            .build();

        Optional<Pair<String, Integer>> versionId = sut.addVersion(premier);
        versionId = sut.addVersion(premier);
        assertThat(versionId).isEmpty();
    }

    @Test
    public void should_find_dataset_version() {
        List<DataSet> dataSets = addVersionsAndAssert();
        for (int i = 0; i < dataSets.size(); i++) {
            assertThat(sut.version(originalDataSet.id, i + 1)).isEqualTo(dataSets.get(i));
        }
    }

    @Test
    public void should_find_all_version() {
        List<DataSet> dataSets = addVersionsAndAssert().stream()
            .map(ds -> DataSet.builder().fromDataSet(ds).withConstants(null).withDatatable(null).build())
            .collect(toList());

        // Second version - name change
        dataSets.set(1, DataSet.builder().fromDataSet(dataSets.get(1)).withDescription(null).withTags(null).build());
        // Third version - constants change
        dataSets.set(2, DataSet.builder().fromDataSet(dataSets.get(2)).withName(null).withDescription(null).withTags(null).build());
        // Fourth version - description change
        dataSets.set(3, DataSet.builder().fromDataSet(dataSets.get(3)).withName(null).withTags(null).build());
        // Fifth version - datatable change
        dataSets.set(4, DataSet.builder().fromDataSet(dataSets.get(4)).withName(null).withDescription(null).withTags(null).build());
        // Sixth version - tags change
        dataSets.set(5, DataSet.builder().fromDataSet(dataSets.get(5)).withName(null).withDescription(null).build());

        assertThat(sut.allVersions(originalDataSet.id)).containsExactlyEntriesOf(Maps.of(
            1, dataSets.get(0), 2, dataSets.get(1), 3, dataSets.get(2), 4, dataSets.get(3), 5, dataSets.get(4), 6, dataSets.get(5)
        ));
    }

    @Test
    public void should_remove_dataset_history() {
        addVersionsAndAssert();
        assertThat(sut.lastVersion(originalDataSet.id)).isEqualTo(6);

        sut.removeHistory(originalDataSet.id);
        assertThatThrownBy(() -> sut.lastVersion(originalDataSet.id))
            .isInstanceOf(DataSetNotFoundException.class);
    }

    private List<DataSet> addVersionsAndAssert() {
        // First version
        addFirstVersion();

        // Second version - name change
        DataSet datasetWithNewName = DataSet.builder()
            .fromDataSet(originalDataSet)
            .withCreationDate(null)
            .withName("new name")
            .build();
        Optional<Pair<String, Integer>> versionId = sut.addVersion(datasetWithNewName);
        assertThat(versionId).isNotEmpty();
        assertThat(versionId.get().getRight()).isEqualTo(2);
        assertThat(versionId.get().getLeft()).isNotBlank();

        // Third version - constants change
        DataSet datasetWithNewConstants = DataSet.builder()
            .fromDataSet(datasetWithNewName)
            .withCreationDate(null)
            .withConstants(Maps.of("uk1", "uv11", "uk22", "uv2"))
            .build();
        versionId = sut.addVersion(datasetWithNewConstants);
        assertThat(versionId).isNotEmpty();
        assertThat(versionId.get().getRight()).isEqualTo(3);
        assertThat(versionId.get().getLeft()).isNotBlank();

        // Fourth version - description change
        DataSet datasetWithNewDescription = DataSet.builder()
            .fromDataSet(datasetWithNewConstants)
            .withCreationDate(null)
            .withDescription("new Description")
            .build();
        versionId = sut.addVersion(datasetWithNewDescription);
        assertThat(versionId).isNotEmpty();
        assertThat(versionId.get().getRight()).isEqualTo(4);
        assertThat(versionId.get().getLeft()).isNotBlank();

        // Fifth version - datatable change
        DataSet datasetWithNewDatatable = DataSet.builder()
            .fromDataSet(datasetWithNewDescription)
            .withCreationDate(null)
            .withDatatable(Lists.list(
                Maps.of("mk1", "mv11", "mk2", "mv21", "mk3", "mv31"),
                Maps.of("mk1", "new12", "mk2", "mv22", "mk3", "mv32"),
                Maps.of("mk1", "mv13", "mk2", "new23", "mk3", "mv33")
            ))
            .build();
        versionId = sut.addVersion(datasetWithNewDatatable);
        assertThat(versionId).isNotEmpty();
        assertThat(versionId.get().getRight()).isEqualTo(5);
        assertThat(versionId.get().getLeft()).isNotBlank();

        // Sixth version - tags change
        DataSet datasetWithNewTags = DataSet.builder()
            .fromDataSet(datasetWithNewDatatable)
            .withCreationDate(null)
            .withTags(Lists.list("tag1", "tag4"))
            .build();
        versionId = sut.addVersion(datasetWithNewTags);
        assertThat(versionId).isNotEmpty();
        assertThat(versionId.get().getRight()).isEqualTo(6);
        assertThat(versionId.get().getLeft()).isNotBlank();

        return Lists.list(originalDataSet, datasetWithNewName, datasetWithNewConstants, datasetWithNewDescription, datasetWithNewDatatable, datasetWithNewTags);
    }

    private void addFirstVersion() {
        Optional<Pair<String, Integer>> versionId = sut.addVersion(originalDataSet);
        assertThat(versionId).isNotEmpty();
        assertThat(versionId.get().getRight()).isEqualTo(1);
        assertThat(versionId.get().getLeft()).isNotBlank();
    }

    private static DataSet dataSet() {
        return DataSet.builder()
            .withName("name")
            .withDescription("description")
            .withTags(Lists.list("tag1", "tag2"))
            .withConstants(Maps.of("uk1", "uv1", "uk2", "uv2"))
            .withDatatable(Lists.list(
                Maps.of("mk1", "mv11", "mk2", "mv21"),
                Maps.of("mk1", "mv12", "mk2", "mv22"),
                Maps.of("mk1", "mv13", "mk2", "mv23")
            ))
            .build();
    }
}
