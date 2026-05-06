package com.tree.generic.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 通用树形结构VO基类
 * 所有ID使用String类型
 * 
 * @param <T> 子类自身类型（用于递归）
 */
@Data
public abstract class AbstractTreeVO<T extends AbstractTreeVO<T>> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    private String id;

    /**
     * 父节点ID
     */
    private String parentId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 路径
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
     * 子节点列表
     */
    private List<T> children;
}
