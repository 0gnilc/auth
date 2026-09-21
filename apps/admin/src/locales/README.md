# locale

每个应用独立维护国际化扩展。本目录负责 Admin 的 Day.js、Element Plus 和应用语言包切换。

应用静态文案直接按业务模块放在 `langs/<locale>/` 中：

```text
langs/en-US/
├── authentication.json
├── systemRole.json
├── systemMenu.json
├── systemAdmin.json
└── ...
```

文件名对应翻译键的顶层命名空间，文件内容从该命名空间的内部字段开始。例如，`systemAdmin.json` 中的 `title` 对应 `$t('systemAdmin.title')`，无需在 JSON 内再嵌套 `systemAdmin`。新增模块时，在各语言目录下添加同名 JSON 即可，加载入口会自动发现。

英文（`en-US`）和中文（`zh-CN`）维护完整模块；豪萨语（`ha-NG`）和约鲁巴语（`yo-NG`）保留已翻译内容，缺失文案由 vue-i18n 回退到英文。不要为了补齐文件而复制英文内容到其他语言。

静态资源按语言延迟加载，合并优先级为：数据库动态消息 < 公共静态消息 < Admin 静态消息。

修改后运行 `pnpm exec vitest run --dom apps/admin/src/locales`，检查资源加载、模块键一致性。
