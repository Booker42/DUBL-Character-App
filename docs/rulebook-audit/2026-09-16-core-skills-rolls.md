# DUBL 3.69 Rulebook Audit — Character Core, Derived Stats, Skills, Rolls

Source of truth: `Dубль All Stars 3.69 REWORK(1)(2).docx`.

Scope of this pass: pages 3, 5, 7–12, 30, 65–71, 107, 123, 126–129, and 385, plus the shared DUBL 3.69 runtime/catalog code that implements those rules. Golden scenarios remain behavior-parity tests; this ledger records rulebook correctness separately.

## Corrected in this pass

| Area | Rulebook | Previous runtime | Correction |
| --- | --- | --- | --- |
| Equipment load on rolls | Light/medium/heavy load applies `-1/-2/-4` to attacks and every Dexterity check (p.385) | Defense/Reflexes/Run were penalized, but attack presets and generic Dexterity checks were not | One shared `rollLoadPenalty` rule now covers attack checks and Dexterity checks without double-penalizing Dexterity attacks |
| Fortitude quick check | Fortitude includes all effective Fortitude modifiers; Still Mountain adds +1 Fortitude | Aggregate `fortitude` included Still Mountain, `RollContext.FORTITUDE` did not | Quick preset includes `stillMountainBonus` |
| Initiative quick check | Storm Lord counts Speed as +1 higher for Run and Initiative | Aggregate `initiative` included Storm Lord, `RollContext.INITIATIVE` did not | Quick preset includes `stormLordBonus` |
| Android quick Fortitude explanation | Quick rolls must expose the same formula that produced the shared bonus | Android showed a hand-written Constitution + Will formula even when passive bonuses were active | Quick Fortitude now consumes the shared `RollContext.FORTITUDE` preset |
| Two-skill synergy | Use the higher skill rank + half the lower skill rank (p.11); general division rounds upward (p.3) | No reusable runtime rule | Added `SkillCheckRules.synergyCombinedRank` |
| Assistance | Helper needs at least rank 1; total 10 grants +1, then +1 per 4 above (p.11) | No reusable runtime rule | Added `SkillCheckRules.assistanceBonus` |
| Negative current Health | Core mechanics explicitly use Health below 0 (`Неваляшка`, `Борьба за жизнь`, Medicine) | Character normalization and platform damage controls stopped at 0 | Current Health may now remain negative; max-health clamping is preserved and both platforms can apply damage below 0 |
| Critical failure vs target | A natural `1–1` makes the check fail with negative consequences (pp.3, 11) | A large numeric bonus could show `Критический провал` and simultaneously `Выше цели` | Added roll-aware target comparison; critical and confirmed critical failures always resolve as target failure while preserving numeric margin |

## Verified without changes

- Primary attributes start at 0. Size modifies Strength/Speed exactly as the table on pp.8–9.
- Defense = `10 - Size + Speed + Dexterity` before load/passive effects.
- Health = `Constitution * Size + Strength`, with the existing Incredible Health passive layered separately.
- Reflexes = `Speed + Dexterity`; Initiative = `Speed + Perception`; Fortitude = `Constitution + Will` before explicit passive bonuses.
- Endurance base maximum is 3; `Выносливый` adds +1 per rank.
- The full Run base/multiplier table for sizes 1–10 and two legs / three-or-more legs matches the core book.
- Attribute XP progression matches the table. `-5 = -200` is a derived refund: the visible table gives `-5 -> -4 = 30` and cumulative `-4 = -170`, so the cumulative value of `-5` is `-200`; it is not a conflicting rulebook value.
- Skill check base is `skill rank + attribute`. Rank cumulative XP is `[0,10,30,60,100,150,210,280,360,450,550]`.
- Re-importing the real 3.69 book produced the same 27 runtime skill IDs, same rank costs, and zero canonical field differences. `Компьютеры` remains intentionally supplemental because it is used later in the book but is absent from the canonical base skill table, leaving Auto 6 / Auto 12 / untrained use unspecified.
- Core attack mappings match p.123: unarmed/melee weapon use Dexterity, primitive shooting uses Dexterity, mechanical/firearm shooting uses Perception, throwing uses Dexterity. The generic attack chooser currently exposes both valid Shooting attributes because weapon type is not part of the roll context.
- Grapple, trip, push, knockdown, feint, disarm, break-item and parry skill/attribute families match pp.126–129 at the generic context level.

## Explicitly unresolved / deferred

These are not silently guessed in this pass.

1. **Double + advantage/hindrance ordering.** The book defines extra dice and doubles, but does not unambiguously state whether a double is detected before or after choosing the retained two dice when extra dice are present. Existing ambiguity handling remains.
2. **Synergy of more than two skills.** The book says two or more skills may synergize, but the formula only clearly specifies the higher rank plus half of the lower rank. The new shared helper intentionally models the unambiguous two-skill case only.
3. **Weapon-property enforcement for Parry/Disarm.** Heavy weapons force Strength and light weapons force Dexterity. The current roll context has no selected-equipment identity, so it exposes both attributes. Fix this when equipment-aware combat roll context is introduced rather than guessing from skill alone.
4. **Primitive vs mechanical Shooting enforcement.** The rulebook switches Shooting from Dexterity to Perception by weapon type. The current generic chooser exposes both; strict enforcement needs the selected weapon in the roll context.
5. **Auto 6 / Auto 12.** Canonical metadata is imported, but there is no complete shared execution workflow for time/safety/critical-failure eligibility yet.
6. **Risk and prolonged checks.** Rules are present in the core book but are not yet modeled as application-level workflows.
7. **Racial/species base Health.** The book permits an additional racial base health pool. The current character model has a manual max-health override but no canonical additive species/race component. Defer until race/species content is modeled.
8. **Armor, resistance, vulnerability as first-class effective stats.** The core book defines them, but the current sheet does not yet maintain a complete combat-effective model. Audit/fix with Equipment/Combat rather than inventing a partial core representation.
9. **Contextual Chi/development effects beyond the already-owned passive components.** Examples such as Storm Lord's thrown-attack modifier belong in the dedicated Development/Chi audit.
10. **Automatic unconscious/death state.** Negative Health is now representable, but the core model does not infer unconscious/dead state from Health alone. The book routes those outcomes through abilities, lethal-damage context, and Fortitude thresholds; implement them during the Combat/Conditions pass rather than guessing from a number.
11. **Temporary/permanent attribute damage/drain.** The core book distinguishes attribute damage/drain and has effects at `-6`; the current character model only has base + generic bonus. This needs a dedicated effective-attribute/status model in the Conditions/Combat pass.
12. **Critical `6–6` against an unmet attack target.** Page 123 states a `6–6` attack is critical only if the total is sufficient to hit. Generic roll UI lacks attack-target semantics, so this pass does not force generic `6–6` into success or failure. Add attack-specific outcome resolution when combat target context is modeled.

## Regression contract added

- `shared/src/commonTest/.../RulebookCoreSkillsRollsTest.kt` checks load penalties, Fortitude/Initiative preset parity, negative Health preservation, synergy, and assistance under the project Kotlin toolchain.
- `shared/src/commonTest/.../RollRulesTest.kt` locks target-aware critical-failure resolution.
- `tools/tests/test_rulebook_core_skills_rolls_contract.py` prevents the platform/source layer from regressing these boundaries.
- `tools/tests/kotlin/SkillCheckRulesHarness.kt` executes the new pure skill rules without Compose or platform dependencies.
- Linux parity CI runs the new source contract before `:shared:desktopTest` and Compose compilation.
