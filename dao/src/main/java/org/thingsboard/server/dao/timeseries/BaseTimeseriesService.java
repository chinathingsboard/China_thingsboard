/**
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.timeseries;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class BaseTimeseriesService implements TimeseriesService {

    private static final int INSERTS_PER_ENTRY = 3;
    private static final int DELETES_PER_ENTRY = INSERTS_PER_ENTRY;

    @Value("${database.ts_max_intervals}")
    private long maxTsIntervals;

    @Autowired
    private TimeseriesDao timeseriesDao;

    @Autowired
    private EntityViewService entityViewService;

    @Override
    public ListenableFuture<List<TsKvEntry>> findAll(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        validate(entityId);
        queries.forEach(this::validate);
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            EntityView entityView = entityViewService.findEntityViewById(tenantId, (EntityViewId) entityId);
            List<ReadTsKvQuery> filteredQueries =
                    queries.stream()
                            .filter(query -> entityView.getKeys().getTimeseries().isEmpty() || entityView.getKeys().getTimeseries().contains(query.getKey()))
                            .collect(Collectors.toList());
            return timeseriesDao.findAllAsync(tenantId, entityView.getEntityId(), updateQueriesForEntityView(entityView, filteredQueries));
        }
        return timeseriesDao.findAllAsync(tenantId, entityId, queries);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, Collection<String> keys) {
        validate(entityId);
        List<ListenableFuture<TsKvEntry>> futures = Lists.newArrayListWithExpectedSize(keys.size());
        keys.forEach(key -> Validator.validateString(key, "Incorrect key " + key));
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            EntityView entityView = entityViewService.findEntityViewById(tenantId, (EntityViewId) entityId);
            List<String> filteredKeys = new ArrayList<>(keys);
            if (entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null &&
                    !entityView.getKeys().getTimeseries().isEmpty()) {
                filteredKeys.retainAll(entityView.getKeys().getTimeseries());
            }
            List<ReadTsKvQuery> queries =
                    filteredKeys.stream()
                            .map(key -> {
                                long endTs = entityView.getEndTimeMs() != 0 ? entityView.getEndTimeMs() : Long.MAX_VALUE;
                                return new BaseReadTsKvQuery(key, entityView.getStartTimeMs(), endTs, 1, "DESC");
                            })
                            .collect(Collectors.toList());

            if (queries.size() > 0) {
                return timeseriesDao.findAllAsync(tenantId, entityView.getEntityId(), queries);
            } else {
                return Futures.immediateFuture(new ArrayList<>());
            }
        }
        keys.forEach(key -> futures.add(timeseriesDao.findLatest(tenantId, entityId, key)));
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId) {
        validate(entityId);
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            EntityView entityView = entityViewService.findEntityViewById(tenantId, (EntityViewId) entityId);
            if (entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null &&
                    !entityView.getKeys().getTimeseries().isEmpty()) {
                return findLatest(tenantId, entityId, entityView.getKeys().getTimeseries());
            } else {
                return Futures.immediateFuture(new ArrayList<>());
            }
        } else {
            return timeseriesDao.findAllLatest(tenantId, entityId);
        }
    }

    @Override
    public ListenableFuture<List<Void>> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry) {
        validate(entityId);
        if (tsKvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        }
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(INSERTS_PER_ENTRY);
        saveAndRegisterFutures(tenantId, futures, entityId, tsKvEntry, 0L);
        return Futures.allAsList(futures);
    }

    @Override
    public ListenableFuture<List<Void>> save(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntries, long ttl) {
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(tsKvEntries.size() * INSERTS_PER_ENTRY);
        for (TsKvEntry tsKvEntry : tsKvEntries) {
            if (tsKvEntry == null) {
                throw new IncorrectParameterException("Key value entry can't be null");
            }
            saveAndRegisterFutures(tenantId, futures, entityId, tsKvEntry, ttl);
        }
        return Futures.allAsList(futures);
    }

    private void saveAndRegisterFutures(TenantId tenantId, List<ListenableFuture<Void>> futures, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        if (entityId.getEntityType().equals(EntityType.ENTITY_VIEW)) {
            throw new IncorrectParameterException("Telemetry data can't be stored for entity view. Read only");
        }
        futures.add(timeseriesDao.savePartition(tenantId, entityId, tsKvEntry.getTs(), tsKvEntry.getKey(), ttl));
        futures.add(timeseriesDao.saveLatest(tenantId, entityId, tsKvEntry));
        futures.add(timeseriesDao.save(tenantId, entityId, tsKvEntry, ttl));
    }

    private List<ReadTsKvQuery> updateQueriesForEntityView(EntityView entityView, List<ReadTsKvQuery> queries) {
        return queries.stream().map(query -> {
            long startTs;
            if (entityView.getStartTimeMs() != 0 && entityView.getStartTimeMs() > query.getStartTs()) {
                startTs = entityView.getStartTimeMs();
            } else {
                startTs = query.getStartTs();
            }

            long endTs;
            if (entityView.getEndTimeMs() != 0 && entityView.getEndTimeMs() < query.getEndTs()) {
                endTs = entityView.getEndTimeMs();
            } else {
                endTs = query.getEndTs();
            }
            return new BaseReadTsKvQuery(query.getKey(), startTs, endTs, query.getInterval(), query.getLimit(), query.getAggregation());
        }).collect(Collectors.toList());
    }

    @Override
    public ListenableFuture<List<Void>> remove(TenantId tenantId, EntityId entityId, List<DeleteTsKvQuery> deleteTsKvQueries) {
        validate(entityId);
        deleteTsKvQueries.forEach(BaseTimeseriesService::validate);
        List<ListenableFuture<Void>> futures = Lists.newArrayListWithExpectedSize(deleteTsKvQueries.size() * DELETES_PER_ENTRY);
        for (DeleteTsKvQuery tsKvQuery : deleteTsKvQueries) {
            deleteAndRegisterFutures(tenantId, futures, entityId, tsKvQuery);
        }
        return Futures.allAsList(futures);
    }

    private void deleteAndRegisterFutures(TenantId tenantId, List<ListenableFuture<Void>> futures, EntityId entityId, DeleteTsKvQuery query) {
        futures.add(timeseriesDao.remove(tenantId, entityId, query));
        futures.add(timeseriesDao.removeLatest(tenantId, entityId, query));
        futures.add(timeseriesDao.removePartition(tenantId, entityId, query));
    }

    private static void validate(EntityId entityId) {
        Validator.validateEntityId(entityId, "Incorrect entityId " + entityId);
    }

    private void validate(ReadTsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("ReadTsKvQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect ReadTsKvQuery. Key can't be empty");
        } else if (query.getAggregation() == null) {
            throw new IncorrectParameterException("Incorrect ReadTsKvQuery. Aggregation can't be empty");
        }
        if(!Aggregation.NONE.equals(query.getAggregation())) {
            long step = Math.max(query.getInterval(), 1000);
            long intervalCounts = (query.getEndTs() - query.getStartTs()) / step;
            if (intervalCounts > maxTsIntervals || intervalCounts < 0) {
                throw new IncorrectParameterException("Incorrect TsKvQuery. Number of intervals is to high - " + intervalCounts + ". " +
                        "Please increase 'interval' parameter for your query or reduce the time range of the query.");
            }
        }
    }

    private static void validate(DeleteTsKvQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("DeleteTsKvQuery can't be null");
        } else if (isBlank(query.getKey())) {
            throw new IncorrectParameterException("Incorrect DeleteTsKvQuery. Key can't be empty");
        }
    }
}
