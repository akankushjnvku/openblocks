package com.openblocks.api.application;

import static com.openblocks.domain.permission.model.ResourceAction.READ_APPLICATIONS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.openblocks.api.home.SessionUserService;
import com.openblocks.api.util.MoreMapUtils;
import com.openblocks.domain.permission.service.ResourcePermissionService;
import com.openblocks.sdk.constants.DslConstants.CompoundAppDslConstants;

import reactor.core.publisher.Mono;

/**
 * For compound application, remove its sub-applications on which current user has no permissions.
 */
@Component
public class CompoundApplicationDslFilter {

    @Autowired
    private SessionUserService sessionUserService;
    @Autowired
    private ResourcePermissionService resourcePermissionService;

    @SuppressWarnings("unchecked")
    public Mono<Void> removeSubAppsFromCompoundDsl(Map<String, Object> dsl) {
        Map<String, Object> ui = (Map<String, Object>) MapUtils.getMap(dsl, CompoundAppDslConstants.UI, new HashMap<>());
        Map<String, Object> comp = (Map<String, Object>) MapUtils.getMap(ui, CompoundAppDslConstants.COMP, new HashMap<>());

        Set<String> subApplicationIds = getAllSubAppIdsFromCompoundAppDsl(comp);
        return sessionUserService.getVisitorId()
                .flatMap(visitorId -> resourcePermissionService.getMaxMatchingPermission(visitorId, subApplicationIds, READ_APPLICATIONS))
                .map(Map::keySet)
                .map(applicationIdsWithPermissions -> Sets.difference(subApplicationIds, applicationIdsWithPermissions))
                .doOnNext(applicationIdsWithoutPermissions -> removeSubAppsFromCompoundDsl(comp, applicationIdsWithoutPermissions))
                .then();
    }

    @SuppressWarnings("unchecked")
    private void removeSubAppsFromCompoundDsl(Map<String, Object> dsl, Set<String> appIdsNeedRemoved) {

        List<Map<String, Object>> items = MoreMapUtils.getList(dsl, CompoundAppDslConstants.ITEMS, new ArrayList<>());
        Iterator<Map<String, Object>> iterator = items.iterator();
        while (iterator.hasNext()) {
            Map<String, Object> item = iterator.next();
            // for leaf node which has empty items.
            if (isLeaf(item)) {
                boolean hideWhenNoPermission = MapUtils.getBoolean(item, CompoundAppDslConstants.HIDE_WHEN_NO_PERMISSION, true);
                if (!hideWhenNoPermission) {
                    continue;
                }

                Map<String, Object> app = (Map<String, Object>) MapUtils.getMap(item, CompoundAppDslConstants.APP, new HashMap<>());
                String appId = MapUtils.getString(app, CompoundAppDslConstants.APP_ID);
                if (StringUtils.isNotBlank(appId) && appIdsNeedRemoved.contains(appId)) {
                    iterator.remove();
                    continue;
                }
                continue;
            }

            // for non-leaf node.
            // recursive here
            removeSubAppsFromCompoundDsl(item, appIdsNeedRemoved);
            // After removing conditional sub-applications, a non-leaf node possible become a leaf node, in which case
            // we will remove itself too.
            if (isLeaf(item)) {
                iterator.remove();
            }
        }
    }

    private boolean isLeaf(Map<String, Object> item) {
        List<Map<String, Object>> subItems = MoreMapUtils.getList(item, CompoundAppDslConstants.ITEMS, new ArrayList<>());
        return CollectionUtils.isEmpty(subItems);
    }

    /**
     * Recursively find all sub-application ids from the DSL of the compound application.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAllSubAppIdsFromCompoundAppDsl(Map<String, Object> dsl) {
        List<Map<String, Object>> items = MoreMapUtils.getList(dsl, CompoundAppDslConstants.ITEMS, new ArrayList<>());
        return items.stream()
                .map(item -> {
                    // If the item is a leaf node, find its id and return it.
                    if (isLeaf(item)) {
                        Map<String, Object> app = (Map<String, Object>) MapUtils.getMap(item, CompoundAppDslConstants.APP, new HashMap<>());
                        String appId = MapUtils.getString(app, CompoundAppDslConstants.APP_ID);
                        if (StringUtils.isBlank(appId)) {
                            return Collections.<String> emptySet();
                        }
                        return Collections.singleton(appId);
                    }
                    // If the item is a non-leaf node, find sub-application ids recursively and return them.
                    return getAllSubAppIdsFromCompoundAppDsl(item);
                })
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }
}
