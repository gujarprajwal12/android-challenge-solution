package com.prajwal.guajr.randomstringgenerator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.prajwal.guajr.randomstringgenerator.databinding.ActivityMainBinding
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.prajwal.guajr.randomstringgenerator.DataClass.RandomString
import com.prajwal.guajr.randomstringgenerator.DataClass.RandomStringResponse
import com.prajwal.guajr.randomstringgenerator.ViewModel.RandomStringViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.prajwal.guajr.randomstringgenerator.databinding.ItemStringBinding
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: RandomStringViewModel by viewModels()
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        setupObservers()
        setupClickListeners()

    }
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: RandomStringViewModel.UiState) {
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.errorText.visibility = if (state.error != null) View.VISIBLE else View.GONE
        state.error?.let { binding.errorText.text = it }

        binding.stringsContainer.removeAllViews()
        state.strings.forEach { randomString ->
            ItemStringBinding.inflate(layoutInflater).apply {
                stringText.text = randomString.value
                lengthText.text = getString(R.string.length, randomString.length)
                createdText.text = displayDateFormat.format(randomString.created)
                deleteButton.setOnClickListener { viewModel.removeString(randomString) }
                binding.stringsContainer.addView(root)
            }
        }
    }

    private fun setupClickListeners() {
        binding.generateButton.setOnClickListener {
            val length = binding.lengthInput.text.toString().toIntOrNull() ?: 0
            if (length > 0) {
                fetchRandomString(length)
            } else {
                viewModel.showError(getString(R.string.invalid_length))
            }
        }

        binding.clearAllButton.setOnClickListener {
            viewModel.clearAllStrings()
        }
    }

    private fun fetchRandomString(length: Int) {
        viewModel.setLoading(true)
        lifecycleScope.launch {
            try {
                withTimeout(15_000) { // 15 second timeout
                    if (!isProviderAvailable()) {
                        viewModel.showError(getString(R.string.provider_not_available))
                        return@withTimeout
                    }

                    val uri = Uri.parse("content://com.iav.contestdataprovider/text")
                    val bundle = Bundle().apply {
                        putInt(ContentResolver.QUERY_ARG_LIMIT, length)
                    }

                    val cursor = contentResolver.query(
                        uri,
                        arrayOf("data"),
                        bundle,
                        null
                    )

                    cursor?.use {
                        if (it.moveToFirst()) {
                            val data = it.getString(0)
                            if (data.isNullOrEmpty()) {
                                viewModel.showError(getString(R.string.empty_response))
                            } else {
                                parseAndAddString(data)
                            }
                        } else {
                            viewModel.showError(getString(R.string.no_data_available))
                        }
                    } ?: viewModel.showError(getString(R.string.service_unavailable))
                }
            } catch (e: TimeoutCancellationException) {
                viewModel.showError(getString(R.string.service_timeout))
            } catch (e: SecurityException) {
                viewModel.showError(getString(R.string.permission_denied))
                Log.e("ContentProvider", "Permission error", e)
            } catch (e: Exception) {
                viewModel.showError(getString(R.string.general_error, e.localizedMessage))
                Log.e("ContentProvider", "Error", e)
            } finally {
                viewModel.setLoading(false)
            }
        }
    }

    private fun parseAndAddString(jsonData: String) {
        try {
            val response = gson.fromJson(jsonData, RandomStringResponse::class.java)
            val randomText = response.randomText
            val createdDate = dateFormat.parse(randomText.created) ?: Date()
            viewModel.addString(RandomString(randomText.value, randomText.length, createdDate))
        } catch (e: Exception) {
            viewModel.showError(getString(R.string.parse_error, e.localizedMessage))
            Log.e("ContentProvider", "Parsing error", e)
        }
    }

    private fun isProviderAvailable(): Boolean {
        return try {
            packageManager.resolveContentProvider(
                "com.iav.contestdataprovider",
                PackageManager.MATCH_ALL
            ) != null
        } catch (e: Exception) {
            false
        }
    }
}