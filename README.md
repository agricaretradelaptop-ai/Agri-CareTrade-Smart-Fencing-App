# Agri CareTrade Smart Fencing V5.2 - Embedded Stock

This version removes the Live Sage connection and keeps the fencing calculator fully self-contained.

- Product catalogue, prices, free stock and purchase-order quantities are stored in `app/src/main/assets/products.json`.
- The Android app loads that embedded stock catalogue directly on startup.
- No Sage bridge URL, credentials, live sync, or server is required.
- Quotes and PDFs identify the source as the embedded stock snapshot.
- To refresh stock later, replace `products.json` with a newer export and rebuild the app.

The app still calculates materials, prices, shortages, additional quantity to order, PDF quotes and email submission.
