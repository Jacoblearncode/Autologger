package com.nibm.autocare

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ActivityHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_history)

        findViewById<ImageButton>(R.id.btnBackHistory).setOnClickListener { finish() }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        loadHistory(uid)
    }

    private fun loadHistory(userId: String) {
        val ll = findViewById<LinearLayout>(R.id.llHistoryFeed)
        val tvEmpty = findViewById<TextView>(R.id.tvHistoryEmpty)

        data class FeedItem(val label: String, val date: String)
        val items = mutableListOf<FeedItem>()

        val db = FirebaseDatabase.getInstance()
        db.reference.child("users_services").child(userId).get()
            .addOnSuccessListener { snap ->
                for (vehicleSnap in snap.children) {
                    val reg = vehicleSnap.key ?: continue
                    for (record in vehicleSnap.children) {
                        val date = record.child("date").getValue(String::class.java) ?: continue
                        val type = record.child("serviceType").getValue(String::class.java) ?: "Service"
                        items.add(FeedItem("🔧 $type — $reg", date))
                    }
                }
                db.reference.child("users_fuel_logs").child(userId).get()
                    .addOnSuccessListener { fuelSnap ->
                        for (entry in fuelSnap.children) {
                            val date = entry.child("date").getValue(String::class.java) ?: continue
                            val reg = entry.child("registrationNumber").getValue(String::class.java) ?: continue
                            items.add(FeedItem("⛽ Fuel log — $reg", date))
                        }
                        val dateFmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        val sorted = items.sortedByDescending {
                            try { dateFmt.parse(it.date) } catch (_: Exception) { null }
                        }
                        if (sorted.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            return@addOnSuccessListener
                        }
                        tvEmpty.visibility = View.GONE
                        val primaryColor = ContextCompat.getColor(this, R.color.text_primary)
                        val secondaryColor = ContextCompat.getColor(this, R.color.text_secondary)
                        val dividerColor = ContextCompat.getColor(this, R.color.gray_300)

                        sorted.forEach { item ->
                            val row = LinearLayout(this).apply {
                                orientation = LinearLayout.HORIZONTAL
                                setPadding(0, 12, 0, 12)
                            }
                            val tvLabel = TextView(this).apply {
                                text = item.label
                                textSize = 13f
                                setTextColor(primaryColor)
                                layoutParams = LinearLayout.LayoutParams(
                                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                                )
                            }
                            val tvDate = TextView(this).apply {
                                text = item.date
                                textSize = 11f
                                setTextColor(secondaryColor)
                            }
                            row.addView(tvLabel)
                            row.addView(tvDate)
                            ll.addView(row)

                            val divider = View(this).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                                )
                                setBackgroundColor(dividerColor)
                            }
                            ll.addView(divider)
                        }
                    }
            }
    }
}
