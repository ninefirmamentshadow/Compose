package com.drafts.compose.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.drafts.compose.R
import com.drafts.compose.databinding.ActivityMainBinding
import com.drafts.compose.ui.check.CheckFragment
import com.drafts.compose.ui.compose.ComposeFragment
import com.drafts.compose.ui.scripts.ScriptsFragment
import com.drafts.compose.ui.tests.TestsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_compose -> show(ComposeFragment(), R.string.tab_compose)
                R.id.nav_check -> show(CheckFragment(), R.string.tab_check)
                R.id.nav_tests -> show(TestsFragment(), R.string.tab_tests)
                R.id.nav_scripts -> show(ScriptsFragment(), R.string.tab_scripts)
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_compose
        }

        // Tapping a finding in CHECK lands on the field it came from.
        viewModel.jump.observe(this) { target ->
            if (target != null && binding.bottomNav.selectedItemId != R.id.nav_compose) {
                binding.bottomNav.selectedItemId = R.id.nav_compose
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.flush()
    }

    private fun show(fragment: Fragment, titleRes: Int) {
        supportActionBar?.setTitle(titleRes)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
