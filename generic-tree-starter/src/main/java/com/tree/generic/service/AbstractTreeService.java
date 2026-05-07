package com.tree.generic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tree.generic.entity.AbstractTreeEntity;
import com.tree.generic.mapper.AbstractTreeMapper;
import com.tree.generic.vo.AbstractTreeVO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用树形结构 Service 基类
 * 提供内存策略和 CTE 策略两种查询方式
 * 
 * @param <T> 实体类型
 * @param <M> Mapper 类型
 * @param <V> VO 类型
 */
public abstract class AbstractTreeService<T extends AbstractTreeEntity<T>, 
                                           M extends AbstractTreeMapper<T>,
                                           V extends AbstractTreeVO<V>> {

    protected final M mapper;

    public AbstractTreeService(M mapper) {
        this.mapper = mapper;
    }

    /**
     * 获取表名（默认从实体类获取）
     */
    protected String getTableName() {
        return mapper.getTableName();
    }

    // ==================== 内存策略方法 ====================

    /**
     * 【内存策略】构建完整树形结构
     * 适合数据量较小的场景（< 10000 条）
     */
    public List<V> buildTreeWithMemoryStrategy() {
        List<T> allNodes = mapper.selectAllNodes(mapper.getTableName(), mapper.getResultType());
        return buildTreeFromFlatList(allNodes, null);
    }

    /**
     * 【内存策略】从指定父节点构建子树
     */
    public List<V> buildTreeWithMemoryStrategy(String parentId) {
        List<T> allNodes = mapper.selectAllNodes(mapper.getTableName(), mapper.getResultType());
        return buildTreeFromFlatList(allNodes, parentId);
    }

    /**
     * 【内存策略】展开到指定节点
     */
    public List<V> expandToNodeWithMemoryStrategy(String nodeId) {
        List<T> ancestors = getAncestorsWithMemoryStrategy(nodeId);
        if (ancestors.isEmpty()) {
            return buildTreeWithMemoryStrategy();
        }

        List<T> allNodes = mapper.selectAllNodes(mapper.getTableName(), mapper.getResultType());
        Set<String> pathIds = new HashSet<>();
        for (T node : ancestors) {
            pathIds.add(node.getId());
        }
        pathIds.add(nodeId);

        Set<String> childrenOfPath = new HashSet<>();
        for (T node : allNodes) {
            if (pathIds.contains(node.getParentId())) {
                childrenOfPath.add(node.getId());
            }
        }

        List<T> filteredNodes = allNodes.stream()
            .filter(n -> pathIds.contains(n.getId()) || childrenOfPath.contains(n.getId()))
            .collect(Collectors.toList());

        return buildTreeFromFlatList(filteredNodes, getRootParentId());
    }

    /**
     * 【内存策略】获取节点的所有祖先
     */
    public List<T> getAncestorsWithMemoryStrategy(String nodeId) {
        List<T> allNodes = mapper.selectAllNodes(mapper.getTableName(), mapper.getResultType());
        Map<String, T> nodeMap = allNodes.stream()
            .collect(Collectors.toMap(T::getId, n -> n));

        List<T> ancestors = new ArrayList<>();
        T current = nodeMap.get(nodeId);
        while (current != null && current.getParentId() != null && !current.getParentId().isEmpty()) {
            T parent = nodeMap.get(current.getParentId());
            if (parent == null) break;
            ancestors.add(0, parent);
            current = parent;
        }
        return ancestors;
    }

    /**
     * 【内存策略】获取节点的所有子孙
     */
    public List<T> getDescendantsWithMemoryStrategy(String nodeId) {
        List<T> allNodes = mapper.selectAllNodes(mapper.getTableName(), mapper.getResultType());
        List<T> result = new ArrayList<>();
        collectDescendants(nodeId, allNodes, result);
        return result;
    }

    private void collectDescendants(String parentId, List<T> allNodes, List<T> result) {
        for (T node : allNodes) {
            if (parentId.equals(node.getParentId())) {
                result.add(node);
                collectDescendants(node.getId(), allNodes, result);
            }
        }
    }

    // ==================== CTE 策略方法 ====================

    /**
     * 【CTE 策略】构建完整树形结构
     * 适合大数据量场景，需要数据库支持 CTE
     */
    public List<V> buildTreeWithCteStrategy() {
        List<T> roots = mapper.selectByLevel(0, mapper.getTableName(), mapper.getResultType());
        if (roots.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<V> result = new ArrayList<>();
        for (T root : roots) {
            V vo = convertToVO(root);
            List<T> descendants = mapper.selectDescendantsExcludeRootByCTE(root.getId(), mapper.getTableName(), mapper.getResultType());
            vo.setChildren(buildTreeFromFlatList(descendants, root.getId()));
            result.add(vo);
        }
        return result;
    }

    /**
     * 【CTE 策略】从指定根节点构建子树
     */
    public V buildTreeWithCteStrategy(String rootId) {
        T root = mapper.selectById(rootId);
        if (root == null) return null;
        
        V vo = convertToVO(root);
        List<T> descendants = mapper.selectDescendantsExcludeRootByCTE(rootId, mapper.getTableName(), mapper.getResultType());
        vo.setChildren(buildTreeFromFlatList(descendants, rootId));
        return vo;
    }

    /**
     * 【CTE 策略】获取节点的所有祖先
     */
    public List<T> getAncestorsWithCteStrategy(String nodeId) {
        return mapper.selectAncestorsByCTE(nodeId, mapper.getTableName(), mapper.getResultType());
    }

    /**
     * 【CTE 策略】获取节点的所有子孙
     */
    public List<T> getDescendantsWithCteStrategy(String nodeId) {
        return mapper.selectDescendantsExcludeRootByCTE(nodeId, mapper.getTableName(), mapper.getResultType());
    }

    /**
     * 【CTE 策略】获取从根到指定节点的路径
     */
    public List<T> getPathWithCteStrategy(String nodeId) {
        return mapper.selectPathByCTE(nodeId, mapper.getTableName(), mapper.getResultType());
    }

    /**
     * 【CTE 策略】统计子孙数量
     */
    public long countDescendantsWithCteStrategy(String nodeId) {
        Long count = mapper.countDescendantsByCTE(nodeId, mapper.getTableName());
        return count != null ? count : 0L;
    }

    // ==================== 通用辅助方法 ====================

    protected List<V> buildTreeFromFlatList(List<T> nodes, String parentId) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<T>> childrenMap = new HashMap<>();
        for (T node : nodes) {
            String pid = node.getParentId() != null ? node.getParentId() : "";
            childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(node);
        }

        if (parentId == null) {
            parentId = getRootParentId();
        }

        List<T> rootNodes = childrenMap.get(parentId);
        if (rootNodes == null) {
            return new ArrayList<>();
        }

        List<V> result = new ArrayList<>();
        for (T node : rootNodes) {
            V vo = convertToVO(node);
            List<V> children = buildTreeFromFlatList(nodes, node.getId());
            vo.setChildren(children);
            result.add(vo);
        }

        return result;
    }

    protected String getRootParentId() {
        return "";
    }

    @SuppressWarnings("unchecked")
    protected V convertToVO(T entity) {
        try {
            Class<V> voClass = (Class<V>) ((java.lang.reflect.ParameterizedType) getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[2];
            V vo = voClass.getDeclaredConstructor().newInstance();
            
            vo.setId(entity.getId());
            vo.setParentId(entity.getParentId());
            vo.setName(entity.getName());
            vo.setLevel(entity.getLevel());
            vo.setPath(entity.getPath());
            vo.setSortOrder(entity.getSortOrder());
            vo.setIsLeaf(entity.getIsLeaf());
            
            return vo;
        } catch (Exception e) {
            throw new RuntimeException("VO 转换失败", e);
        }
    }

    // ==================== 基础 CRUD 方法 ====================

    public boolean saveNode(T node) {
        if (node.getParentId() == null || node.getParentId().isEmpty()) {
            node.setLevel(0);
            node.setPath("/" + node.getId() + "/");
        } else {
            T parent = mapper.selectById(node.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("父节点不存在：" + node.getParentId());
            }
            node.setLevel(parent.getLevel() + 1);
            node.setPath(parent.getPath() + node.getId() + "/");
        }
        
        if (node.getParentId() != null && !node.getParentId().isEmpty()) {
            T parent = mapper.selectById(node.getParentId());
            if (parent != null) {
                parent.setIsLeaf(false);
                mapper.updateById(parent);
            }
        }
        node.setIsLeaf(true);
        
        return mapper.insert(node) > 0;
    }

    public boolean updateNode(T node) {
        return mapper.updateById(node) > 0;
    }

    public boolean deleteNode(String nodeId) {
        List<T> descendants = mapper.selectDescendantsExcludeRootByCTE(nodeId, mapper.getTableName(), mapper.getResultType());
        for (T descendant : descendants) {
            mapper.deleteById(descendant.getId());
        }
        
        T node = mapper.selectById(nodeId);
        if (node == null) return false;
        
        if (node.getParentId() != null && !node.getParentId().isEmpty()) {
            List<T> siblings = mapper.selectChildrenByParentId(node.getParentId(), mapper.getTableName(), mapper.getResultType());
            if (siblings.size() <= 1) {
                T parent = mapper.selectById(node.getParentId());
                if (parent != null) {
                    parent.setIsLeaf(true);
                    mapper.updateById(parent);
                }
            }
        }
        
        return mapper.deleteById(nodeId) > 0;
    }

    public boolean moveNode(String nodeId, String newParentId) {
        T node = mapper.selectById(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("节点不存在：" + nodeId);
        }

        if (nodeId.equals(newParentId)) {
            throw new IllegalArgumentException("不能移动到自身");
        }
        
        if (newParentId != null && !newParentId.isEmpty()) {
            List<T> descendants = mapper.selectDescendantsExcludeRootByCTE(nodeId, mapper.getTableName(), mapper.getResultType());
            for (T descendant : descendants) {
                if (descendant.getId().equals(newParentId)) {
                    throw new IllegalArgumentException("不能移动到子孙节点下");
                }
            }
        }

        String oldPath = node.getPath();
        String oldParentId = node.getParentId();
        
        if (newParentId == null || newParentId.isEmpty()) {
            node.setParentId("");
            node.setLevel(0);
            node.setPath("/" + nodeId + "/");
        } else {
            T newParent = mapper.selectById(newParentId);
            if (newParent == null) {
                throw new IllegalArgumentException("新父节点不存在：" + newParentId);
            }
            node.setParentId(newParentId);
            node.setLevel(newParent.getLevel() + 1);
            node.setPath(newParent.getPath() + nodeId + "/");
        }
        
        mapper.updateById(node);

        if (!oldPath.equals(node.getPath())) {
            mapper.batchUpdatePathPrefix(oldPath, node.getPath(), mapper.getTableName());
        }

        if (oldParentId != null && !oldParentId.isEmpty()) {
            List<T> oldSiblings = mapper.selectChildrenByParentId(oldParentId, mapper.getTableName(), mapper.getResultType());
            if (oldSiblings.isEmpty()) {
                T oldParent = mapper.selectById(oldParentId);
                if (oldParent != null) {
                    oldParent.setIsLeaf(true);
                    mapper.updateById(oldParent);
                }
            }
        }

        if (newParentId != null && !newParentId.isEmpty()) {
            T newParent = mapper.selectById(newParentId);
            if (newParent != null) {
                newParent.setIsLeaf(false);
                mapper.updateById(newParent);
            }
        }

        return true;
    }
}
