package me.fengmlo.electricityassistant.ui.applist

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.provider.Settings
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import me.drakeet.multitype.ItemViewBinder
import me.drakeet.multitype.MultiTypeAdapter
import me.fengmlo.electricityassistant.R
import me.fengmlo.electricityassistant.base.BaseActivity
import me.fengmlo.electricityassistant.base.BinderClickListener
import me.fengmlo.electricityassistant.base.BinderViewHolder
import me.fengmlo.electricityassistant.bindView


class AppListActivity : BaseActivity() {

    private val rvAppList: androidx.recyclerview.widget.RecyclerView by bindView(R.id.rv_app_list)

    private lateinit var model: AppListViewModel
    private val adapter = MultiTypeAdapter()

    override fun getLayoutId() = R.layout.activity_app_list

    override fun initView() {
        model = ViewModelProviders.of(this).get(AppListViewModel::class.java)
        model.getAppList().observe(this, Observer {
            adapter.items = it ?: arrayListOf<PackageInfo>()
        })

        rvAppList.adapter = adapter.apply { register(PackageInfo::class.java, AppBinder()) }
        rvAppList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvAppList.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                this,
                androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
            )
        )
    }

    private class AppBinder : ItemViewBinder<PackageInfo, AppHolder>() {

        override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): AppHolder {
            val root = inflater.inflate(R.layout.item_app_list, parent, false)
            return AppHolder(root).apply {
                onItemClickListener = BinderClickListener.getInstance<PackageInfo> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val context = itemView.context
                    val uri = Uri.fromParts("package", it.packageName, null)
                    intent.data = uri
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                itemView.setOnClickListener(onItemClickListener)
            }
        }

        override fun onBindViewHolder(holder: AppHolder, bean: PackageInfo) {
            val packageManager = holder.itemView.context.packageManager
            holder.tvAppName.text = bean.applicationInfo.loadLabel(packageManager).toString()
            holder.tvPackageName.text = bean.packageName
            holder.ivApp.setImageDrawable(
                    packageManager.getApplicationInfo(
                            bean.packageName,
                            0
                    ).loadIcon(packageManager)
            )
            holder.onItemClickListener.bean = bean
        }

    }

    private class AppHolder(itemView: View) : BinderViewHolder<PackageInfo>(itemView) {
        val ivApp: ImageView by bindView(R.id.iv_app)
        val tvPackageName: TextView by bindView(R.id.tv_package_name)
        val tvAppName: TextView by bindView(R.id.tv_app_name)
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, AppListActivity::class.java)
            context.startActivity(starter)
        }
    }
}
