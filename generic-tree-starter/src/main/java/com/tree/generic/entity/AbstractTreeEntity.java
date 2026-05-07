package com.tree.generic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用树形结构实体基类
 * 所有ID使用String类型
 * 
 * @param <T> 子类自身类型（用于递归）
 */
@Data
public abstract class AbstractTreeEntity<T extends AbstractTreeEntity<T>> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID（主键）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 父节点ID（根节点为null或空字符串）
     */
    private String parentId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 层级（从0开始，根节点为0）
     */
    private Integer level;

    /**
     * 路径（格式：/id1/id2/id3/）
     */
    private String path;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 是否叶子节点
     */
    private Boolean isLeaf;

    /**
     * 获取表名
     * 默认实现：优先读取@TableName注解，否则根据类名转换
     * 
     * @return 表名
     */
    public String getTableName() {
        Class<?> clazz = this.getClass();
        TableName tableNameAnnotation = clazz.getAnnotation(TableName.class);
        if (tableNameAnnotation != null && !tableNameAnnotation.value().isEmpty()) {
            return tableNameAnnotation.value();
        }
        // 类名去掉"Entity"后缀，驼峰转下划线
        String simpleName = clazz.getSimpleName();
        if (simpleName.endsWith("Entity")) {
            simpleName = simpleName.substring(0, simpleName.length() - 6);
        }
        return camelCaseToUnderscore(simpleName);
    }

    /**
     * 驼峰转下划线
     */
    private String camelCaseToUnderscore(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
