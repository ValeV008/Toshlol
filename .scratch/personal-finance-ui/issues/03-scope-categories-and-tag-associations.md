# 03: Scope Categories and Tag associations by Transaction direction

**What to build:** A user can classify Income and Expense Transactions without applying a Category or Category-associated Tag to the wrong kind of Transaction.

**Blocked by:** Personal Finance 02: Record and review Transactions; Personal Finance 03: Organize Transactions with Categories and Tags

**Status:** resolved

## Parent-spec requirements

- [Spec user stories 15–19 and 49](../spec.md#user-stories): Categories remain optional and direction-specific; Category-associated Tags are selectable only with their Category; changing or clearing Category removes Tags associated with the previous Category; renamed Categories and Tags display their current names in historical Transactions. `organizeCategoriesAndTagsWithoutDamagingHistoricalTransactions` verifies optional assignment, direction validation, Tag compatibility and removal, persistence, and renamed historical names. `changeTransactionDirectionScopesCategoriesAndRemovesAssociatedTags` verifies the accessible entry/edit journey and displayed Income Category and remaining Tag state.
- [Spec implementation decisions](../spec.md#implementation-decisions): stable Category and Tag IDs remain on Transactions, while displayed names resolve from the current entities. `organizeCategoriesAndTagsWithoutDamagingHistoricalTransactions` verifies rename behavior through the public repository operations.
- [Spec testing decisions](../spec.md#testing-decisions): verify finance rules through repository operations and critical journeys through accessible Compose semantics. Coverage: `organizeCategoriesAndTagsWithoutDamagingHistoricalTransactions` and `changeTransactionDirectionScopesCategoriesAndRemovesAssociatedTags`.

- [x] Income and Expense Categories are separate sets; a Category belongs to only one direction.
- [x] A Transaction can use only a Category belonging to its Income or Expense direction; Category remains optional.
- [x] A Category-associated Tag can be assigned only when its associated Category is selected on the Transaction; unassociated Tags remain available to either direction.
- [x] When a Transaction's Category changes or is cleared, Tags associated with the previous Category are removed automatically without a notice.
- [x] Historical Transactions retain their Category and Tag references and display renamed entity names correctly.
- [x] Tests verify direction validation, optional Categories, Tag association rules, automatic Tag removal, persistence, and the relevant entry/edit journeys through public behavior seams.

## Answer

Changing a Transaction's direction now clears its previous Category and removes Tags associated with that Category, while preserving independent Tags. The transaction form exposes semantics selectors for direction and Tag compatibility checks. Repository-backed coverage verifies optional and direction-matched Categories, invalid direction rejection, Category and Tag renames on historical Transactions, and automatic removal after Category changes. The API 29 Compose journey verifies switching an edited Expense to Income, selecting an Income Category, disabling the old Category-associated Tag, preserving an independent Tag, and displaying the resulting Income state.

Verification: `testDebugUnitTest`, `ktlintCheck`, and `compileDebugAndroidTestKotlin` passed. The focused repository and Compose tests passed on API 29. The full API 29 suite had one Budget journey timeout; that Budget journey passed when run alone, and is outside this issue's scope.
