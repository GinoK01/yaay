# Security and limits

This addon includes defensive controls for inventory interaction, claim rate limiting, and hourly quotas.

## Anti-exploit inventory protection

Configured in `limits.yml` under `anti-exploit`:

- `strict-inventory-lock`
- `block-shift-click`
- `block-number-key`
- `block-collect-to-cursor`
- `block-drag`
- `block-creative-actions`
- `claim-lock-enabled`

### How it protects

- Cancels risky inventory actions while menu is open.
- Restricts top-inventory interaction to controlled click handling.
- Applies a claim lock per player to avoid duplicate claim race conditions.

## Claim cooldown

Configured in `limits.yml` under `claim-rate-limit`:

- `enabled`
- `cooldown-ms`

When enabled, repeated claims inside cooldown window are denied with message.

## Hourly limit

Configured in `limits.yml` under `hourly-limit`:

- `enabled`
- `max-items-per-hour`

Behavior:

- Counter is tracked per player.
- Counter resets every hour boundary.
- Item amount is counted against limit.
- If item add fails, limit usage is rolled back.

## Inventory space protection

Before granting item, addon checks whether player inventory has room for the exact item stack.

If no room is available, claim is denied and no quota is consumed.

## Visibility safety

Addon only shows claimable files allowed by both:

- Yamipa storage permission/path rules.
- Addon visibility regex patterns in `config.yml`.

Default patterns:

- `^public/.+`
- `^private/#player#/.+`

## Recovery rewrite for legacy lore

When an old dropped image item with legacy Yamipa lore is picked up, addon can rewrite display metadata so item text remains consistent with addon formatting.

This helps prevent legacy lore from reappearing after place/break flows.
