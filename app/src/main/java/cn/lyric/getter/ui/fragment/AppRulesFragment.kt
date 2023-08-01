package cn.lyric.getter.ui.fragment


import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.lyric.getter.R
import cn.lyric.getter.config.ActivityOwnSP.config
import cn.lyric.getter.data.AppInfos
import cn.lyric.getter.data.AppRule
import cn.lyric.getter.databinding.FragmentAppRulesBinding
import cn.lyric.getter.databinding.ItemsAppBinding
import cn.lyric.getter.tool.ActivityTools.getAppRules
import cn.lyric.getter.tool.ActivityTools.openUrl
import cn.lyric.getter.tool.ActivityTools.updateAppRules
import cn.lyric.getter.tool.JsonTools.toJSON
import cn.lyric.getter.tool.Tools.goMainThread
import cn.lyric.getter.ui.adapter.AppRulesAdapter
import cn.lyric.getter.ui.dialog.MaterialProgressDialog


class AppRulesFragment : Fragment() {

    private lateinit var appAdapter: AppRulesAdapter

    private val appRules: List<AppRule> by lazy { getAppRules().appRules }
    private val manager: PackageManager by lazy { requireContext().packageManager }
    private val packageManager: PackageManager by lazy { requireContext().packageManager }
    private var _binding: FragmentAppRulesBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAppRulesBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mLayoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        appAdapter = AppRulesAdapter().apply {
            setOnItemClickListener(object : AppRulesAdapter.OnItemClickListener {
                override fun onItemClick(position: Int, viewBinding: ItemsAppBinding) {
                    viewBinding.apply{
                        appRulesCardView.apply {
                            visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        }
                        appRulesTextView.apply {
                            if (text.isEmpty()) {
                                text = appRules.filter { it.packageName == dataLists[position].packageName }.toJSON(true)
                            }
                        }
                    }
                }
            })
        }
        binding.apply {
            toolbar.apply {
                inflateMenu(R.menu.app_rules_menu)
                menu.findItem(R.id.show_all_rules).apply {
                    isChecked = config.showAllRules
                }
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.show_all_rules -> {
                            it.isChecked = !it.isChecked
                            config.showAllRules = it.isChecked
                            loadAppRules()
                        }

                        R.id.update_app_rules -> {
                            updateAppRules()
                        }
                    }
                    true
                }
            }
            card.setOnClickListener {
                "https://github.com/xiaowine/Lyric-Getter/issues/new?assignees=&labels=&projects=&template=feature_request.md&title=App%E8%A7%84%E5%88%99%E6%94%B9%E5%8A%A8".openUrl()
            }
            recyclerView.apply {
                layoutManager = mLayoutManager
                setItemViewCacheSize(2000)
                adapter = appAdapter
            }
            swipeRefreshLayout.setOnRefreshListener {
                loadAppRules(true)
            }
            loadAppRules()
        }
    }


    private fun loadAppRules(isSwipeRefresh: Boolean = false) {
        appAdapter.removeAllData()
        val dialog = MaterialProgressDialog(requireContext()).apply {
            setTitle(getString(R.string.getting_app_information))
            setMessage(getString(R.string.getting_app_information_tips))
            show()
        }
        Thread {
            val installedPackages = manager.getInstalledPackages(0)
            val appInfosPackNames = installedPackages.map { it.packageName }
            appRules.forEach { appRule ->
                var appInfos: AppInfos? = null
                if (appInfosPackNames.contains(appRule.packageName)) {
                    val packageInfo = installedPackages.filter { it.packageName == appRule.packageName }[0]
                    val applicationInfo = packageInfo.applicationInfo
                    appInfos = AppInfos(applicationInfo.loadLabel(packageManager).toString(), applicationInfo.loadIcon(packageManager), packageInfo.packageName, packageInfo.versionCode, appRule)

                } else if (config.showAllRules) {
                    val packageInfo = installedPackages.filter { it.packageName == "com.android.systemui" }[0]
                    appInfos = AppInfos(appRule.name, packageInfo.applicationInfo.loadIcon(packageManager), appRule.packageName, 0, appRule, false)
                }
                goMainThread { appInfos?.let { appAdapter.addData(it) } }
            }
            dialog.dismiss()
            goMainThread {
                binding.toolbar.title = "${getString(R.string.app_rules_fragment_label)}:(${appAdapter.dataLists.size})"
                if (isSwipeRefresh) binding.swipeRefreshLayout.isRefreshing = false
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}