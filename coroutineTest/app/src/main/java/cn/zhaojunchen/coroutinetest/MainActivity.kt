package cn.zhaojunchen.coroutinetest

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // set default entry
        TestActivity.start(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.testLaunch01 -> {
                TestLaunch01Activity.start(this)
            }
            R.id.testException1 -> {
                TestExceptionActivity.start(this)
            }
            R.id.testRandom1 -> {
                TestActivity.start(this)
            }

        }
    }

}