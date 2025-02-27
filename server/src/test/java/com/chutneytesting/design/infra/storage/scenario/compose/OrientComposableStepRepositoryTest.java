package com.chutneytesting.design.infra.storage.scenario.compose;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chutneytesting.design.domain.scenario.compose.AlreadyExistingComposableStepException;
import com.chutneytesting.design.domain.scenario.compose.ComposableStep;
import com.chutneytesting.design.domain.scenario.compose.ComposableStepRepository;
import com.chutneytesting.design.domain.scenario.compose.ParentStepId;
import com.chutneytesting.design.domain.scenario.compose.Strategy;
import com.chutneytesting.design.infra.storage.scenario.compose.orient.OrientComponentDB;
import com.chutneytesting.design.infra.storage.scenario.compose.orient.changelog.OrientChangelog;
import com.chutneytesting.tests.OrientDatabaseHelperTest;
import com.chutneytesting.tools.ImmutablePaginationRequestParametersDto;
import com.chutneytesting.tools.ImmutableSortRequestParametersDto;
import com.chutneytesting.tools.PaginatedDto;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OrientComposableStepRepositoryTest {

    private static final String DATABASE_NAME = "orient_composable_step_test";
    private static final OrientDatabaseHelperTest orientDatabaseHelperTest = new OrientDatabaseHelperTest(DATABASE_NAME);

    private static ComposableStepRepository sut;

    @BeforeAll
    public static void setUp() {
        sut = new OrientComposableStepRepository(orientDatabaseHelperTest.orientComponentDB, orientDatabaseHelperTest.stepMapper);
        OLogManager.instance().setWarnEnabled(false);
    }

    @AfterEach
    public void after() {
        orientDatabaseHelperTest.truncateCollection(OrientComponentDB.STEP_CLASS);
        orientDatabaseHelperTest.truncateCollection(OrientComponentDB.GE_STEP_CLASS);
    }

    @AfterAll
    public static void tearDown() {
        orientDatabaseHelperTest.destroyDB();
    }

    @Test
    public void should_find_saved_step_when_search_by_id() {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("timeOut", "10 s");
        parameters.put("retryDelay", "10 s");

        final ComposableStep fStep = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep(
                "a thing is connected",
                new Strategy("retry-with-timeout", parameters)
            )
        );

        // When
        ComposableStep foundFStep = sut.findById(fStep.id);

        // Then
        assertThat(foundFStep).isNotNull();
        assertThat(foundFStep.id).isEqualTo(fStep.id);
        assertThat(foundFStep.name).isEqualTo(fStep.name);
        assertThat(foundFStep.strategy.type).isEqualToIgnoringCase("retry-with-timeout");
        assertThat(foundFStep.strategy.parameters.get("retryDelay")).isEqualTo("10 s");
        assertThat(foundFStep.strategy.parameters.get("timeOut")).isEqualTo("10 s");
    }

    @Test
    public void should_find_saved_step_with_tags_when_search_by_id() {
        // Given
        List<String> tags = Stream.of("zug zug", "dabu").collect(toList());
        final ComposableStep fStep = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep(
                "a thing is connected",
                tags
            )
        );

        // When
        ComposableStep foundFStep = sut.findById(fStep.id);

        // Then
        assertThat(foundFStep).isNotNull();
        assertThat(foundFStep.id).isEqualTo(fStep.id);
        assertThat(foundFStep.name).isEqualTo(fStep.name);
        assertThat(foundFStep.tags).isEqualTo(fStep.tags);
    }

    @Test
    public void should_find_all_steps_when_findAll_called() {
        should_save_all_func_steps_with_multiple_step_types_when_save_scenario();
        List<ComposableStep> all = sut.findAll();
        assertThat(all).hasSize(7);
    }

    @Test
    public void should_save_all_func_steps_with_multiple_step_types_when_save_scenario() {
        // Given
        final ComposableStep f1 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("when"));
        final ComposableStep f21 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("then sub 1"));
        final ComposableStep f22 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("then sub 2 with implementation", "{\"type\": \"debug\"}"));
        final ComposableStep f23 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("then sub 3 with implementation", "  \"type\": \"debug\"  "));
        final ComposableStep f24 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("then sub 4"));
        final ComposableStep f25 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("then sub 5 with implementation", " {\r\"type\": \"debug\"\r} "));
        final ComposableStep f2 = orientDatabaseHelperTest.buildComposableStep("then", f21, f22, f23, f24, f25);
        List<ComposableStep> steps = Arrays.asList(f1, f2);

        // When
        String record1 = sut.save(f1);
        String record2 = sut.save(f2);
        List<String> recordIds = Arrays.asList(record1, record2);

        // Then
        assertScenarioRids(steps, recordIds);
    }

    @Test
    public void should_not_update_func_step_when_name_already_exists() {
        // Given
        String name = "a thing is connected";
        sut.save(orientDatabaseHelperTest.buildComposableStep(name));

        // When
        assertThatThrownBy(() -> sut.save(orientDatabaseHelperTest.buildComposableStep(name)))
            .isInstanceOf(AlreadyExistingComposableStepException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    public void should_update_func_step_when_id_already_exists() {
        // Given
        final ComposableStep subWithImplementation = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("sub with implementation", "{\"type\": \"debug\"}"));
        String name = "a thing is connected";
        String fStepId = sut.save(
            orientDatabaseHelperTest.buildComposableStep(name, subWithImplementation)
        );

        // When
        String newName = "another thing is connected";
        String newSubTechnicalContent = "{\"type\": \"success\"}";
        final ComposableStep newSub_with_implementation = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("sub with implementation", newSubTechnicalContent, subWithImplementation.id));
        String updateFStepId = sut.save(ComposableStep.builder()
            .withName(newName)
            .withId(fStepId)
            .withSteps(singletonList(newSub_with_implementation))
            .build()
        );
        ComposableStep updatedFStep = sut.findById(updateFStepId);

        // Then
        assertThat(fStepId).isEqualTo(updateFStepId);
        assertThat(updatedFStep.name).isEqualTo(newName);
        assertThat(updatedFStep.steps.get(0).implementation.get()).isEqualTo(newSubTechnicalContent);
    }

    @Test
    public void should_find_saved_func_steps_by_page_when_find_called() {
        // Given
        final ComposableStep fStep_11 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("sub something happen 1.1"));
        final ComposableStep fStep_12 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("sub something happen 1.2 with implementation", "{\"type\": \"debug\"}"));
        final ComposableStep fStep_13 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("sub something happen 1.3"));
        final ComposableStep fStep_1 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("sub something happen 1", fStep_11, fStep_12, fStep_13));
        final ComposableStep fStep_2 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("another sub something 2 with implementation", "{\"type\": \"debug\"}"));
        final ComposableStep fStep_3 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("sub something happen 3"));
        final ComposableStep fStepRoot = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("something happen", fStep_1, fStep_2, fStep_3));

        List<ComposableStep> fSteps = Arrays.asList(fStepRoot, fStep_11, fStep_12, fStep_13, fStep_1, fStep_2, fStep_3);

        // When
        long elementPerPage = 2;
        PaginatedDto<ComposableStep> foundFStepsPage1 = findWithPagination(1, elementPerPage);
        PaginatedDto<ComposableStep> foundFStepsPage2 = findWithPagination(3, elementPerPage);
        PaginatedDto<ComposableStep> foundFStepsPage3 = findWithPagination(5, elementPerPage);
        PaginatedDto<ComposableStep> foundFStepsPage4 = findWithPagination(7, elementPerPage);

        // Then
        List<ComposableStep> foundFSteps = new ArrayList<>();
        foundFSteps.addAll(foundFStepsPage1.data());
        foundFSteps.addAll(foundFStepsPage2.data());
        foundFSteps.addAll(foundFStepsPage3.data());
        foundFSteps.addAll(foundFStepsPage4.data());

        assertThat(foundFStepsPage1.data().size()).isEqualTo(elementPerPage);
        assertThat(foundFStepsPage1.totalCount()).isEqualTo(fSteps.size());
        assertThat(foundFStepsPage2.data().size()).isEqualTo(elementPerPage);
        assertThat(foundFStepsPage2.totalCount()).isEqualTo(fSteps.size());
        assertThat(foundFStepsPage3.data().size()).isEqualTo(elementPerPage);
        assertThat(foundFStepsPage3.totalCount()).isEqualTo(fSteps.size());
        assertThat(foundFSteps.size()).isEqualTo(fSteps.size());
        assertThat(foundFSteps).containsExactlyInAnyOrderElementsOf(fSteps);
    }

    @Test
    public void should_find_saved_func_steps_with_filter_when_find_called() {
        // Given
        final ComposableStep fStep_1 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("some thing is set"));
        final ComposableStep fStep_2 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("another thing happens", "{\"type\": \"debug\"}"));
        final ComposableStep fStep_3 = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("the result is beauty"));
        final ComposableStep fStepRoot = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("big root thing", fStep_1, fStep_2, fStep_3));

        // When
        PaginatedDto<ComposableStep> filteredByNameFSteps = findWithFilters("beauty", null, null);
        PaginatedDto<ComposableStep> sortedFSteps = findWithFilters("", "name", null);
        PaginatedDto<ComposableStep> sortedDescendentFSteps = findWithFilters("", "name", "");
        PaginatedDto<ComposableStep> filteredFSteps = findWithFilters("thing", "name", "name");

        // Then
        assertThat(filteredByNameFSteps.data()).containsExactly(fStep_3);
        assertThat(sortedFSteps.data()).containsExactly(fStep_2, fStepRoot, fStep_1, fStep_3);
        assertThat(sortedDescendentFSteps.data()).containsExactly(fStep_3, fStep_1, fStepRoot, fStep_2);
        assertThat(filteredFSteps.data()).containsExactly(fStep_1, fStepRoot, fStep_2);
    }

    @Test
    public void should_find_func_step_parents_when_asked_for() {
        // Given
        ComposableStep fStep_111   = saveAndReload(orientDatabaseHelperTest.buildComposableStep("that 1"));
        ComposableStep fStep_112   = saveAndReload(orientDatabaseHelperTest.buildComposableStep("that 2"));
        ComposableStep fStep_11    = saveAndReload(orientDatabaseHelperTest.buildComposableStep("that", fStep_111, fStep_112));
        ComposableStep fStep_12    = saveAndReload(orientDatabaseHelperTest.buildComposableStep("inner that", fStep_11));
        ComposableStep fStepRoot_1 = saveAndReload(orientDatabaseHelperTest.buildComposableStep("a thing", fStep_11, fStep_12, fStep_11));
        ComposableStep fStep_21    = saveAndReload(orientDatabaseHelperTest.buildComposableStep("this", fStep_11));

        // When
        List<ParentStepId> parentsId = sut.findParents(fStep_11.id);

        // Then
        assertThat(parentsId)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ParentStepId(fStepRoot_1.id, fStepRoot_1.name, false),
                new ParentStepId(fStep_12.id, fStep_12.name, false),
                new ParentStepId(fStep_21.id, fStep_21.name, false));
    }

    @Test
    public void should_find_saved_step_parameters_and_build_correct_execution_parameters_when_search_by_id() {
        // Given
        Map<String, String> actionParameters = Maps.of(
            "action parameter with default value", "default action parameter value",
            "action parameter with no default value", "",
            "another action parameter with default value", "another default action parameter value");
        final ComposableStep actionStep = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep(
                "action with parameters",
                actionParameters
            )
        );

        Map<String, String> firstActionInstanceDataSet = Maps.of(
            "action parameter with default value", "first action instance parameter value",
            "action parameter with no default value", "",
            "another action parameter with default value", "another default action parameter value");
        final ComposableStep firstActionStepInstance = ComposableStep.builder()
            .from(actionStep)
            .withExecutionParameters(firstActionInstanceDataSet)
            .build();

        Map<String, String> secondActionInstanceDataSet = Maps.of(
            "action parameter with default value", "default action parameter value",
            "action parameter with no default value", "second action instance not default value value",
            "another action parameter with default value", "");
        final ComposableStep secondActionStepInstance = ComposableStep.builder()
            .from(actionStep)
            .withExecutionParameters(secondActionInstanceDataSet)
            .build();

        Map<String, String> middleParentParameters = Maps.of(
            "middle parent parameter with default value", "default middle parent parameter value",
            "middle parent parameter with not default value", "",
            "another middle parent parameter with default value", "another default middle parent parameter value");
        final ComposableStep middleParentStep = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep(
                "middle parent with parameters",
                middleParentParameters,
                firstActionStepInstance, secondActionStepInstance
            )
        );
        Map<String, String> middleParentExpectedDataSet = Maps.of(
            "action parameter with no default value", "",
            "another action parameter with default value", "",
            "middle parent parameter with default value", "default middle parent parameter value",
            "middle parent parameter with not default value", "",
            "another middle parent parameter with default value", "another default middle parent parameter value"
        );

        Map<String, String> firstMiddleParentInstanceDataSet = Maps.of(
            "action parameter with no default value", "first middle parent action no default value",
            "another action parameter with default value", "first middle parent action value",
            "middle parent parameter with default value", "default middle parent parameter value",
            "middle parent parameter with not default value", "first middle parent instance not default value value",
            "another middle parent parameter with default value", ""
        );
        final ComposableStep firstMiddleParentStepInstance = ComposableStep.builder()
            .from(middleParentStep)
            .withExecutionParameters(firstMiddleParentInstanceDataSet)
            .build();

        Map<String, String> secondMiddleParentInstanceDataSet = Maps.of(
            "action parameter with no default value", "",
            "another action parameter with default value", "second middle parent action value",
            "middle parent parameter with default value", "second middle parent parameter value",
            "middle parent parameter with not default value", "",
            "another middle parent parameter with default value", "another second middle parent parameter value"
        );
        final ComposableStep secondMiddleParentStepInstance = ComposableStep.builder()
            .from(middleParentStep)
            .withExecutionParameters(secondMiddleParentInstanceDataSet)
            .build();

        Map<String, String> thirdActionInstanceDataSet = Maps.of(
            "action parameter with default value", "third action instance parameter value",
            "action parameter with no default value", "yet another third action instance parameter value",
            "another action parameter with default value", "another third action instance parameter value");
        final ComposableStep thirdActionStepInstance = ComposableStep.builder()
            .from(actionStep)
            .withExecutionParameters(thirdActionInstanceDataSet)
            .build();

        Map<String, String> parentParameters = Maps.of(
            "parent parameter", "parent parameter default value");
        final ComposableStep parentStep = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep(
                "parent with parameters",
                parentParameters,
                firstMiddleParentStepInstance, thirdActionStepInstance, secondMiddleParentStepInstance
            )
        );
        Map<String, String> parentExpectedDataSet = Maps.of(
            "another middle parent parameter with default value", "",
            "action parameter with no default value", "",
            "middle parent parameter with not default value", "",
            "parent parameter", "parent parameter default value"
        );

        // When
        ComposableStep foundAction = sut.findById(actionStep.id);
        ComposableStep foundMiddleParentFStep = sut.findById(middleParentStep.id);
        ComposableStep foundParentFStep = sut.findById(parentStep.id);

        // Then
        assertThat(foundAction.defaultParameters).containsExactlyEntriesOf(actionParameters);
        assertThat(foundAction.executionParameters).containsExactlyEntriesOf(actionParameters);

        assertThat(foundMiddleParentFStep.steps.get(0).defaultParameters).containsExactlyEntriesOf(actionParameters);
        assertThat(foundMiddleParentFStep.steps.get(0).executionParameters).containsExactlyEntriesOf(firstActionInstanceDataSet);
        assertThat(foundMiddleParentFStep.steps.get(1).defaultParameters).containsExactlyEntriesOf(actionParameters);
        assertThat(foundMiddleParentFStep.steps.get(1).executionParameters).containsExactlyEntriesOf(secondActionInstanceDataSet);
        assertThat(foundMiddleParentFStep.defaultParameters).containsExactlyEntriesOf(middleParentParameters);
        assertThat(foundMiddleParentFStep.executionParameters).containsExactlyEntriesOf(middleParentExpectedDataSet);

        assertThat(foundParentFStep.defaultParameters).containsExactlyEntriesOf(parentParameters);
        assertThat(foundParentFStep.executionParameters).containsExactlyEntriesOf(parentExpectedDataSet);
        assertThat(foundParentFStep.steps.get(0).defaultParameters).containsExactlyEntriesOf(middleParentParameters);
        assertThat(foundParentFStep.steps.get(0).executionParameters).containsExactlyEntriesOf(firstMiddleParentInstanceDataSet);
        assertThat(foundParentFStep.steps.get(1).defaultParameters).containsExactlyEntriesOf(actionParameters);
        assertThat(foundParentFStep.steps.get(1).executionParameters).containsExactlyEntriesOf(thirdActionInstanceDataSet);
        assertThat(foundParentFStep.steps.get(2).defaultParameters).containsExactlyEntriesOf(middleParentParameters);
        assertThat(foundParentFStep.steps.get(2).executionParameters).containsExactlyEntriesOf(secondMiddleParentInstanceDataSet);
    }

    @Test
    public void should_delete_step_and_update_edges_when_deleteById_called() {
        // Given
        final ComposableStep step = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("a step", Maps.of("param", "default value")));

        final ComposableStep stepInstance = ComposableStep.builder()
            .from(step)
            .withExecutionParameters(Maps.of("param", ""))
            .build();

        final ComposableStep stepInstanceB = ComposableStep.builder()
            .from(step)
            .withExecutionParameters(Maps.of("param", "hard value"))
            .build();

        final ComposableStep parentFStep = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("a parent step", stepInstance, stepInstanceB)
        );

        // When
        sut.deleteById(step.id);
        ComposableStep parentFoundStep = sut.findById(parentFStep.id);

        // Then
        assertThat(orientDatabaseHelperTest.loadById(step.id)).isNull();
        assertThat(parentFoundStep.steps).isEmpty();
    }

    @Test
    public void changelog_n5_should_update_selenium_tasks() {
        // G
        final ComposableStep step = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("selenium-get", "{\n" +
                "            \"identifier\": \"selenium-get-text\",\n" +
                "            \"inputs\": [ \n" +
                "              {\"name\":\"web-driver\",\"value\":\"${#webDriver}\"},\n" +
                "              {\"name\":\"action\",\"value\":\"\"},\n" +
                "              {\"name\":\"selector\",\"value\":\"//chutney-main-menu/nav/ul/ul/div/div/chutney-menu-item[**menu**]/li/a\"},\n" +
                "              {\"name\":\"by\",\"value\":\"xpath\"},\n" +
                "              {\"name\":\"value\",\"value\":\"\"},\n" +
                "              {\"name\":\"wait\",\"value\":\"5\"},\n" +
                "              {\"name\":\"switchType\",\"value\":\"\"},\n" +
                "              {\"name\":\"menuItemSelector\",\"value\":\"\"}]\n" +
                "        }"));
        final ComposableStep stepOld = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("selenium-get-old", "{\n" +
                "            \"identifier\": \"selenium-get-text\",\n" +
                "            \"inputs\": {\n" +
                "                \"web-driver\": \"${#webDriver}\",\n" +
                "                \"selector\": \"//icg-login//button[@type='submit']/span\",\n" +
                "                \"by\": \"xpath\",\n" +
                "                \"wait\": 2,\n" +
                "                \"value\": \"one value\",\n" +
                "                \"switchType\": \"type\",\n" +
                "                \"menuItemSelector\": \"selector\"\n" +
                "            }\n" +
                "        }"));

        // W
        try (ODatabaseSession dbSession = orientDatabaseHelperTest.orientComponentDB.dbPool().acquire()) {
            OrientChangelog.updateSeleniumTaskParametersRight(dbSession);
        }

        // T
        ComposableStep step1 = sut.findById(step.id);
        assertThat(step1.implementation.get()).doesNotContain("name\":\"action", "name\":\"value", "name\":\"switchType", "name\":\"menuItemSelector");
        ComposableStep stepO = sut.findById(stepOld.id);
        assertThat(stepO.implementation.get()).doesNotContain("action", "value", "switchType", "menuItemSelector");
    }

    @Test
    public void changelog_n12_should_update_sql_tasks() {
        // G
        final ComposableStep step = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("new sql", "{\n" +
                "            \"identifier\": \"sql\",\n" +
                "            \"inputs\": [ \n" +
                "              {\"name\":\"nbLoggedRow\",\"value\":\"42\"}\n" +
                "             ]" +
                "        }"));
        final ComposableStep stepOld = saveAndReload(
            orientDatabaseHelperTest.buildComposableStep("old sql", "{\n" +
                "            \"identifier\": \"sql\",\n" +
                "            \"inputs\": []\n" +
                "        }"));

        // W
        try (ODatabaseSession dbSession = orientDatabaseHelperTest.orientComponentDB.dbPool().acquire()) {
            OrientChangelog.addInputToSqlTask(dbSession);
        }

        // T
        ComposableStep step1 = sut.findById(step.id);
        assertThat(step1.implementation.get()).contains("name\":\"nbLoggedRow\",\"value\":\"42\"");
        ComposableStep stepO = sut.findById(stepOld.id);
        assertThat(stepO.implementation.get()).contains("name\":\"nbLoggedRow\",\"value\":\"\"");
    }

    private ComposableStep saveAndReload(ComposableStep composableStep) {
        return orientDatabaseHelperTest.saveAndReload(sut, composableStep);
    }

    private void assertScenarioRids(List<ComposableStep> scenario, List<String> rids) {
        IntStream.range(0, rids.size())
            .forEach(idx -> {
                String rid = rids.get(idx);
                ComposableStep fStep = sut.findById(rid);
                assertThat(fStep.id).isEqualTo(rid);
                assertComposableStep(scenario.get(idx), fStep);
            });
    }

    private void assertComposableStep(ComposableStep expectedFStep, ComposableStep step) {
        assertComposableStep(
            step,
            expectedFStep.name,
            expectedFStep.implementation,
            expectedFStep.steps.size());

        IntStream.range(0, expectedFStep.steps.size()).forEach(
            idx -> {
                ComposableStep subStep = step.steps.get(idx);
                assertComposableStep(expectedFStep.steps.get(idx), subStep);
            }
        );
    }

    private void assertComposableStep(ComposableStep step, String name, Optional<String> implementation, int functionalChildStepsSize) {
        assertThat(step).isInstanceOf(ComposableStep.class);
        assertThat(step.name).isEqualTo(name);

        if (implementation.isPresent()) {
            assertThat(step.implementation.orElseThrow(() -> new IllegalStateException("Implementation should be set"))).isEqualTo(implementation.get());
        } else {
            assertThat(step.implementation.isPresent()).isFalse();
        }
        assertThat(step.id).isNotEmpty();
        assertThat(step.steps.size()).isEqualTo(functionalChildStepsSize);
    }

    private PaginatedDto<ComposableStep> findWithPagination(long startElementIdx, long limit) {
        return sut.find(
            ImmutablePaginationRequestParametersDto.builder()
                .start(startElementIdx)
                .limit(limit)
                .build(),
            ImmutableSortRequestParametersDto.builder()
                .build(),
            ComposableStep.builder()
                .withName("")
                .withSteps(Collections.emptyList())
                .build()
        );
    }

    private PaginatedDto<ComposableStep> findWithFilters(String name, String sort, String desc) {
        return sut.find(
            ImmutablePaginationRequestParametersDto.builder()
                .start(1L)
                .limit(100L)
                .build(),
            ImmutableSortRequestParametersDto.builder()
                .sort(sort)
                .desc(desc)
                .build(),
            ComposableStep.builder()
                .withName(name)
                .withSteps(Collections.emptyList())
                .build()
        );
    }

    @Test
    public void should_pull_up_empty_parameters_needed_for_executing_a_composable_step() {

        // Given
        Map<String, String> leafDefaultParameters = Maps.of(
            "leaf default param", "leaf default value",
            "leaf empty param", "",
            "leaf second param", "leaf second value"
        );

        final ComposableStep leaf = saveAndReload(
            ComposableStep.builder()
                .withName("leaf")
                .withDefaultParameters(leafDefaultParameters)
                .build()
        );

        Map<String, String> subStepDefaultParameters = Maps.of(
            "substep default param", "substep default value",
            "substep empty param", "",
            "substep second param", "substep second value"
        );
        final ComposableStep subStep = saveAndReload(
            ComposableStep.builder()
                .withName("subStep")
                .withDefaultParameters(subStepDefaultParameters)
                .withSteps(singletonList(leaf))
                .build()
        );

        Map<String, String> parentDefaultParameters = Maps.of(
            "parent default param", "parent default value"
        );
        final ComposableStep parentStep = saveAndReload(
            ComposableStep.builder()
                .withName("parent")
                .withDefaultParameters(parentDefaultParameters)
                .withSteps(singletonList(subStep))
                .build()
        );

        // When
        ComposableStep actualLeaf = sut.findById(leaf.id);
        ComposableStep actualSubStep = sut.findById(subStep.id);
        ComposableStep actualParent = sut.findById(parentStep.id);

        // Then
        assertThat(actualLeaf.defaultParameters).containsExactlyEntriesOf(leafDefaultParameters);
        assertThat(actualLeaf.executionParameters).containsExactlyEntriesOf(leafDefaultParameters);

        assertThat(actualSubStep.steps.get(0).defaultParameters).containsExactlyEntriesOf(leafDefaultParameters);
        assertThat(actualSubStep.steps.get(0).executionParameters).containsExactlyEntriesOf(leafDefaultParameters);
        assertThat(actualSubStep.defaultParameters).containsExactlyEntriesOf(subStepDefaultParameters);
        assertThat(actualSubStep.executionParameters).containsExactlyEntriesOf(Maps.of(
            "leaf empty param", "",
            "substep default param", "substep default value",
            "substep empty param", "",
            "substep second param", "substep second value"
        ));

        assertThat(actualParent.steps.get(0).defaultParameters).containsExactlyEntriesOf(subStepDefaultParameters);
        assertThat(actualParent.steps.get(0).executionParameters).containsExactlyEntriesOf(Maps.of(
            "leaf empty param", "",
            "substep default param", "substep default value",
            "substep empty param", "",
            "substep second param", "substep second value"
        ));
        assertThat(actualParent.defaultParameters).containsExactlyEntriesOf(parentDefaultParameters);
        assertThat(actualParent.executionParameters).containsExactlyEntriesOf(Maps.of(
            "leaf empty param", "",
            "substep empty param", "",
            "parent default param", "parent default value"
        ));
    }

    @Test
    public void should_pull_up_empty_parameters_because_of_a_remove_when_in_use() {
        // Given
        Map<String, String> leafDefaultParameters = Maps.of(
            "leaf default param", "leaf default value",
            "leaf empty param", "",
            "leaf second param", "leaf second value"
        );

        final ComposableStep leaf = saveAndReload(
            ComposableStep.builder()
                .withName("leaf")
                .withDefaultParameters(leafDefaultParameters)
                .build()
        );

        Map<String, String> leafExecutionParameters = Maps.of(
            "leaf default param", "value is override",
            "leaf empty param", "value is set",
            "leaf second param", "" /*value is removed*/
        );
        ComposableStep leafWithOverride = ComposableStep.builder()
            .from(leaf)
            .withExecutionParameters(leafExecutionParameters)
            .build();

        Map<String, String> parentDefaultParameters = Maps.of(
            "parent default param", "parent default value",
            "parent empty param", ""
        );

        final ComposableStep parent = saveAndReload(
            ComposableStep.builder()
                .withName("parent")
                .withDefaultParameters(parentDefaultParameters)
                .withSteps(singletonList(leafWithOverride))
                .build()
        );

        // When
        ComposableStep actualLeaf = sut.findById(leaf.id);
        ComposableStep actualParent = sut.findById(parent.id);

        // Then
        assertThat(actualLeaf.defaultParameters).containsExactlyEntriesOf(leafDefaultParameters);
        assertThat(actualLeaf.executionParameters).containsExactlyEntriesOf(leafDefaultParameters); // Because not in use under a parent step

        assertThat(actualParent.steps.get(0).defaultParameters).isEqualTo(leafDefaultParameters);
        assertThat(actualParent.steps.get(0).executionParameters).containsExactlyEntriesOf(leafExecutionParameters);
        assertThat(actualParent.defaultParameters).containsExactlyEntriesOf(parentDefaultParameters);
        assertThat(actualParent.executionParameters).containsExactlyEntriesOf(Maps.of(
            "leaf second param", "", /*value was removed*/
            "parent default param", "parent default value",
            "parent empty param", ""
        ));
    }

    @Test
    public void should_not_update_parents_relations_when_changing_default_parameters() {
        // Given
        ComposableStep leaf = saveAndReload(ComposableStep.builder()
            .withName("leaf")
            .withDefaultParameters(Maps.of(
                "empty param", "",
                "default param", "default value",
                "second default param", "second default value")
            ).build());
        ComposableStep subStep = saveAndReload(
            ComposableStep.builder()
                .withName("subStep")
                .withSteps(singletonList(ComposableStep.builder()
                    .from(leaf)
                    .withExecutionParameters(Maps.of(
                        "empty param", "value is override",
                        "default param", "value is override")
                    ).build()
                ))
                .build()
        );

        ComposableStep parent = saveAndReload(
            ComposableStep.builder()
                .withName("parent")
                .withSteps(singletonList(subStep))
                .build()
        );

        // Verify everything is setup correctly before updating leaf default parameters
        ComposableStep actualParent = orientDatabaseHelperTest.findByName(parent.name);
        ComposableStep actualSubStep = orientDatabaseHelperTest.findByName(subStep.name);
        assertThat(actualParent.defaultParameters).isEqualTo(emptyMap());
        assertThat(actualParent.executionParameters).isEqualTo(emptyMap());
        assertThat(actualSubStep.defaultParameters).isEqualTo(emptyMap());
        assertThat(actualSubStep.executionParameters).isEqualTo(emptyMap());
        assertThat(actualSubStep.steps.get(0).defaultParameters).isEqualTo(Maps.of(
            "empty param", "",
            "default param", "default value",
            "second default param", "second default value")
        );
        assertThat(actualSubStep.steps.get(0).executionParameters).containsExactlyEntriesOf(Maps.of(
            "empty param", "value is override",
            "default param", "value is override",
            "second default param", "second default value")
        );

        // When update leaf parameters
        ComposableStep updatedLeaf = ComposableStep.builder()
            .from(leaf)
            .withDefaultParameters(Maps.of(
                "empty param", "has value",
                "another empty param", "",
                "toto param", "toto",
                "default param", "updated default value",
                "second default param", "updated second default value"))
            .build();
        sut.save(updatedLeaf);

        // Then
        ComposableStep actualParentAfterUpdate = orientDatabaseHelperTest.findByName(parent.name);
        ComposableStep actualSubStepAfterUpdate = orientDatabaseHelperTest.findByName(subStep.name);
        assertThat(actualParentAfterUpdate.defaultParameters).isEqualTo(emptyMap());
        assertThat(actualParentAfterUpdate.executionParameters).isEqualTo(Maps.of("another empty param", ""));
        assertThat(actualSubStepAfterUpdate.defaultParameters).isEqualTo(emptyMap());
        assertThat(actualSubStepAfterUpdate.executionParameters).isEqualTo(Maps.of("another empty param", ""));
        assertThat(actualSubStepAfterUpdate.steps.get(0).defaultParameters).isEqualTo(Maps.of(
            "empty param", "has value",
            "another empty param", "",
            "toto param", "toto",
            "default param", "updated default value",
            "second default param", "updated second default value")
        );
        assertThat(actualSubStepAfterUpdate.steps.get(0).executionParameters).isEqualTo(Maps.of(
            "empty param", "value is override",
            "another empty param", "",
            "toto param", "toto",
            "default param", "value is override",
            "second default param", "second default value")
        );
    }
}
