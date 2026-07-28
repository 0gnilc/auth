# 后台管理 System 模块重构与开发最终规格

本文记录本轮后台管理 System 模块重构的需求演变、最终业务边界和实现约束。若早期讨论、旧实现或其他临时文档与本文冲突，以本文所列的最终结论为准。

## 一、需求演变与最终边界

最初目标是参考 Vben Playground，实现后台管理员、角色、权限、菜单管理。第一轮设计曾让权限、菜单页面反向关注角色绑定，造成职责倒转。最终职责固定为：

- 管理员管理自己的角色。
- 角色管理自己的权限和菜单。
- 权限和菜单页面不反向管理角色。

由于最初只提供了 GitHub 地址，早期前端实现没有充分遵循 Playground。后续改为直接参考本地代码：

- `/Users/kyhns7/code/vue-vben-admin/playground`
- `/Users/kyhns7/code/vue-vben-admin/playground/src/views/system`

Playground 只作为页面组织、表格表单组合和交互方式的参考，不能作为业务契约直接照搬。最终业务契约以本项目 TypeScript API、Java Controller、Service 和数据库结构为准。

本轮范围包括：

- `system/admin`
- `system/role`
- `system/permission`
- `system/menu`
- `system/i18n-message`
- 上述页面依赖的前端 Adapter、公共 Drawer 辅助能力、动态国际化加载和相关后端接口。

## 二、前端技术与文件组织

- 保留 Element Plus，不引入 `antdv-next`。
- 页面使用 `useVbenVxeGrid`、`useVbenForm`、`useVbenDrawer` 和 `VbenTableAction`。
- 每个 System 页面以 `index.vue` 作为页面入口，配套 `data.ts` 和 `modules/*.vue`。
- 不再保留同一页面下职责重复的 `list.vue`；已有 `list.vue` 内容迁移并合并到 `index.vue`，避免额外路由包装层。
- 角色菜单授权组件命名为 `modules/menu.vue`，不使用冗余的 `menu-drawer.vue` 名称。
- `apps/admin/src/views/system/components` 只放 System 页面真正共用的组件或辅助逻辑，并添加简短的职责注释。
- 所有编辑表单 Drawer、角色/权限多选 Drawer 和菜单树授权 Drawer 都必须检查未保存修改。

### 2.1 查询表单与 Grid 数据契约

- 查询只在用户点击查询、重置或工具栏刷新时执行，`submitOnChange` 保持为 `false`。
- Grid 查询回调的表单参数统一命名为 `args`，不使用含义过窄的 `formValues`。
- 页面查询回调统一返回 `{ list, total }`。后端分页对象仍是 `{ list, totalCount }`，页面负责把 `totalCount` 映射为 `total`。
- VXE 全局响应映射为：

```ts
response: {
  result: 'list',
  total: 'total',
  list: 'list',
}
```

其中 `result` 是 VXE 对非分页结果根路径的配置项，不是页面中 `const result = await ...` 的变量名；分页列表和总数分别读取 `list`、`total`。

- System 列表查询设置 `proxyConfig.showLoading = false`，避免点击查询或重置时整张表出现加载遮罩并产生明显闪烁。

### 2.2 Drawer 未保存确认

- 公共函数使用 `confirmDrawerClose(changed: boolean): Promise<boolean>`。
- 页面直接用 `isEqual` 比较当前值和初始快照，并把是否发生变化的布尔值传入，不再保留额外的 `hasChanges` 包装函数。
- 无修改时直接允许关闭；有修改时使用 Element Plus 确认框询问是否放弃修改。
- 保存成功后关闭 Drawer 不再重复询问。

### 2.3 表单默认按钮

- Admin Element Plus Adapter 中，`DefaultButton` 全局映射为 `ElButton type="default"`，不再使用灰色实心的 `type="info"`。
- 表单重置按钮使用 `DefaultButton`，提交/查询按钮使用 `PrimaryButton`，后者继续映射为 `type="primary"`。

## 三、访问码与按钮鉴权

