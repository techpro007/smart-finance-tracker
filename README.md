# 💰 Smart Finance Tracker

A full-featured **personal finance desktop application** built with Java Swing, featuring AI-powered financial advice via the Groq API (Llama 3.1 8B).

---

## 📸 Overview

Smart Finance Tracker helps you take control of your money — log income and expenses, set monthly budgets, visualize spending by category, and get personalized AI advice, all from a sleek dark-themed desktop GUI.

---

## ✨ Features

| Module | Description |
|--------|-------------|
| 📊 **Dashboard** | Live stats — Net Balance, Total Income, Total Expenses, and a Financial Health Score (0–100). Recent transactions and category spending bars. |
| ➕ **Add Transaction** | Log income or expenses with category, description, amount, and date. Auto-saves to CSV instantly. |
| 📋 **Transactions** | Full table of all records with live search/filter, row deletion, and one-click CSV export. |
| 🤖 **AI Advisor** | Sends your financial data to Groq's Llama 3.1-8B model. Get a spending assessment, top concerns, 3 saving tips, and a monthly savings target. |
| 🎯 **Budgets** | Set monthly limits per category. Color-coded progress bars (green/yellow/red) show how close you are to your limit. |
| ⚙️ **Settings** | Manage your Groq API key and clear all transaction data. |

---

## 🛠️ Tech Stack

- **Language:** Java SE (JDK 11+)
- **UI Framework:** Java Swing (JFrame, CardLayout, GridBagLayout, Graphics2D)
- **AI Integration:** Groq REST API — `llama-3.1-8b-instant` model
- **HTTP:** `HttpURLConnection` — no external libraries
- **Data Storage:** Local CSV files (`transactions.csv`, `budgets.csv`)
- **Threading:** `SwingWorker` for non-blocking API calls

---

## 🚀 Getting Started

### Prerequisites
- Java JDK 11 or higher installed
- A free [Groq API key](https://console.groq.com) *(optional — only needed for AI Advisor)*

### Run the App

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/smart-finance-tracker.git
cd smart-finance-tracker

# 2. Compile
javac FinanceTracker.java

# 3. Run
java FinanceTracker
```

That's it — no Maven, no Gradle, no dependencies!

---

## 🤖 Setting Up AI Advisor

1. Go to [console.groq.com](https://console.groq.com) and sign up for free
2. Create an API key (starts with `gsk_...`)
3. In the app, open **Settings** and paste your key
4. Go to **AI Advisor** and click **"Analyse My Spending"**

---

## 📁 File Structure

```
smart-finance-tracker/
│
├── FinanceTracker.java      # Entire application (1,227 lines)
├── transactions.csv         # Auto-created on first transaction
├── budgets.csv              # Auto-created when budgets are set
└── README.md
```

---

## 🧮 Financial Health Score

The app calculates a score out of 100 based on three factors:

| Factor | Max Points | How |
|--------|-----------|-----|
| Savings Ratio | 50 pts | `(income - expense) / income × 100` |
| Activity | 20 pts | Number of transactions logged × 2 |
| Budget Adherence | 30 pts | % of set budgets kept within limit |

---

## 📊 Data Format

**transactions.csv** (pipe-delimited to avoid comma conflicts in descriptions):
```
Expense|Food & Dining|Lunch at café|120.00|2025-01-15
Income|Salary|Monthly salary|25000.00|2025-01-01
```

**budgets.csv:**
```
Food & Dining|3000.0
Transport|1000.0
```

---

## 🎨 UI Design

- Dark navy theme (`#0C0E17` background) with accent blue highlights
- Custom `Graphics2D` painting for rounded icons and progress bars
- Hover effects on all navigation buttons via `MouseAdapter`
- Minimum window size: 920 × 580px

---

## 📝 Expense Categories

`Food & Dining` · `Transport` · `Education` · `Entertainment` · `Shopping` · `Healthcare` · `Rent & Housing` · `Utilities` · `Other`

## 💵 Income Categories

`Salary` · `Pocket Money` · `Freelance` · `Part-time` · `Other Income`

---

## 🔮 Future Improvements

- [ ] Monthly/yearly spending graphs using JFreeChart
- [ ] Export reports as PDF
- [ ] Dark/light theme toggle
- [ ] Multi-currency support
- [ ] Password-protected data encryption

---

## 👨‍💻 Author

Built as a Java programming project demonstrating:
- Object-oriented design in Java
- Desktop GUI development with Swing
- REST API integration without external libraries
- Local file persistence with CSV

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
