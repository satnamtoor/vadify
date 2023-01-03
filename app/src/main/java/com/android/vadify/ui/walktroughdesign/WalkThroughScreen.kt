package com.android.vadify.ui.walktroughdesign

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.android.vadify.R
import com.android.vadify.databinding.ActivityWalkThroughScreenBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.login.StartUpActivity
import com.android.vadify.ui.walktroughdesign.adapter.ScreenOverViewAdapter
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.activity_walk_through_screen.*


class WalkThroughScreen : DataBindingActivity<ActivityWalkThroughScreenBinding>() {

    companion object {
        const val TOTAL_TAB = 4
        const val INTIAL_COUNTER = 0
        const val LAST_TAB_INDEX = 3
    }


    override val layoutRes: Int
        get() = R.layout.activity_walk_through_screen


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribe(skipBtn.throttleClicks()) {
            startActivity(Intent(this@WalkThroughScreen, StartUpActivity::class.java))
            finish()
        }

        with(view_pager) {
            val pagerAdapter = ScreenOverViewAdapter(supportFragmentManager, TOTAL_TAB)
            this.adapter = pagerAdapter
            spring_dots_indicator.setViewPager(this)
            viewPagerListener()
        }
    }

    var isNeedToNavigate = false
    private fun viewPagerListener() {

        var counter = INTIAL_COUNTER
        view_pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                skipBtn.setText("SKIP")
                if (positionOffsetPixels == INTIAL_COUNTER && position == LAST_TAB_INDEX) {
                    counter++
                    skipBtn.setText("NEXT")
                    if (counter > 1 && !isNeedToNavigate) {
                        isNeedToNavigate = true
                        counter = INTIAL_COUNTER
                        //Log.i("initial-counter",""+counter)
                        startActivity(Intent(this@WalkThroughScreen, StartUpActivity::class.java))
                        finish()
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                counter = INTIAL_COUNTER
            }
        })
    }

}