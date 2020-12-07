package org.jlleitschuh.gradle.ktlint.android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import org.jlleitschuh.gradle.ktlint.android.ItemListActivity.SimpleItemRecyclerViewAdapter.ViewHolder
import org.jlleitschuh.gradle.ktlint.android.databinding.ActivityItemListBinding
import org.jlleitschuh.gradle.ktlint.android.dummy.DummyContent

/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [ItemDetailActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class ItemListActivity : AppCompatActivity() {

    private var viewBinding: ActivityItemListBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityItemListBinding.inflate(LayoutInflater.from(this))
        setContentView(viewBinding?.root)

        setSupportActionBar(viewBinding?.toolbar)
        viewBinding?.toolbar?.title = title

        viewBinding?.fab?.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        setupRecyclerView(viewBinding?.itemListContainer?.itemList)
    }

    private fun setupRecyclerView(recyclerView: RecyclerView?) {
        recyclerView?.adapter = SimpleItemRecyclerViewAdapter(DummyContent.ITEMS)
    }

    class SimpleItemRecyclerViewAdapter(
        private val mValues: List<DummyContent.DummyItem>,
    ) : RecyclerView.Adapter<ViewHolder>() {

        private val mOnClickListener: View.OnClickListener

        init {
            mOnClickListener = View.OnClickListener { v ->
                val item = v.tag as DummyContent.DummyItem
                val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                    putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id)
                }
                v.context.startActivity(intent)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = mValues[position]
            holder.mIdView.text = item.id
            holder.mContentView.text = item.content

            with(holder.itemView) {
                tag = item
                setOnClickListener(mOnClickListener)
            }
        }

        override fun getItemCount(): Int {
            return mValues.size
        }

        inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val mIdView: TextView = mView.findViewById(R.id.id_text)
            val mContentView: TextView = mView.findViewById(R.id.content)
        }
    }
}
