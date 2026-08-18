package com.furry.live2dlauncher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.furry.live2dlauncher.R
import com.furry.live2dlauncher.core.AppItem
import com.furry.live2dlauncher.core.AppRepository
import com.furry.live2dlauncher.databinding.DrawerFragmentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 应用抽屉：展示全部可启动应用，支持搜索过滤。
 * 上滑桌面或点击指示条打开。
 */
class AppDrawerFragment : Fragment() {

    private var _binding: DrawerFragmentBinding? = null
    private val binding get() = _binding!!
    private val repo by lazy { AppRepository(requireContext()) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var loadJob: Job? = null
    private var allApps: List<AppItem> = emptyList()
    private lateinit var adapter: AppAdapter

    var onAppSelected: ((AppItem) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DrawerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = AppAdapter { item ->
            onAppSelected?.invoke(item)
        }
        binding.appList.layoutManager = LinearLayoutManager(requireContext())
        binding.appList.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadApps()
    }

    private fun loadApps() {
        loadJob?.cancel()
        loadJob = scope.launch {
            allApps = withContext(Dispatchers.IO) { repo.loadLaunchableApps() }
            adapter.submitList(allApps)
        }
    }

    private fun filter(query: String) {
        if (query.isBlank()) {
            adapter.submitList(allApps)
        } else {
            adapter.submitList(allApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            })
        }
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}

/** 应用列表适配器 */
class AppAdapter(
    private val onClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppAdapter.Holder>() {

    private val items = mutableListOf<AppItem>()

    class Holder(val view: View) : RecyclerView.ViewHolder(view)

    fun submitList(list: List<AppItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val icon = holder.view.findViewById<android.widget.ImageView>(R.id.app_icon)
        val name = holder.view.findViewById<android.widget.TextView>(R.id.app_name)
        icon.setImageDrawable(item.icon)
        name.text = item.label
        holder.view.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
