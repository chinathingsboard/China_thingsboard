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
package org.thingsboard.server.dao.sql.relation;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTimeDao;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;
import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

/**
 * Created by Valerii Sosliuk on 5/29/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaRelationDao extends JpaAbstractDaoListeningExecutorService implements RelationDao {

    @Autowired
    private RelationRepository relationRepository;

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        return service.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeGroup(
                        UUIDConverter.fromTimeUUID(from.getId()),
                        from.getEntityType().name(),
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        return service.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(
                        UUIDConverter.fromTimeUUID(from.getId()),
                        from.getEntityType().name(),
                        relationType,
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        return service.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeGroup(
                        UUIDConverter.fromTimeUUID(to.getId()),
                        to.getEntityType().name(),
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return service.submit(() -> DaoUtil.convertDataList(
                relationRepository.findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(
                        UUIDConverter.fromTimeUUID(to.getId()),
                        to.getEntityType().name(),
                        relationType,
                        typeGroup.name())));
    }

    @Override
    public ListenableFuture<Boolean> checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(() -> relationRepository.findOne(key) != null);
    }

    @Override
    public ListenableFuture<EntityRelation> getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(() -> DaoUtil.getData(relationRepository.findOne(key)));
    }

    private RelationCompositeKey getRelationCompositeKey(EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        return new RelationCompositeKey(fromTimeUUID(from.getId()),
                from.getEntityType().name(),
                fromTimeUUID(to.getId()),
                to.getEntityType().name(),
                relationType,
                typeGroup.name());
    }

    @Override
    public boolean saveRelation(TenantId tenantId, EntityRelation relation) {
        return relationRepository.save(new RelationEntity(relation)) != null;
    }

    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        return service.submit(() -> relationRepository.save(new RelationEntity(relation)) != null);
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        RelationCompositeKey key = new RelationCompositeKey(relation);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    @Override
    public boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return deleteRelationIfExists(key);
    }

    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        RelationCompositeKey key = getRelationCompositeKey(from, to, relationType, typeGroup);
        return service.submit(
                () -> deleteRelationIfExists(key));
    }

    private boolean deleteRelationIfExists(RelationCompositeKey key) {
        boolean relationExistsBeforeDelete = relationRepository.exists(key);
        if (relationExistsBeforeDelete) {
            relationRepository.delete(key);
        }
        return relationExistsBeforeDelete;
    }

    @Override
    public boolean deleteOutboundRelations(TenantId tenantId, EntityId entity) {
        boolean relationExistsBeforeDelete = relationRepository
                .findAllByFromIdAndFromType(UUIDConverter.fromTimeUUID(entity.getId()), entity.getEntityType().name())
                .size() > 0;
        if (relationExistsBeforeDelete) {
            relationRepository.deleteByFromIdAndFromType(UUIDConverter.fromTimeUUID(entity.getId()), entity.getEntityType().name());
        }
        return relationExistsBeforeDelete;
    }

    @Override
    public ListenableFuture<Boolean> deleteOutboundRelationsAsync(TenantId tenantId, EntityId entity) {
        return service.submit(
                () -> {
                    boolean relationExistsBeforeDelete = relationRepository
                            .findAllByFromIdAndFromType(UUIDConverter.fromTimeUUID(entity.getId()), entity.getEntityType().name())
                            .size() > 0;
                    if (relationExistsBeforeDelete) {
                        relationRepository.deleteByFromIdAndFromType(UUIDConverter.fromTimeUUID(entity.getId()), entity.getEntityType().name());
                    }
                    return relationExistsBeforeDelete;
                });
    }

    @Override
    public ListenableFuture<List<EntityRelation>> findRelations(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup, EntityType childType, TimePageLink pageLink) {
        Specification<RelationEntity> timeSearchSpec = JpaAbstractSearchTimeDao.getTimeSearchPageSpec(pageLink, "toId");
        Specification<RelationEntity> fieldsSpec = getEntityFieldsSpec(from, relationType, typeGroup, childType);
        Sort.Direction sortDirection = pageLink.isAscOrder() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = new PageRequest(0, pageLink.getLimit(), sortDirection, "toId");
        return service.submit(() ->
                DaoUtil.convertDataList(relationRepository.findAll(where(timeSearchSpec).and(fieldsSpec), pageable).getContent()));
    }

    private Specification<RelationEntity> getEntityFieldsSpec(EntityId from, String relationType, RelationTypeGroup typeGroup, EntityType childType) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                Predicate fromIdPredicate = criteriaBuilder.equal(root.get("fromId"),  UUIDConverter.fromTimeUUID(from.getId()));
                predicates.add(fromIdPredicate);
                Predicate fromEntityTypePredicate = criteriaBuilder.equal(root.get("fromType"), from.getEntityType().name());
                predicates.add(fromEntityTypePredicate);
            }
            if (relationType != null) {
                Predicate relationTypePredicate = criteriaBuilder.equal(root.get("relationType"), relationType);
                predicates.add(relationTypePredicate);
            }
            if (typeGroup != null) {
                Predicate typeGroupPredicate = criteriaBuilder.equal(root.get("relationTypeGroup"), typeGroup.name());
                predicates.add(typeGroupPredicate);
            }
            if (childType != null) {
                Predicate childTypePredicate = criteriaBuilder.equal(root.get("toType"), childType.name());
                predicates.add(childTypePredicate);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
