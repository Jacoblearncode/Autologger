package com.nibm.autocare.SplashScreen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.nibm.autocare.Authentication.LoginActivity
import com.nibm.autocare.R
import com.nibm.autocare.SettingsManager

/**
 * First-launch walkthrough (U7). Three swipeable slides introducing the app.
 * Shown only once — the seen flag is persisted in SettingsManager.
 */
class OnboardingActivity : AppCompatActivity() {

    private data class Page(val iconRes: Int, val title: String, val body: String)

    private val pages = listOf(
        Page(R.drawable.ic_empty_vehicle, "Track Your Vehicles",
            "Keep all your vehicles in one place with photos, mileage, and health at a glance."),
        Page(R.drawable.ic_wrench, "Log Services & Fuel",
            "Record every service, fuel fill-up, and trip. Attach photos and notes to build a full history."),
        Page(R.drawable.ic_chart, "Insights & Reminders",
            "See spending trends, get reminders before services and documents expire, and export reports.")
    )

    private lateinit var dots: Array<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val pager = findViewById<ViewPager2>(R.id.onboardPager)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnSkip = findViewById<TextView>(R.id.btnSkip)

        pager.adapter = OnboardAdapter()
        setupDots()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                btnNext.text = if (position == pages.lastIndex) "Get Started" else "Next"
            }
        })

        btnNext.setOnClickListener {
            if (pager.currentItem == pages.lastIndex) finishOnboarding()
            else pager.currentItem += 1
        }
        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        SettingsManager.setOnboardingSeen(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupDots() {
        val container = findViewById<LinearLayout>(R.id.dotsContainer)
        dots = Array(pages.size) { ImageView(this) }
        val size = (8 * resources.displayMetrics.density).toInt()
        val margin = (4 * resources.displayMetrics.density).toInt()
        dots.forEach { dot ->
            dot.setImageResource(R.drawable.onboarding_dot)
            val lp = LinearLayout.LayoutParams(size, size).apply { setMargins(margin, 0, margin, 0) }
            container.addView(dot, lp)
        }
        updateDots(0)
    }

    private fun updateDots(active: Int) {
        dots.forEachIndexed { i, dot ->
            dot.alpha = if (i == active) 1f else 0.3f
            val color = if (i == active) R.color.accent_lime else R.color.gray
            dot.setColorFilter(ContextCompat.getColor(this, color))
        }
    }

    private inner class OnboardAdapter : RecyclerView.Adapter<OnboardAdapter.PageVH>() {
        inner class PageVH(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.ivOnboardIcon)
            val title: TextView = view.findViewById(R.id.tvOnboardTitle)
            val body: TextView = view.findViewById(R.id.tvOnboardBody)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return PageVH(v)
        }

        override fun onBindViewHolder(holder: PageVH, position: Int) {
            val page = pages[position]
            holder.icon.setImageResource(page.iconRes)
            holder.title.text = page.title
            holder.body.text = page.body
        }

        override fun getItemCount() = pages.size
    }
}
