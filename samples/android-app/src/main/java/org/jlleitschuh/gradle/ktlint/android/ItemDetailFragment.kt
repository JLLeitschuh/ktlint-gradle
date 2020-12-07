package org.jlleitschuh.gradle.ktlint.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jlleitschuh.gradle.ktlint.android.databinding.ItemDetailBinding
import org.jlleitschuh.gradle.ktlint.android.dummy.DummyContent

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a [ItemListActivity]
 * in two-pane mode (on tablets) or a [ItemDetailActivity]
 * on handsets.
 */
class ItemDetailFragment : Fragment() {

    /**
     * The dummy content this fragment is presenting.
     */
    private var mItem: DummyContent.DummyItem? = null

    private var viewBinding: ItemDetailBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments?.containsKey(ARG_ITEM_ID) == true) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = DummyContent.ITEM_MAP[arguments?.getString(ARG_ITEM_ID)]
            mItem?.let {
                (activity as? ItemDetailActivity)?.viewBinding?.detailToolbar?.title = it.content
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = ItemDetailBinding.inflate(inflater, container, false)

        // Show the dummy content as text in a TextView.
        mItem?.let {
            viewBinding?.itemDetail?.text = it.details
        }

        return viewBinding?.root
    }

    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_ID = "item_id"
    }
}