- 页面按钮使用面向业务动作的访问码，例如 `system:admin:create`、`system:role:manage-menus`，不再把 `POST:/xxx` 形式的接口权限编码直接当作 UI 按钮访问码。
- `getMenuAccessCodes` 返回当前用户获授权且已启用的按钮菜单 `accessCode` 集合；该集合与 `PermissionBo` 的接口权限是两套不同概念。
- 访问码通常只在单个操作位置使用，因此不新增 `SYSTEM_ACCESS` 常量对象，页面直接写语义明确的字符串字面量。
- `VbenTableAction` 的 Admin Adapter 默认注入 `hasPermission`：把 `action.auth` 的字符串或字符串数组交给 `useAccess().hasAccessByCodes`。
- 不再为这一层适配额外创建 `createTableActionPermissionChecker`；Adapter 内使用简短的 `hasPermission` 完成参数归一化和调用即可。
- 页面使用 `VbenTableAction` 时只需声明 `auth`，不需要重复传入 `:has-permission`；显式传入时仍允许覆盖默认实现。
- 新增按钮等非 `VbenTableAction` 操作继续使用 `v-access:code`。
- 前端访问码只控制操作是否可见或可用，后端仍负责最终鉴权。

## 四、后台管理员管理

- 使用分页列表。
- 按用户名、昵称、启用状态查询。
- 支持新增、修改、删除和角色管理。
- 创建时密码必填；编辑时密码可空，只有填写后才修改密码。
- 密码长度为 8 至 32 位，必须同时包含大小写字母、数字和特殊字符，且不允许空白字符。
- 当前管理员不能禁用或删除自己。
- 后端保留两个独立语义：`wasEnabled` 表示本次是否发生“已启用到禁用”的转换，当前用户判断继续单独使用用户 ID 比较；前者还用于决定是否清理被禁用管理员的会话，不能合并成“是否禁用当前管理员”一个变量。
- 行操作直接展示编辑、角色，删除放入更多菜单。
- 每个管理员必须保留内置 `admin` 基线角色，前后端都不能把它取消。

### 4.1 状态列

- 完全采用 Playground 角色页面的状态呈现方式。
- 当前用户拥有 `system:admin:update` 时使用 `CellSwitch`，切换前二次确认并调用更新接口。
- 没有更新访问码时使用只读 `CellTag`，而不是展示不可操作的开关。

### 4.2 角色列与角色 Drawer

- 列表显示角色标识码，不改成角色名称。
- 行内使用 Tag，最多显示前两个角色标识。
- 超出两个时显示可点击的 `+n`；点击后使用 Element Plus Popover 展示全部角色 Tag。
- Popover 中“共 N 个角色”使用小号、浅灰的次要文字样式。
- 角色 Drawer 使用多选框网格，区分已选和可选内容；候选项同时展示角色名称、角色标识和说明。
- `admin` 角色在 Drawer 中保持选中且不可取消。

## 五、角色与权限管理

### 5.1 角色

- 角色列表不分页。
- 支持按编码、名称、是否内置查询。
- 支持新增、修改、删除、权限管理和菜单管理。
- 行操作直接展示编辑、权限、菜单，删除放入更多菜单。
- 权限 Drawer 使用一行多个的复选框列表，并单独展示已选项。
- 菜单 Drawer 使用树选择，采用父子联动；后端保存时仍补齐所选菜单的祖先层级。
- 内置角色不可编辑、删除，也不可修改权限或菜单授权。
- 角色标识使用冒号分隔命名空间，例如 `rbac:manager`、`i18n:manager`，不再使用 `rbac-manager` 一类连字符格式。

### 5.2 权限

- 权限列表不分页。
- 支持按编码、名称、目标标识和是否公开查询。
- 支持新增、修改、删除。
- 新增权限时“公开”默认关闭，用户可以主动开启或关闭。
- 从非公开修改为公开时进行二次确认。
- 内置权限不可编辑或删除。

### 5.3 授权保存命名

角色、权限和菜单关系保存属于“用提交集合替换当前关系”，既可能新增，也可能删除，不使用含义不完整的 `updateXxx`：

