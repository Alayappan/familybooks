package com.example.sync

import com.example.data.model.FamilyInvoiceEntity
import com.example.data.model.FamilyMemberEntity
import com.example.data.model.SyncPayload
import com.example.data.model.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject

object JsonSerializer {

    fun serializeSyncPayload(payload: SyncPayload): String {
        val root = JSONObject()
        root.put("deviceId", payload.deviceId)
        root.put("memberName", payload.memberName)
        root.put("deviceName", payload.deviceName)
        root.put("macAddress", payload.macAddress)
        root.put("appVersion", payload.appVersion)
        root.put("timestamp", payload.timestamp)

        val txArray = JSONArray()
        for (tx in payload.transactions) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type)
            obj.put("amount", tx.amount)
            obj.put("date", tx.date)
            obj.put("category", tx.category)
            obj.put("paymentMethod", tx.paymentMethod)
            obj.put("paidBy", tx.paidBy)
            obj.put("notes", tx.notes)
            obj.put("createdAt", tx.createdAt)
            obj.put("updatedAt", tx.updatedAt)
            obj.put("isDeleted", tx.isDeleted)
            obj.put("deviceId", tx.deviceId)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val invArray = JSONArray()
        for (inv in payload.invoices) {
            val obj = JSONObject()
            obj.put("id", inv.id)
            obj.put("title", inv.title)
            obj.put("fromMember", inv.fromMember)
            obj.put("toMember", inv.toMember)
            obj.put("amount", inv.amount)
            obj.put("date", inv.date)
            if (inv.dueDate != null) obj.put("dueDate", inv.dueDate)
            obj.put("notes", inv.notes)
            obj.put("status", inv.status)
            if (inv.settledAt != null) obj.put("settledAt", inv.settledAt)
            obj.put("createdAt", inv.createdAt)
            obj.put("updatedAt", inv.updatedAt)
            obj.put("isDeleted", inv.isDeleted)
            obj.put("deviceId", inv.deviceId)
            invArray.put(obj)
        }
        root.put("invoices", invArray)

        val memArray = JSONArray()
        for (mem in payload.members) {
            val obj = JSONObject()
            obj.put("id", mem.id)
            obj.put("deviceId", mem.deviceId)
            obj.put("name", mem.name)
            obj.put("deviceName", mem.deviceName)
            obj.put("macAddress", mem.macAddress)
            obj.put("role", mem.role)
            obj.put("avatarColorHex", mem.avatarColorHex)
            obj.put("hostAddress", mem.hostAddress)
            obj.put("port", mem.port)
            obj.put("createdAt", mem.createdAt)
            obj.put("updatedAt", mem.updatedAt)
            obj.put("isDeleted", mem.isDeleted)
            memArray.put(obj)
        }
        root.put("members", memArray)

        return root.toString()
    }

    fun deserializeSyncPayload(jsonString: String): SyncPayload {
        val root = JSONObject(jsonString)
        val deviceId = root.optString("deviceId", "")
        val memberName = root.optString("memberName", "Unknown")
        val deviceName = root.optString("deviceName", "")
        val macAddress = root.optString("macAddress", "")
        val appVersion = root.optString("appVersion", "1.0")
        val timestamp = root.optLong("timestamp", System.currentTimeMillis())

        val transactions = mutableListOf<TransactionEntity>()
        val txArray = root.optJSONArray("transactions")
        if (txArray != null) {
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = obj.getString("id"),
                        type = obj.optString("type", "EXPENSE"),
                        amount = obj.optDouble("amount", 0.0),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        category = obj.optString("category", "General"),
                        paymentMethod = obj.optString("paymentMethod", "Cash"),
                        paidBy = obj.optString("paidBy", "Unknown"),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        deviceId = obj.optString("deviceId", "")
                    )
                )
            }
        }

        val invoices = mutableListOf<FamilyInvoiceEntity>()
        val invArray = root.optJSONArray("invoices")
        if (invArray != null) {
            for (i in 0 until invArray.length()) {
                val obj = invArray.getJSONObject(i)
                invoices.add(
                    FamilyInvoiceEntity(
                        id = obj.getString("id"),
                        title = obj.optString("title", "Invoice"),
                        fromMember = obj.optString("fromMember", ""),
                        toMember = obj.optString("toMember", ""),
                        amount = obj.optDouble("amount", 0.0),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        dueDate = if (obj.has("dueDate")) obj.optLong("dueDate") else null,
                        notes = obj.optString("notes", ""),
                        status = obj.optString("status", "UNPAID"),
                        settledAt = if (obj.has("settledAt")) obj.optLong("settledAt") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false),
                        deviceId = obj.optString("deviceId", "")
                    )
                )
            }
        }

        val members = mutableListOf<FamilyMemberEntity>()
        val memArray = root.optJSONArray("members")
        if (memArray != null) {
            for (i in 0 until memArray.length()) {
                val obj = memArray.getJSONObject(i)
                val mDeviceName = obj.optString("deviceName", obj.optString("role", "Family Device"))
                val mMacAddress = obj.optString("macAddress", "")
                members.add(
                    FamilyMemberEntity(
                        id = obj.getString("id"),
                        deviceId = obj.optString("deviceId", ""),
                        name = obj.getString("name"),
                        deviceName = mDeviceName,
                        macAddress = mMacAddress,
                        role = obj.optString("role", mDeviceName),
                        avatarColorHex = obj.optString("avatarColorHex", "#0A2540"),
                        hostAddress = obj.optString("hostAddress", ""),
                        port = obj.optInt("port", 8976),
                        isSelf = false,
                        connectionStatus = "SYNCED",
                        lastConnectedAt = System.currentTimeMillis(),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
        }

        return SyncPayload(
            deviceId = deviceId,
            memberName = memberName,
            deviceName = deviceName,
            macAddress = macAddress,
            appVersion = appVersion,
            timestamp = timestamp,
            transactions = transactions,
            invoices = invoices,
            members = members
        )
    }
}
