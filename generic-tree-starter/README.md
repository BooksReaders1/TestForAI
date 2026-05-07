# Generic Tree Starter - 通用树形结构处理库

基于 Spring Boot + MyBatis-Plus 的通用树形结构处理库，提供完整的增删改查、构造树、展开树、路径查询等功能。

## 特性

- ✅ 所有 ID 使用 String 类型
- ✅ 完整集成 MyBatis-Plus
- ✅ 提供两种查询策略：
  - **内存策略**：适合小数据量场景（< 10000 条）
  - **CTE 策略**：适合大数据量场景（需要 MySQL 8.0+/PostgreSQL 支持）
- ✅ 自动计算层级（level）和路径（path）
- ✅ 支持节点移动（自动更新子树路径）
- ✅ 级联删除子孙节点

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.tree.generic</groupId>
    <artifactId>generic-tree-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 创建实体类（PO）

```java
@Data
@TableName("sys_dept")
public class DeptEntity extends AbstractTreeEntity<DeptEntity> {
    // 可以添加额外字段
    private String remark;
}
```

### 3. 创建 VO 类

```java
@Data
public class DeptVO extends AbstractTreeVO<DeptVO> {
    // 可以添加额外字段
    private String remark;
}
```

### 4. 创建 Mapper 接口

```java
@Mapper
public interface DeptMapper extends AbstractTreeMapper<DeptEntity> {
    // 无需重写任何方法，getTableName() 有默认实现
}
```

### 5. 创建 Service 类

```java
@Service
public class DeptService extends AbstractTreeService<DeptEntity, DeptMapper, DeptVO> {
    
    @Autowired
    public DeptService(DeptMapper mapper) {
        super(mapper);
    }
}
```

### 6. 使用示例

```java
@RestController
@RequestMapping("/dept")
public class DeptController {

    @Autowired
    private DeptService deptService;

    // 构建完整树（内存策略）
    @GetMapping("/tree/memory")
    public List<DeptVO> getTreeMemory() {
        return deptService.buildTreeWithMemoryStrategy();
    }

    // 构建完整树（CTE 策略）
    @GetMapping("/tree/cte")
    public List<DeptVO> getTreeCte() {
        return deptService.buildTreeWithCteStrategy();
    }

    // 从指定节点构建子树
    @GetMapping("/tree/{id}")
    public DeptVO getSubTree(@PathVariable String id) {
        return deptService.buildTreeWithCteStrategy(id);
    }

    // 展开到指定节点
    @GetMapping("/expand/{id}")
    public List<DeptVO> expandToNode(@PathVariable String id) {
        return deptService.expandToNodeWithMemoryStrategy(id);
    }

    // 获取祖先节点
    @GetMapping("/ancestors/{id}")
    public List<DeptEntity> getAncestors(@PathVariable String id) {
        return deptService.getAncestorsWithCteStrategy(id);
    }

    // 获取子孙节点
    @GetMapping("/descendants/{id}")
    public List<DeptEntity> getDescendants(@PathVariable String id) {
        return deptService.getDescendantsWithCteStrategy(id);
    }

    // 保存节点
    @PostMapping
    public boolean save(@RequestBody DeptEntity entity) {
        return deptService.saveNode(entity);
    }

    // 移动节点
    @PutMapping("/move/{id}/{newParentId}")
    public boolean move(@PathVariable String id, @PathVariable String newParentId) {
        return deptService.moveNode(id, newParentId);
    }

    // 删除节点（级联删除）
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return deptService.deleteNode(id);
    }
}
```

## 表结构要求

数据库表需要包含以下字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | VARCHAR(64) | 主键 |
| parent_id | VARCHAR(64) | 父节点 ID |
| name | VARCHAR(100) | 节点名称 |
| level | INT | 层级（从 0 开始） |
| path | VARCHAR(500) | 路径（格式：/id1/id2/id3/） |
| sort_order | INT | 排序号 |
| is_leaf | TINYINT(1) | 是否叶子节点 |

## API 说明

### 内存策略方法
- `buildTreeWithMemoryStrategy()` - 构建完整树
- `buildTreeWithMemoryStrategy(parentId)` - 从指定父节点构建子树
- `expandToNodeWithMemoryStrategy(nodeId)` - 展开到指定节点
- `getAncestorsWithMemoryStrategy(nodeId)` - 获取祖先节点
- `getDescendantsWithMemoryStrategy(nodeId)` - 获取子孙节点

### CTE 策略方法
- `buildTreeWithCteStrategy()` - 构建完整树
- `buildTreeWithCteStrategy(rootId)` - 从指定根节点构建子树
- `getAncestorsWithCteStrategy(nodeId)` - 获取祖先节点
- `getDescendantsWithCteStrategy(nodeId)` - 获取子孙节点
- `getPathWithCteStrategy(nodeId)` - 获取从根到节点的路径
- `countDescendantsWithCteStrategy(nodeId)` - 统计子孙数量

### CRUD 方法
- `saveNode(entity)` - 保存节点（自动计算 level 和 path）
- `updateNode(entity)` - 更新节点
- `deleteNode(nodeId)` - 删除节点（级联删除子孙）
- `moveNode(nodeId, newParentId)` - 移动节点

## 注意事项

1. **CTE 策略**需要数据库支持递归 CTE（MySQL 8.0+、PostgreSQL 等）
2. **内存策略**适合数据量较小的场景，会一次性加载所有节点到内存
3. 根节点的 `parentId` 应为空字符串或 null
4. 移动节点时会自动验证不能移动到自身或子孙节点下
