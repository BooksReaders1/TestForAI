package com.tree.generic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tree.generic.entity.AbstractTreeEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通用树形结构 Mapper 基接口
 * 
 * @param <T> 实体类型，必须继承 AbstractTreeEntity
 */
public interface AbstractTreeMapper<T extends AbstractTreeEntity<T>> extends BaseMapper<T> {

    /**
     * 获取表名（由子类实现）
     * 默认实现会从实体类的@TableName 注解或类名推断
     */
    default String getTableName() {
        try {
            Class<T> entityClass = getEntityClass();
            T instance = entityClass.getDeclaredConstructor().newInstance();
            return instance.getTableName();
        } catch (Exception e) {
            throw new RuntimeException("无法获取表名，请重写 getTableName() 方法", e);
        }
    }

    /**
     * 获取实体类类型
     */
    @SuppressWarnings("unchecked")
    default Class<T> getEntityClass() {
        return (Class<T>) ((java.lang.reflect.ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    /**
     * 获取结果类型类名（用于 XML 中的 resultType 占位符）
     */
    default String getResultType() {
        return getEntityClass().getName();
    }

    List<T> selectChildrenByParentId(@Param("parentId") String parentId,
                                     @Param("tableName") String tableName,
                                     @Param("resultType") String resultType);

    List<T> selectByLevel(@Param("level") Integer level,
                          @Param("tableName") String tableName,
                          @Param("resultType") String resultType);

    List<T> selectByPathPrefix(@Param("pathPrefix") String pathPrefix,
                               @Param("tableName") String tableName,
                               @Param("resultType") String resultType);

    List<T> selectAllNodes(@Param("tableName") String tableName,
                           @Param("resultType") String resultType);

    List<T> selectByIds(@Param("ids") List<String> ids,
                        @Param("tableName") String tableName,
                        @Param("resultType") String resultType);

    List<T> selectDescendantsByCTE(@Param("rootId") String rootId,
                                   @Param("tableName") String tableName,
                                   @Param("resultType") String resultType);

    List<T> selectDescendantsExcludeRootByCTE(@Param("rootId") String rootId,
                                              @Param("tableName") String tableName,
                                              @Param("resultType") String resultType);

    List<T> selectAncestorsByCTE(@Param("nodeId") String nodeId,
                                 @Param("tableName") String tableName,
                                 @Param("resultType") String resultType);

    List<T> selectPathByCTE(@Param("nodeId") String nodeId,
                            @Param("tableName") String tableName,
                            @Param("resultType") String resultType);

    Long countDescendantsByCTE(@Param("rootId") String rootId,
                               @Param("tableName") String tableName);

    int batchUpdatePathPrefix(@Param("oldPrefix") String oldPrefix, 
                              @Param("newPrefix") String newPrefix,
                              @Param("tableName") String tableName);

    int updateChildrenParentId(@Param("oldParentId") String oldParentId,
                               @Param("newParentId") String newParentId,
                               @Param("tableName") String tableName);
}