- 管理员角色：`saveAdminRoles`，`POST /sys/admin/roles/save`。
- 角色权限：`saveRolePermissions`，`POST /authz/role-permission/save`。
- 角色菜单：`saveRoleMenus`，`POST /authz/role-menu/save`。

Controller、Service、实现类、前端 API、调用点、测试和初始化权限数据统一使用 `saveXxx` 与 `/save`。

## 六、菜单管理

- 使用树状 Grid，不分页。
- 在前端按国际化后的标题、菜单名称、路径或访问码进行关键词过滤；查询框只显示简洁的“关键词”。
- 行操作直接展示新增下级、编辑，删除放入更多菜单。
- 空目录保留在菜单管理和授权树中；运行时没有可用导航后代时从路由树裁剪。
- 删除前展示后代数量和角色授权影响；存在内置后代时禁止删除。

### 6.1 类型与父级联动

- 菜单类型只在新增时选择，保存成功后不可修改；界面提示固定为“保存成功后类型不可再修改”。
- 修改类型只能删除后重新新增，原授权不自动迁移。
- 菜单类型驱动上级菜单候选项；切换类型时直接清空已选上级，不保留复杂的兼容判断。
- “新增下级”只是新增菜单的快捷入口，只预填上级菜单，其余行为与普通“新增菜单”一致。
- 类型切换使用 Playground 菜单表单的按钮式 RadioGroup 布局。
- 菜单 Drawer 使用更宽的表单布局；Divider 上下间距保持紧凑，但 Divider 下方仍保留必要空间。
- 按钮类型不显示“路由配置”等与按钮无关的 Divider 和高级路由字段。

层级规则：

- 目录可以包含目录、页面、内嵌页、外链和按钮。
- 页面只能包含按钮。
- 内嵌页、外链和按钮不能包含子节点。
- 按钮可以放在目录或页面下，用于多个页面共享同一个按钮标识的场景。
- 按钮不能直接放在根节点。
- 前后端都校验自身、后代、循环引用和非法父子关系。

### 6.2 字段与校验

- 目录要求路径。
- 页面要求路径和组件。
- 按钮要求访问码。
- 内嵌页要求路径和 iframe URL。
- 外链要求路径和外部 URL。
- iframe URL 和外链必须是完整的 `http/https` 地址，最大 500 字符。
- HTTP URL 判断复用率不足，不新增公共 `validateHttpUrl`；校验逻辑保留在菜单表单和后端对应位置。
- 高级路由字段按菜单类型显示，切换类型时清理不再适用的字段。
- 菜单状态使用与 Playground 一致的启用/禁用按钮式 RadioGroup。

`ignoreAccess`、`menuVisibleWithForbidden` 可以继续存在于 Vben 全局前端路由类型中，因为它们在前端或联合访问模式下有意义；它们不进入本项目当前菜单管理 API、表单或 Java 菜单模型。

### 6.3 完整更新语义

- 菜单更新接口定义为完整更新，不是部分更新。
- 调用方必须提交除只读字段外的完整菜单数据；省略字段不表示保留原值。
- Service 使用数据库中的同一个菜单对象承接 DTO 全量复制，再统一规范化、校验并 `updateById`。
- DTO 中为 `null` 且对应 BO 使用 `FieldStrategy.ALWAYS` 的可空字段会被明确清空。
- 前端和 Java Service 接口都添加注释说明完整更新语义。

### 6.4 菜单标题国际化边界

- 菜单表只保存标题 Message Key。
- 菜单提交本身不查询、不校验、不保存、不刷新国际化消息。
- 菜单表单中的 `I18nMessageInput` 是独立的消息编辑动作：它先保存国际化消息，成功后才把 Message Key 回写到菜单表单。
- 国际化消息已保存但菜单 Drawer 随后取消或保存失败时，不回滚消息，允许暂时存在未被菜单引用的消息。

## 七、国际化消息管理

### 7.1 分类与消息身份

