# Transfers and Account Balances

Although the initial MVP excluded Transfers, the app will model movement between a user's Accounts as a distinct Transfer, not as Income plus Expense, and calculate signed Account balances from an optional opening balance and dated movements. Transfers update both Accounts but stay out of Income, Expense, and Budget totals; their paired list views and inclusion in Backups add model and migration complexity, accepted to keep internal movements from misrepresenting earnings or spending and to make Account balances explainable.
