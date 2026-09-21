# Internationalization

This document maps the current internationalization owners. Domain meaning remains in the [Admin System glossary](../../apps/admin/CONTEXT.md), while durable message-lifecycle decisions remain in [ADR-0010](../adr/0010-keep-dynamic-i18n-auxiliary-to-business-resources.md) and [ADR-0011](../adr/0011-use-global-message-keys-and-category-scoped-bundles.md).

## Message Sources

| Content | Authoritative source | Presentation |
| --- | --- | --- |
| Page, control, and validation labels | Frontend static JSON bundles | Admin |
| Backend response and validation text | Module-owned Spring `MessageSource` bundles | Server response |
| Menu titles and operator-managed business text | `sys_i18n` dynamic messages | Admin runtime bundle |

Static application messages and dynamic database messages have different locale sets. Do not infer one set from the other.

## Locale Sets

- Frontend static messages support `en-US`, `zh-CN`, `ha-NG`, and `yo-NG`. [`SUPPORTED_LOCALES`](../../packages/@core/base/shared/src/constants/locales.ts) is the owner.
- Backend static messages support the same four locales. [`SupportedLocale`](../../apps/server/gnilc-common/gnilc-common-core/src/main/java/com/gnilc/common/i18n/SupportedLocale.java) is the owner.
- Dynamic messages currently support `zh-CN` and `en-US`. [`I18nMessageConstants`](../../apps/server/gnilc-core/src/main/java/com/gnilc/core/i18n/I18nMessageConstants.java) is the owner.
- `en-US` is the fallback locale. Every dynamic message requires an English value; the Chinese value is optional.

Adding a static locale requires matching frontend and Server bundles. Adding a dynamic locale is a separate change to the dynamic-message constants, persistence validation, Admin editor, bundle tests, and presentation.

## Backend Static Messages

Each Maven module owns its message keys and localized `.properties` files. The application composition lists the active basenames; shared modules do not own business-specific text.

Requests select a supported locale through `Accept-Language`. Missing or unsupported values fall back to `en-US`. Backend errors are resolved at their owning source as recorded by [ADR-0003](../adr/0003-resolve-client-facing-backend-errors-at-source.md); frontend request-error presentation remains at each application's boundary under [ADR-0018](../adr/0018-inject-request-error-messages-at-the-application-boundary.md).

## Dynamic Messages

Dynamic messages use one global Message Key identity. Category is a mutable grouping and runtime bundle scope, not part of identity. The supported categories are `default` and `admin`.

The current management and bundle interface is owned by [`I18nMessageController`](../../apps/server/gnilc-core/src/main/java/com/gnilc/core/i18n/controller/I18nMessageController.java):

```text
POST /sys/i18n-message/bundle/{category}
POST /sys/i18n-message/categories
POST /sys/i18n-message/page
POST /sys/i18n-message/values/{messageKey}
POST /sys/i18n-message/create
POST /sys/i18n-message/save
POST /sys/i18n-message/remove/{messageKey}
```

Admin loads the `admin` bundle after authentication. [`dynamic.ts`](../../apps/admin/src/locales/dynamic.ts) owns session loading and reload behavior; [`index.ts`](../../apps/admin/src/locales/index.ts) owns message composition. Dynamic messages have the lowest priority, shared static messages override them, and Admin static messages have the highest priority. A dynamic-load failure leaves static messages available and may be retried later.

## Admin Editing

The Admin message page creates, edits, queries, and removes dynamic messages. Message Key is immutable during edit; changing identity requires creating a new message and removing the old one. Saving a message may move all of its locale rows to another supported category.

`I18nMessageInput` is the reusable Message Key editor for business forms. Its implementation and interface are owned by [`packages/effects/common-ui`](../../packages/effects/common-ui/src/components/i18n-message-input/). It saves dynamic text independently from the enclosing business resource, as required by ADR-0010.

## Change Checklist

- Update the owning constant or interface rather than copying a supported-value list into another document.
- Keep static and dynamic locale changes independent unless the requirement explicitly covers both.
- Preserve global Message Key uniqueness, category consistency, and ancestor/descendant conflict rejection.
- Keep business-resource persistence independent from dynamic-message persistence.
- Update the focused Server and Admin tests that exercise the changed owner.