- 删除国际化领域中的 `client` 客户端概念，统一改为单值 `category` 分类。
- 当前分类常量为 `default`、`admin`，按此顺序返回。
- 分类是具体业务值，接口、数据库和页面直接展示该值，不做国际化翻译或显示名称映射。
- 分类由后端常量维护，不新增分类表或分类管理页面。
- 分类必填，不存在未分类消息。
- 新增消息默认选择 `/categories` 返回的第一项；返回空列表时保持空，新增按钮不可提交有效数据。
- Message Key 是全局唯一身份，分类只用于组织、查询过滤和运行时语言包范围，不参与唯一性判定。
- 已有消息的 Message Key 只读；修改 Key 必须删除后重新新增。
- 分类在国际化消息管理 Drawer 中可修改，修改分类表示把该 Message Key 的全部语言值整体移动到另一个分类。
- 一种语言一条物理记录，唯一索引为 `message_key + locale`；同一 Message Key 的全部语言行必须具有相同分类。
- 同一 Message Key 与其祖先或后代路径冲突在全局范围拒绝，不按分类隔离。

### 7.2 接口

- `POST /sys/i18n-message/categories` 返回后端支持的分类字符串数组。
- `POST /sys/i18n-message/bundle/{category}` 按路径分类加载语言包；Admin 前端固定请求 `/bundle/admin`。
- 不再通过 `X-Client`、`X-Category` 或通用请求拦截器传递分类。
- `POST /sys/i18n-message/page` 使用可选 `category` 过滤。
- `POST /sys/i18n-message/values/{messageKey}` 只按全局 Message Key 查询。
- `POST /sys/i18n-message/remove/{messageKey}` 只按全局 Message Key 删除。
- 查询和删除 Controller 直接使用 `@PathVariable`，不再创建无意义的 `I18nMessageClientDto` 一类包装 DTO。
- `POST /sys/i18n-message/save` 显式提交必填 `category`、`messageKey` 和 `values`。
- 分页项、单条查询和保存结果统一包含 `category`、`messageKey`、`values`。

### 7.3 页面与保存规则

- 页面采用分页 Grid 和直接编辑 Drawer，不在管理 Drawer 内嵌 `I18nMessageInput`。
- `en-US` 必填并作为兜底语言；其他语言可以为空。
- 清空某种非英文语言表示删除该语言值；不能清空英文。
- 整条消息删除必须单独确认。
- 国际化消息只是展示配置，不反查、不阻止、不修改、不级联菜单等业务资源。
- 删除被菜单引用的消息后，菜单继续存在，只可能显示 Message Key。
- 前端不校验动态 Message Key 与 Admin 静态语言包 Key 是否冲突，也不阻止保存；通过人为命名约束规避覆盖。
- 动态消息优先级低于公共静态语言包和 Admin 静态语言包，因此动态消息不能覆盖随版本发布的静态页面文案。
- 保存时修改前或修改后的分类为 `admin`，刷新当前管理端动态语言包。
- 删除 `admin` 消息后刷新当前管理端；删除 `default` 消息时不刷新 Admin。
- 不通知其他浏览器或其他分类的运行客户端。
- 并发编辑采用最后一次保存生效，不新增版本号或乐观锁。

## 八、I18nMessageInput

### 8.1 查询、重置与保存

- Message Key 始终可编辑，包括修改菜单时。
- 打开组件时，当前 Key 非空则自动查询一次并初始化语言输入；Key 为空时不查询。
- 打开后修改 Key 不自动查询，保留当前语言输入，避免覆盖用户草稿。
- Key 改动后必须点击右侧搜索按钮并查询成功，才能保存。
- Message Key 输入框和搜索按钮在 DOM 中是两个独立控件，但组成连续的输入框组，视觉效果类似 suffix icon，不使用会覆盖长文本的绝对定位按钮。
- 查询返回 `null` 或空 `values` 时保留现有语言输入，不进入重置状态。
- 查询返回至少一种语言时先清空全部语言输入，再完整回填查询结果，按钮随后切换为重置。
- 重置只清空语言输入，保留 Key 和本次成功查询状态。
- 查询失败时禁止保存；Key 再次变化后清除旧 Key 的错误状态。
- 忽略过期异步响应；查询或保存期间禁用重复操作。
- 搜索按钮可用时显示 pointer，禁用时显示 not-allowed。
- `en-US` 在编辑区排第一、显示必填标识并强制非空。
- 保存接口失败时保持 Popover 打开并保留草稿。
- 保存成功后才更新组件绑定的 Message Key 并关闭 Popover。

