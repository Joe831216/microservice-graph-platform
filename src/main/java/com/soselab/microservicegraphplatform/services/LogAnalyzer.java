package com.soselab.microservicegraphplatform.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soselab.microservicegraphplatform.bean.elasticsearch.MgpLog;
import com.soselab.microservicegraphplatform.bean.elasticsearch.RequestAndResponseMessage;
import com.soselab.microservicegraphplatform.bean.mgp.AppMetrics;
import com.soselab.microservicegraphplatform.bean.mgp.Status;
import com.soselab.microservicegraphplatform.repositories.elasticsearch.HttpRequestAndResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class LogAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(LogAnalyzer.class);

    @Autowired
    private HttpRequestAndResponseRepository httpRequestAndResponseRepository;
    @Autowired
    private ObjectMapper mapper;

    public AppMetrics getMetrics(String systemName, String appName, String version) {
        List<MgpLog> responseLogs = getRecentResponseLogs(systemName, appName, version, 100);
        AppMetrics metrics = new AppMetrics();
        Integer averageDuration = getAverageResponseDuration(responseLogs);
        if (averageDuration != null) {
            metrics.setAverageDuration(averageDuration);
        }
        //logger.info(systemName + ":" + appName + ":" + version + " : average duration calculate by recent " + responseLogs.size() + " responses: " + metrics.getAverageDuration() + "ms");
        metrics.setStatuses(getResponseStatusMetrics(responseLogs));
        metrics.setErrorCount(getErrorCount(systemName, appName, version));
        //logger.info(systemName + ":" + appName + ":" + version + " : error count: " + metrics.getErrorCount());
        return metrics;
    }

    private List<MgpLog> getRecentResponseLogs(String systemName, String appName, String version, int size) {
        return httpRequestAndResponseRepository.findResponseBySystemNameAndAppNameAndVersion
                (systemName, appName, version, new PageRequest(0, size, new Sort(Sort.Direction.DESC, "@timestamp")));
    }

    private Integer getAverageResponseDuration(List<MgpLog> logs) {
        Integer averageDuration = null;
        if (logs.size() > 0) {
            int logCount = 0;
            int durationCount = 0;
            for (MgpLog log : logs) {
                RequestAndResponseMessage message = null;
                try {
                    message = mapper.readValue(log.getMessage(), RequestAndResponseMessage.class);
                    if (message != null) {
                        logCount++;
                        durationCount += message.getDuration();
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (durationCount > 0) {
                averageDuration = durationCount / logCount;
            }
        }
        return averageDuration;
    }

    private List<Status> getResponseStatusMetrics(List<MgpLog> logs) {
        Map<Integer, Integer> statusCount = new HashMap<>();
        if (logs.size() > 0) {
            for (MgpLog log : logs) {
                RequestAndResponseMessage message = null;
                try {
                    message = mapper.readValue(log.getMessage(), RequestAndResponseMessage.class);
                    if (message != null) {
                        statusCount.merge(message.getStatus(), 1, (oldCount, newCount) -> oldCount + 1);
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        List<Status> statuses = new ArrayList<>();
        statusCount.forEach((status, count) -> {
            statuses.add(new Status(status, count, (float) count/logs.size()));
        });

        return statuses;
    }

    private Integer getErrorCount(String systemName, String appName, String version) {
        return httpRequestAndResponseRepository.findErrorsBySystemNameAndAppNameAndVersion(systemName, appName, version).size();
    }

}
