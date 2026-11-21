package com.example.successtudy.logic;

import com.example.successtudy.database.dao.AppDao;
import com.example.successtudy.database.entity.Note;
import com.example.successtudy.database.entity.Transaction;
import com.example.successtudy.database.entity.User;

public class MarketplaceService {

    public enum PurchaseResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        ALREADY_OWNED,
        OWN_NOTE,
        USER_NOT_FOUND,
        ERROR
    }

    /**
     * Executes note purchase logic inside background database transactions.
     * Deducts cost from buyer, pays seller (applying 15% platform commission), and creates a Transaction log.
     */
    public static PurchaseResult purchaseNote(AppDao dao, int buyerId, int noteId) {
        try {
            // 1. Fetch buyer
            User buyer = dao.getUserById(buyerId);
            if (buyer == null) return PurchaseResult.USER_NOT_FOUND;

            // 2. Fetch note
            Note note = dao.getNoteById(noteId);
            if (note == null) return PurchaseResult.ERROR;

            // 3. Prevent buying own note
            if (note.uploaderId == buyerId) {
                return PurchaseResult.OWN_NOTE;
            }

            // 4. Check if already purchased
            Transaction existingTx = dao.getTransactionByNoteAndBuyer(noteId, buyerId);
            if (existingTx != null) {
                return PurchaseResult.ALREADY_OWNED;
            }

            // 5. Check balance
            if (buyer.balance < note.price) {
                return PurchaseResult.INSUFFICIENT_FUNDS;
            }

            // 6. Fetch seller
            User seller = dao.getUserById(note.uploaderId);
            if (seller == null) return PurchaseResult.USER_NOT_FOUND;

            // 7. Process financial transfer
            buyer.balance -= note.price;
            
            double commission = note.price * 0.15;
            double sellerEarnings = note.price - commission;
            seller.balance += sellerEarnings;

            // Update database states
            dao.updateUser(buyer);
            dao.updateUser(seller);

            // Log Transaction
            Transaction tx = new Transaction(buyerId, seller.id, noteId, note.price, System.currentTimeMillis());
            dao.insertTransaction(tx);

            return PurchaseResult.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return PurchaseResult.ERROR;
        }
    }
}
