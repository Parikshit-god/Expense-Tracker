package com.example.ExpenseTracker;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    DatabaseHelper db;
    SharedPreferences sharedPreferences;

    // Layouts
    View layoutLogin, layoutRegister, layoutDashboard, layoutAddTransaction;

    // Dashboard Components
    TextView tvUserName, tvTotalBalance, tvTotalIncome, tvTotalExpense;
    RecyclerView recyclerView;

    // Add Transaction Components
    Spinner spinnerCategory;
    EditText etAmount, etDescription;
    Button btnTypeIncome, btnTypeExpense, btnSelectDate;

    String selectedType = "Expense";
    String currentUserEmail;
    long selectedDateMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("NeonExpPrefs", MODE_PRIVATE);

        // --- FIND VIEWS ---
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutRegister = findViewById(R.id.layoutRegister);
        layoutDashboard = findViewById(R.id.layoutDashboard);
        layoutAddTransaction = findViewById(R.id.layoutAddTransaction);

        tvUserName = findViewById(R.id.tvUserName);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        spinnerCategory = findViewById(R.id.spinnerCategory);
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        btnTypeIncome = findViewById(R.id.btnTypeIncome);
        btnTypeExpense = findViewById(R.id.btnTypeExpense);
        btnSelectDate = findViewById(R.id.btnSelectDate);

        // --- 1. CHECK LOGIN STATUS ---
        String savedEmail = sharedPreferences.getString("EMAIL", null);
        if(savedEmail != null) {
            currentUserEmail = savedEmail;
            showDashboard();
        } else {
            showLogin();
        }

        // --- LOGIN/REGISTER ACTIONS ---
        EditText etLoginEmail = findViewById(R.id.etLoginEmail);
        EditText etLoginPass = findViewById(R.id.etLoginPass);

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etLoginEmail.getText().toString();
            String pass = etLoginPass.getText().toString();
            if(db.checkUser(email, pass)) {
                currentUserEmail = email;
                sharedPreferences.edit().putString("EMAIL", email).apply();
                showDashboard();
            } else {
                Toast.makeText(this, "Invalid Login", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.tvGoToRegister).setOnClickListener(v -> {
            layoutLogin.setVisibility(View.GONE);
            layoutRegister.setVisibility(View.VISIBLE);
        });

        EditText etRegName = findViewById(R.id.etRegName);
        EditText etRegEmail = findViewById(R.id.etRegEmail);
        EditText etRegPass = findViewById(R.id.etRegPass);

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            if(db.registerUser(etRegName.getText().toString(), etRegEmail.getText().toString(), etRegPass.getText().toString())) {
                Toast.makeText(this, "Registered!", Toast.LENGTH_SHORT).show();
                layoutRegister.setVisibility(View.GONE);
                layoutLogin.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> {
            layoutRegister.setVisibility(View.GONE);
            layoutLogin.setVisibility(View.VISIBLE);
        });

        // --- DASHBOARD ACTIONS ---
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            layoutAddTransaction.setVisibility(View.VISIBLE);
            loadSpinnerCategories();
            selectedDateMillis = System.currentTimeMillis(); // Default to now
            updateDateButtonText();
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            sharedPreferences.edit().remove("EMAIL").apply();
            currentUserEmail = null;
            showLogin();
        });

        // --- ADD TRANSACTION ACTIONS ---
        findViewById(R.id.btnCancel).setOnClickListener(v -> layoutAddTransaction.setVisibility(View.GONE));

        btnSelectDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDateMillis);
            new DatePickerDialog(this, (view, year, month, day) -> {
                cal.set(year, month, day);
                selectedDateMillis = cal.getTimeInMillis();
                updateDateButtonText();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTypeIncome.setOnClickListener(v -> {
            selectedType = "Income";
            btnTypeIncome.setTextColor(Color.parseColor("#39FF14")); // Neon Green
            btnTypeExpense.setTextColor(Color.parseColor("#9E9E9E")); // Grey
        });

        btnTypeExpense.setOnClickListener(v -> {
            selectedType = "Expense";
            btnTypeExpense.setTextColor(Color.parseColor("#FF1744")); // Neon Red
            btnTypeIncome.setTextColor(Color.parseColor("#9E9E9E")); // Grey
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String amt = etAmount.getText().toString();
            String desc = etDescription.getText().toString();
            String cat = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "General";
            if (!amt.isEmpty()) {
                db.addTransaction(currentUserEmail, Double.parseDouble(amt), cat, selectedType, desc, selectedDateMillis);
                layoutAddTransaction.setVisibility(View.GONE);
                etAmount.setText("");
                etDescription.setText("");
                refreshDashboard();
            }
        });
    }

    private void updateDateButtonText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        btnSelectDate.setText("Date: " + sdf.format(new Date(selectedDateMillis)));
    }

    private void showLogin() {
        layoutDashboard.setVisibility(View.GONE);
        layoutRegister.setVisibility(View.GONE);
        layoutAddTransaction.setVisibility(View.GONE);
        layoutLogin.setVisibility(View.VISIBLE);
    }

    private void showDashboard() {
        layoutLogin.setVisibility(View.GONE);
        layoutRegister.setVisibility(View.GONE);
        layoutDashboard.setVisibility(View.VISIBLE);
        tvUserName.setText("Hello, " + db.getUserName(currentUserEmail));
        refreshDashboard();
    }

    private void refreshDashboard() {
        double income = db.getTotal(currentUserEmail, "Income");
        double expense = db.getTotal(currentUserEmail, "Expense");
        double balance = income - expense;

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        tvTotalBalance.setText(fmt.format(balance));
        tvTotalIncome.setText(fmt.format(income));
        tvTotalExpense.setText(fmt.format(expense));

        List<Transaction> list = db.getRecentTransactions(currentUserEmail);
        recyclerView.setAdapter(new TransactionAdapter(list, transaction -> {
            // Delete confirmation
            new AlertDialog.Builder(this)
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure you want to delete this?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        db.deleteTransaction(transaction.id);
                        refreshDashboard();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }));
    }

    private void loadSpinnerCategories() {
        List<String> cats = db.getCategories();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cats) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(Color.WHITE);
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.WHITE);
                view.setBackgroundColor(Color.parseColor("#2C2C2C"));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    // ============ RECYCLERVIEW ADAPTER ============
    interface OnItemLongClick { void onLongClick(Transaction t); }

    class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.Holder> {
        List<Transaction> list;
        OnItemLongClick listener;

        public TransactionAdapter(List<Transaction> list, OnItemLongClick listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(getLayoutInflater().inflate(R.layout.item_transaction, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Transaction t = list.get(position);
            holder.tvCat.setText(t.category);
            holder.tvDate.setText(new SimpleDateFormat("dd MMM", Locale.US).format(new Date(t.date)));

            NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

            if(t.type.equals("Income")) {
                holder.tvAmt.setTextColor(Color.parseColor("#39FF14")); // Neon Green
                holder.tvAmt.setText("+ " + fmt.format(t.amount));
                holder.img.setColorFilter(Color.parseColor("#39FF14"));
            } else {
                holder.tvAmt.setTextColor(Color.parseColor("#FF1744")); // Neon Red
                holder.tvAmt.setText("- " + fmt.format(t.amount));
                holder.img.setColorFilter(Color.parseColor("#FF1744"));
            }

            // Long Press to Delete
            holder.itemView.setOnLongClickListener(v -> {
                listener.onLongClick(t);
                return true;
            });
        }
        @Override public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvCat, tvDate, tvAmt;
            ImageView img;
            public Holder(View v) {
                super(v);
                tvCat = v.findViewById(R.id.tvCategory);
                tvDate = v.findViewById(R.id.tvDate);
                tvAmt = v.findViewById(R.id.tvAmount);
                img = v.findViewById(R.id.imgIcon);
            }
        }
    }

    // ============ DATABASE ============
    class Transaction {
        int id; double amount; String category, type, desc; long date;
        Transaction(int id, double a, String c, String t, String ds, long d) {
            this.id = id; amount=a; category=c; type=t; desc=ds; date=d;
        }
    }

    public class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) { super(context, "ExpDB_Final_v2", null, 1); }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE users (email TEXT PRIMARY KEY, name TEXT, password TEXT)");
            db.execSQL("CREATE TABLE tx (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, amount REAL, category TEXT, type TEXT, desc TEXT, date INTEGER)");
            db.execSQL("CREATE TABLE cats (name TEXT)");
            db.execSQL("INSERT INTO cats VALUES ('Salary'), ('Food'), ('Transport'), ('Bills'), ('Entertainment'), ('Health'), ('Other')");
        }
        public void onUpgrade(SQLiteDatabase db, int o, int n) {
            db.execSQL("DROP TABLE IF EXISTS users"); db.execSQL("DROP TABLE IF EXISTS tx"); db.execSQL("DROP TABLE IF EXISTS cats"); onCreate(db);
        }
        public boolean registerUser(String n, String e, String p) {
            ContentValues v = new ContentValues(); v.put("name", n); v.put("email", e); v.put("password", p);
            return getWritableDatabase().insert("users", null, v) != -1;
        }
        public boolean checkUser(String e, String p) {
            Cursor c = getReadableDatabase().rawQuery("SELECT * FROM users WHERE email=? AND password=?", new String[]{e, p});
            boolean r = c.getCount() > 0; c.close(); return r;
        }
        public String getUserName(String e) {
            Cursor c = getReadableDatabase().rawQuery("SELECT name FROM users WHERE email=?", new String[]{e});
            String n = c.moveToFirst() ? c.getString(0) : "User"; c.close(); return n;
        }
        public void addTransaction(String email, double amt, String cat, String type, String desc, long date) {
            ContentValues v = new ContentValues();
            v.put("email", email); v.put("amount", amt); v.put("category", cat); v.put("type", type); v.put("desc", desc); v.put("date", date);
            getWritableDatabase().insert("tx", null, v);
        }
        public void deleteTransaction(int id) {
            getWritableDatabase().delete("tx", "id=?", new String[]{String.valueOf(id)});
        }
        public double getTotal(String email, String type) {
            Cursor c = getReadableDatabase().rawQuery("SELECT SUM(amount) FROM tx WHERE email=? AND type=?", new String[]{email, type});
            double res = c.moveToFirst() ? c.getDouble(0) : 0; c.close(); return res;
        }
        public List<Transaction> getRecentTransactions(String email) {
            List<Transaction> l = new ArrayList<>();
            Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tx WHERE email=? ORDER BY date DESC LIMIT 10", new String[]{email});
            while(c.moveToNext()) {
                // Ensure column indices match the schema: id=0, email=1, amount=2, category=3, type=4, desc=5, date=6
                l.add(new Transaction(c.getInt(0), c.getDouble(2), c.getString(3), c.getString(4), c.getString(5), c.getLong(6)));
            }
            c.close(); return l;
        }
        public List<String> getCategories() {
            List<String> l = new ArrayList<>();
            Cursor c = getReadableDatabase().rawQuery("SELECT * FROM cats", null);
            while(c.moveToNext()) l.add(c.getString(0));
            c.close(); return l;
        }
    }
}