菜单原 Key A 改为 B 表示创建或更新 B，再让菜单引用 B，不是重命名 A。公共组件不新增 `category` Prop，也不内置 `admin`；它只按 Key 查询。菜单页面注入的保存回调追加 `category: 'admin'`，若原 Key 属于其他分类，保存会把整条消息移动到 `admin`。

### 8.2 显示、尺寸和 UI 依赖

- 删除组件自身的 `defaultLocale` Prop。
- Popover 外、仍属于组件本身的只读触发输入框，按当前界面语言、`en-US`、Message Key 的顺序显示。
- 新增 `size: 'large' | 'default' | 'small'`，统一控制外部触发框、Message Key 输入组、查询按钮和编辑区间距。
- 尺寸以当前 Element Plus `2.14.2` 源码为准，外层控件高度分别为 `40px`、`32px`、`24px`，不使用受根字号影响的 `h-10/h-8/h-6`。
- Message Key 输入组聚焦时由整个组合框显示边框状态，不能因内部输入框聚焦而丢失外边框。
- `I18nMessageInput` 内部直接使用的 `@vben-core/shadcn-ui` Input，与表单 Schema 中经 Admin Adapter 映射到 `ElInput` 的 `component: 'Input'` 不是同一个组件。
- 本轮不把 `I18nMessageInput` 改造成可切换 UI 库的注入式组件；若未来需要，应拆分无头状态逻辑和应用层 Renderer，而不是在本轮高度修正中扩大范围。

### 8.3 动态语言包刷新

- 组件保存消息成功后，由菜单页面注入的保存回调刷新 Admin 动态语言包；菜单实体保存不负责刷新。
- 消息保存成功但重新获取或应用全局动态配置失败时，消息仍视为保存成功，更新绑定 Key、关闭 Popover 并提示刷新失败。
- 动态加载失败时保持“未加载”状态并清除进行中的共享 Promise，使现有后续加载入口可以重试；不新增或改写路由守卫流程。

## 九、动态语言加载与弹层层级

- `locales/index.ts` 将消息构建、动态快照应用和真实语言切换分离。
- 动态刷新使用 `setLocaleMessage` 原位更新所有支持语言，不临时切换当前界面语言、Element Plus 或 Day.js。
- 只有用户真正切换语言时才更新第三方组件语言。
- 兜底语言从 `zh-CN` 改为 `en-US`，但不强制当前界面使用英文。
- 动态消息、公共静态消息、Admin 静态消息按“动态 < 公共静态 < Admin 静态”的优先级合并。
- 不新增全局 `usePopupZIndex` 或全局自增层级计数器。
- 不给公共 Alert、Drawer、Modal、Popover 新增 `zIndex` Prop 或参数；原本已有的能力不修改。
- 撤销本轮曾加入 `I18nMessageInput` 和 Alert 的硬编码层级，通过现有 Portal、组件层级和必要的调用方 `style`/既有 Props 解决具体场景。

## 十、API 类型、参数与公共工具

- 前端 API 模块定义聚合资源类型，再用内联 `Pick`、`Omit`、`Partial` 派生操作参数。
- 删除无必要的 `CreateInput`、`UpdateInput`、`SaveInput`，不为每个接口行为创建一个接口类型。
- 请求函数参数统一命名为 `params` 或 `data`，不使用抽象的 `filters`、`input`。
- 表单提交前按页面显式调用公共 `trimToNull`，不在 Form Adapter 中全局隐式修改所有字段。
- 前端 `trimToNull` 位于 `packages/@core/base/shared/src/utils/object.ts`，只处理对象顶层自有字符串属性，把首尾空白去掉并将空字符串转为 `null`，支持可变排除字段。
- 管理员密码排除 trim，保留原始输入并由密码复杂度规则拒绝空白；允许空格具有业务含义的其他字段也必须显式排除。
- 不再保留菜单专用的 `normalizeMenuStrings`。
- Java Bean 非空复制和字符串规范化统一放入 `BeanPropertyUtils`，避免与 Spring Bean 工具类重名。
- `AccessPrincipalUtils` 保持静态工具类，不注入国际化服务。
- `FieldStrategy.ALWAYS` 只标注确实需要把 `null` 写回数据库的可清空字段，不能无差别加到全部 BO 属性。

