package com.chutneytesting.design.infra.storage.campaign;

import static java.util.Optional.ofNullable;

import com.chutneytesting.design.domain.campaign.CampaignExecutionReport;
import com.chutneytesting.design.domain.campaign.ScenarioExecutionReportCampaign;
import com.chutneytesting.design.domain.scenario.ScenarioNotFoundException;
import com.chutneytesting.design.domain.scenario.TestCaseRepository;
import com.chutneytesting.execution.domain.history.ExecutionHistory;
import com.chutneytesting.execution.domain.history.ImmutableExecutionHistory;
import com.chutneytesting.execution.domain.report.ServerReportStatus;
import com.google.common.collect.Lists;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

@Component
public class CampaignExecutionReportMapper implements ResultSetExtractor<List<CampaignExecutionReport>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignExecutionReportMapper.class);

    private final TestCaseRepository repository;

    CampaignExecutionReportMapper(TestCaseRepository repository) {
        this.repository = repository;
    }

    // TODO : Joint should be done in CampaignExecutionRepository.
    @Override
    public List<CampaignExecutionReport> extractData(ResultSet resultset) throws SQLException {
        Map<Long, List<ScenarioExecutionReportCampaign>> scenarioByCampaignId = new HashMap<>();
        Map<Long, CampaignExecutionHolder> campaignExecutionReportByCampaignId = new HashMap<>();
        while (resultset.next()) {
            long campaignExecutionId = resultset.getLong("ID");
            String scenarioId = resultset.getString("SCENARIO_ID");
            String title = resultset.getString("CAMPAIGN_TITLE");
            boolean partialExecution = resultset.getBoolean("PARTIAL_EXECUTION");
            String executionEnvironment = resultset.getString("EXECUTION_ENVIRONMENT");
            String userId = resultset.getString("USER_ID");
            Long campaignId = resultset.getLong("CAMPAIGN_ID");
            String dataSetId = resultset.getString("EXECUTION_DATASET_ID");
            Integer dataSetVersion = ofNullable(resultset.getString("EXECUTION_DATASET_VERSION")).map(Integer::valueOf).orElse(null);

            try {
                String scenarioName = repository.findById(scenarioId).metadata().title();

                ScenarioExecutionReportCampaign scenarioExecutionReport = readScenarioExecutionReport(resultset, scenarioId, scenarioName);
                scenarioByCampaignId.putIfAbsent(campaignExecutionId, Lists.newArrayList());
                campaignExecutionReportByCampaignId.putIfAbsent(campaignExecutionId, new CampaignExecutionHolder(campaignExecutionId, title, partialExecution, executionEnvironment, userId, campaignId, dataSetId, dataSetVersion));
                scenarioByCampaignId.get(campaignExecutionId).add(scenarioExecutionReport);
            } catch (ScenarioNotFoundException snfe) {
                LOGGER.warn("Campaign history reference a no longer existing scenario[" + scenarioId + "]");
            }
        }
        return scenarioByCampaignId.entrySet().stream().
            map(entry -> {
                Long campaignExecutionId = entry.getKey();
                CampaignExecutionHolder campaignExecutionHolder = campaignExecutionReportByCampaignId.get(campaignExecutionId);
                return new CampaignExecutionReport(
                    campaignExecutionId,
                    campaignExecutionHolder.campaignId,
                    entry.getValue(),
                    campaignExecutionHolder.title,
                    campaignExecutionHolder.partialExecution,
                    campaignExecutionHolder.executionEnvironment,
                    campaignExecutionHolder.dataSetId,
                    campaignExecutionHolder.dataSetVersion,
                    campaignExecutionHolder.userId);
            }).collect(Collectors.toList());
    }

    private ScenarioExecutionReportCampaign readScenarioExecutionReport(ResultSet resultset, String scenarioId, String scenarioName) throws SQLException {
        ExecutionHistory.ExecutionSummary execution;
        if (resultset.getLong("SCENARIO_EXECUTION_ID") == -1) {
            execution = ImmutableExecutionHistory.ExecutionSummary.builder()
                .executionId(-1L)
                .testCaseTitle(scenarioName)
                .time(LocalDateTime.now())
                .status(ServerReportStatus.NOT_EXECUTED)
                .duration(0)
                .environment("")
                .user("")
                .build();
        } else {
            execution = mapExecutionWithoutReport(resultset, scenarioName);
        }
        return new ScenarioExecutionReportCampaign(scenarioId, scenarioName, execution);
    }


    private ExecutionHistory.ExecutionSummary mapExecutionWithoutReport(ResultSet rs, String scenarioName) throws SQLException {
        return ImmutableExecutionHistory.ExecutionSummary.builder()
            .executionId(rs.getLong("SCENARIO_EXECUTION_ID"))
            .time(Instant.ofEpochMilli(rs.getLong("EXECUTION_TIME")).atZone(ZoneId.systemDefault()).toLocalDateTime())
            .duration(rs.getLong("DURATION"))
            .status(ServerReportStatus.valueOf(rs.getString("STATUS")))
            .info(ofNullable(rs.getString("INFORMATION")))
            .error(ofNullable(rs.getString("ERROR")))
            .testCaseTitle(scenarioName)
            .environment(rs.getString("ENVIRONMENT"))
            .datasetId(ofNullable(rs.getString("DATASET_ID")))
            .datasetVersion(ofNullable(rs.getString("DATASET_VERSION")).map(Integer::valueOf))
            .user(rs.getString("USER_ID"))
            .build();
    }

    private static class CampaignExecutionHolder {
        public final Long id;
        public final String title;
        public final boolean partialExecution;
        public final String executionEnvironment;
        public final String userId;
        public final Long campaignId;
        public final String dataSetId;
        public final Integer dataSetVersion;

        public CampaignExecutionHolder(Long id, String title, boolean partialExecution, String executionEnvironment, String userId, Long campaignId, String dataSetId, Integer dataSetVersion) {
            this.id = id;
            this.title = title;
            this.partialExecution = partialExecution;
            this.executionEnvironment = executionEnvironment;
            this.userId = userId;
            this.campaignId = campaignId;
            this.dataSetId = dataSetId;
            this.dataSetVersion = dataSetVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CampaignExecutionHolder that = (CampaignExecutionHolder) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

}
