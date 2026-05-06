package com.example.lulufindy;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class WalletManager {

    public interface BalanceCallback {
        void onBalanceFetched(double balance);
    }

    public interface WalletBalanceListener {
        void onBalanceChanged(double balance);
    }

    private static double localWalletBalance = 0.0;
    private static ListenerRegistration listener;



    public static void getWalletBalance(BalanceCallback callback) {
        String uid = getCurrentUserUid();
        if (uid == null) {
            callback.onBalanceFetched(localWalletBalance);
            return;
        }

        DocumentReference walletRef = FirebaseFirestore.getInstance().collection("wallets").document(uid);
        walletRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Double balance = documentSnapshot.getDouble("balance");
                callback.onBalanceFetched(balance != null ? balance : 0.0);
            } else {
                callback.onBalanceFetched(0.0);
            }
        });
    }

    public static void addToWallet(double amount) {
        String uid = getCurrentUserUid();
        if (uid == null) {
            localWalletBalance += amount;
            return;
        }

        DocumentReference walletRef = FirebaseFirestore.getInstance().collection("wallets").document(uid);
        walletRef.get().addOnSuccessListener(document -> {
            double current = 0;
            if (document.exists()) {
                Double b = document.getDouble("balance");
                if (b != null) current = b;
            }
            walletRef.set(new Wallet(current + amount));
        });
    }

    public static void deductFromWallet(double amount, BalanceCallback callback) {
        String uid = getCurrentUserUid();
        if (uid == null) {
            if (amount <= localWalletBalance) {
                localWalletBalance -= amount;
                callback.onBalanceFetched(localWalletBalance);
            } else {
                callback.onBalanceFetched(-1);
            }
            return;
        }

        DocumentReference walletRef = FirebaseFirestore.getInstance().collection("wallets").document(uid);
        walletRef.get().addOnSuccessListener(doc -> {
            double current = 0;
            if (doc.exists()) {
                Double b = doc.getDouble("balance");
                if (b != null) current = b;
            }

            if (amount <= current) {
                double newBalance = current - amount;
                walletRef.set(new Wallet(newBalance)).addOnSuccessListener(unused -> {
                    callback.onBalanceFetched(newBalance);
                });
            } else {
                callback.onBalanceFetched(-1);
            }
        });
    }

    public static void setWalletBalance(double amount) {
        String uid = getCurrentUserUid();
        if (uid == null) {
            localWalletBalance = amount;
            return;
        }

        DocumentReference walletRef = FirebaseFirestore.getInstance().collection("wallets").document(uid);
        walletRef.set(new Wallet(amount));
    }

    public static void startListening(WalletBalanceListener callback) {
        String uid = getCurrentUserUid();
        if (uid == null) return;

        DocumentReference walletRef = FirebaseFirestore.getInstance().collection("wallets").document(uid);
        stopListening();

        listener = walletRef.addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null) return;
            Double balance = snapshot.getDouble("balance");
            callback.onBalanceChanged(balance != null ? balance : 0.0);
        });
    }

    public static void stopListening() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }



    public static double getLocalWalletBalance() {
        return localWalletBalance;
    }

    public static void addToLocalWallet(double amount) {
        localWalletBalance += amount;
    }

    public static boolean deductFromLocalWallet(double amount) {
        if (amount <= localWalletBalance) {
            localWalletBalance -= amount;
            return true;
        }
        return false;
    }

    public static void setLocalWalletBalance(double amount) {
        localWalletBalance = amount;
    }



    private static String getCurrentUserUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }



    public static class Wallet {
        public double balance;

        public Wallet() {}
        public Wallet(double balance) {
            this.balance = balance;
        }
    }
}