### 10.1 管理员可空字段的 JSON 语义

- `avatarSpecified`、`descSpecified` 用于区分“请求未携带字段”和“请求明确携带 null”。
- Jackson 反序列化时，请求包含 `"avatar": null` 或 `"desc": null` 会调用对应 `@JsonSetter` 并把 specified 标记为 true；字段缺失时不会调用 setter。
- 因此更新管理员时可以明确清空头像或描述，同时保持真正未提交的字段不变，当前标记逻辑保留。

## 十一、数据库初始化与开发环境

- 初始化脚本必须为内置 `admin` 角色补齐 System 页面及按钮菜单授权，保证新建开发库后 Admin 能获得 `getMenuAccessCodes` 返回的按钮访问码。
- RBAC 管理角色标识迁移为 `rbac:manager`，国际化管理角色标识使用 `i18n:manager`。
- System 页面按钮菜单使用 `system:*` 业务访问码；接口权限仍保留 HTTP 方法和路径编码，两者职责分离。
- 开发数据库重建使用 `apps/server/gnilc-bootstrap/src/main/resources/application-dev.yml` 中的开发环境配置，并按部署 SQL 顺序初始化。

## 十二、测试、验收与交付

- 保留当前 `codex/admin-rbac-management` 分支和已有未提交修改。
- 不提交、不推送、不创建 PR，除非用户后续明确要求。
- 前端运行 lint、Admin 类型检查、单元测试和生产构建，并清理测试警告。
- 后端快速测试运行 `mvn -f apps/server/pom.xml test`。
- Docker 启动后运行完整 `mvn -f apps/server/pom.xml verify`，使用 Testcontainers MySQL 8 和 Redis 8。
- 本地页面检查全部 System 页面、Drawer、菜单类型联动、授权选择、角色 `+n` Popover、I18nMessageInput、未保存确认和访问码控制。
- 登录页面滑块用于防止用户误触；后台接口测试本身不依赖前端 CAPTCHA。手工页面测试时可以直接完成滑块。
- 最终耗时统计区分本轮实测和上次中断任务的估算，并分析主要耗时及优化方式。

## 十三、明确废弃的方案

以下方案在讨论中出现过，但已被后续结论取代，不应重新进入实现：

- 权限或菜单页面反向管理角色。
- 把 Playground 业务接口契约原样照搬到本项目。
- 同一页面同时保留 `list.vue` 与 `index.vue`。
- 全局 `usePopupZIndex`、公共弹层新增层级参数、I18nMessageInput/Alert 硬编码层级。
- 使用 HTTP 接口权限码作为前端按钮访问码。
- 为一次性访问码建立 `SYSTEM_ACCESS` 常量对象。
- `createTableActionPermissionChecker` 形式的额外权限校验包装函数。
- `updateAdminRoles`、`updateRolePermissions`、`updateRoleMenus` 及对应 `/update` 路径。
- 独立的 `hasChanges`、过长或语义不符的 Drawer 关闭函数名。
- 国际化 `client`、请求头传客户端、`client + messageKey` 联合身份。
- 多分类 CSV、未分类消息、国际化分类表。
- Message Key 重命名和 `previousKey`。
- 菜单实体保存时隐式保存或刷新国际化消息。
- 国际化消息反向校验、阻止或级联业务资源。
- 菜单专用 `normalizeMenuStrings` 和低复用率的公共 `validateHttpUrl`。
- 在 Form Adapter 中对所有字符串进行无法排除的全局 trim。

本文即本轮多轮确认后的最终规格。
