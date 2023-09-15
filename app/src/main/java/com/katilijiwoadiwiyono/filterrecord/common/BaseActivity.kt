package com.katilijiwoadiwiyono.filterrecord.common

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB: ViewBinding>: AppCompatActivity() {

    private lateinit var _binding : VB
    val binding: VB get() {
        if(::_binding.isInitialized) return _binding
        else _binding = getViewBinding()
        return _binding
    }

    abstract fun getViewBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = getViewBinding()
        setContentView(binding.root)
    }

    fun showToast(message: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, length).show()
    }

}