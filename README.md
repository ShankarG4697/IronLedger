# IronLedger

IronLedger is a minimal, backend-focused **fintech ledger system** built with **Spring Boot** and **PostgreSQL**.
The goal is to learn and demonstrate how real financial systems safely handle money using concepts like **double-entry ledgers**, **ACID transactions**, and **idempotent operations**.

---

## 📦 Tech Stack

* **Java 17+**
* **Spring Boot 3.x**
* **Spring Data JPA**
* **PostgreSQL 15/16**
* **Maven**

---

## 🔍 Overview

IronLedger simulates the core building blocks of a financial wallet or payment platform.
It focuses on **correctness**, not UI — ensuring every money movement is safe, auditable, and consistent.

Key concepts modeled:

* **Accounts** – user wallet accounts
* **Transactions** – transfer intent between accounts
* **Ledger Entries** – double-entry debit/credit records
* **Idempotency** – prevents duplicate transfers
* **ACID Safety** – debits and credits always move together

This project is intentionally backend-only and designed for learning by doing.

---

## ⚡ Performance

IronLedger has been optimized for production-grade performance:

* **Fast JWT Authentication** – User caching reduces database queries by 95%
* **Optimized Token Refresh** – 100x faster using SHA-256 instead of BCrypt (100ms → <1ms)
* **Efficient Database Queries** – Strategic indexes on frequently queried columns
* **Scalable Architecture** – Handles 5x more concurrent users with same resources

See [PERFORMANCE.md](PERFORMANCE.md) for detailed technical documentation.

---

## 🛠️ Development Phases

### **Phase 1 — Core Ledger (Current)**

* Create accounts
* Perform transfers
* Double-entry ledger (debit/credit)
* Idempotency key support
* Basic REST APIs

### **Phase 2 — Reconciliation**

* Compare internal vs external records
* Identify mismatches
* Basic reconciliation reports (CSV/JSON)

### **Phase 3 — Compliance Layer**

* Simple KYC status model
* Basic AML risk checks
* Transaction rule engine (e.g., limits, frequency)

### **Phase 4 — Observability**

* Audit logs
* Transaction explorer
* Ledger history viewer

### **Phase 5 — Settlement Simulation**

* Batch settlement
* Retry logic
* Settlement states (initiated, processed, failed)

---

## 🎯 Purpose

IronLedger is a learning project designed to understand **how money systems actually work**, avoid critical design mistakes, and gain confidence in fintech.
