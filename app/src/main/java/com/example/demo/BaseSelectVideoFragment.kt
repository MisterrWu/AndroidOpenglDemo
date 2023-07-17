package com.example.demo

import android.content.ContentResolver
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.example.demo.databinding.FragmentSelectVideoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseSelectVideoFragment : Fragment() {

    private lateinit var binding: FragmentSelectVideoBinding
    protected var adapter: BaseQuickAdapter<Media, BaseViewHolder>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        activity?.also {
            (it as AppCompatActivity).supportActionBar?.title = getTitle()
            it.invalidateOptionsMenu()
        }
        adapter = object : BaseQuickAdapter<Media, BaseViewHolder>(R.layout.layout_media_item){
            override fun convert(holder: BaseViewHolder, item: Media) {
                holder.setText(R.id.tv_name, item.name)
                holder.getViewOrNull<CheckBox>(R.id.check_box)?.also {checkBox ->
                    checkBox.setOnCheckedChangeListener(null)
                    checkBox.isChecked = item.isSelected
                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        item.isSelected = isChecked
                        if(item.isSelected) {
                            onItemSelected(item, holder.bindingAdapterPosition)
                        }
                    }
                    holder.itemView.setOnClickListener {
                        checkBox.performClick()
                    }
                }
            }
        }
        binding.recyclerList.adapter = adapter
        initData()
    }

    protected open fun onItemSelected(item: Media, position: Int) {}

    private fun initData() {
        lifecycleScope.launch {
            val list = activity?.let { getVideoFiles(it.contentResolver) }
            list?.also { adapter?.setNewInstance(it) }
        }
    }

    private suspend fun getVideoFiles(contentResolver: ContentResolver) = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
        )

        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )
        val list = mutableListOf<Media>()

        cursor?.use { cs ->
            val pathColumnIndex = cs.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val nameColumnIndex = cs.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            while (cs.moveToNext() && list.size < 20) {
                val path = cs.getString(pathColumnIndex)
                val name = cs.getString(nameColumnIndex)
                val media = Media(name, path, false)
                list.add(media)
            }
        }
        list
    }

    abstract fun getTitle(): CharSequence

    protected fun getSelectedMedias() : List<Media> {
        return adapter?.data?.let { data ->
            data.filter { it.isSelected }
        } ?: mutableListOf()
    }

    protected fun showToast(msg:String) {
        context?.also {
            Toast.makeText(it.applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        activity?.menuInflater?.inflate(R.menu.menu_main,menu)
        menu.findItem(R.id.action_settings)?.title = "确认"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_settings) {
            clickRightMenu()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    abstract fun clickRightMenu()
}