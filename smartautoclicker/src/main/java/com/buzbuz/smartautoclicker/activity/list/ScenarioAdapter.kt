
package com.buzbuz.smartautoclicker.activity.list

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.base.extensions.setLeftCompoundDrawable
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.databinding.ItemDumbScenarioBinding
import com.buzbuz.smartautoclicker.databinding.ItemEmptyScenarioBinding
import com.buzbuz.smartautoclicker.databinding.ItemSmartScenarioBinding

import kotlinx.coroutines.Job

/**
 * Adapter for the display of the click scenarios created by the user into a RecyclerView.
 *
 * @param startScenarioListener listener upon the click on a scenario.
 * @param exportClickListener listener upon the export button of a scenario.
 * @param deleteScenarioListener listener upon the delete button of a scenario.
 */
class ScenarioAdapter(
    private val bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val startScenarioListener: ((ScenarioListUiState.Item) -> Unit),
    private val exportClickListener: ((ScenarioListUiState.Item) -> Unit),
    private val deleteScenarioListener: ((ScenarioListUiState.Item) -> Unit),
) : ListAdapter<ScenarioListUiState.Item, RecyclerView.ViewHolder>(ScenarioDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ScenarioListUiState.Item.Empty -> R.layout.item_empty_scenario
            is ScenarioListUiState.Item.Valid.Smart -> R.layout.item_smart_scenario
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_empty_scenario -> EmptyScenarioHolder(
                ItemEmptyScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                startScenarioListener,
                deleteScenarioListener,
            )

            R.layout.item_smart_scenario -> SmartScenarioViewHolder(
                ItemSmartScenarioBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                bitmapProvider,
                startScenarioListener,
                exportClickListener,
                deleteScenarioListener,
            )

            else -> throw IllegalArgumentException("Unsupported view type !")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EmptyScenarioHolder -> holder.onBind(getItem(position) as ScenarioListUiState.Item.Empty)
            is SmartScenarioViewHolder -> holder.onBind(getItem(position) as ScenarioListUiState.Item.Valid.Smart)
        }
    }
}

/** DiffUtil Callback comparing two ScenarioItem when updating the [ScenarioAdapter] list. */
object ScenarioDiffUtilCallback: DiffUtil.ItemCallback<ScenarioListUiState.Item>() {
    override fun areItemsTheSame(oldItem: ScenarioListUiState.Item, newItem: ScenarioListUiState.Item): Boolean =
        when {
            oldItem is ScenarioListUiState.Item.Empty.Smart && newItem is  ScenarioListUiState.Item.Empty.Smart->
                oldItem.scenario.id == newItem.scenario.id
            oldItem is ScenarioListUiState.Item.Valid.Smart && newItem is  ScenarioListUiState.Item.Valid.Smart->
                oldItem.scenario.id == newItem.scenario.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: ScenarioListUiState.Item, newItem: ScenarioListUiState.Item): Boolean =
        oldItem == newItem
}

class EmptyScenarioHolder(
    private val viewBinding: ItemEmptyScenarioBinding,
    private val startScenarioListener: ((ScenarioListUiState.Item) -> Unit),
    private val deleteScenarioListener: ((ScenarioListUiState.Item) -> Unit),
): RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(scenarioItem: ScenarioListUiState.Item.Empty) = viewBinding.apply {
        scenarioName.text = scenarioItem.displayName
        scenarioName.setLeftCompoundDrawable(
            R.drawable.ic_smart
        )

        buttonStart.setOnClickListener { startScenarioListener(scenarioItem) }
        buttonDelete.setOnClickListener { deleteScenarioListener(scenarioItem) }
    }
}

/** ViewHolder for the [ScenarioAdapter]. */
class SmartScenarioViewHolder(
    private val viewBinding: ItemSmartScenarioBinding,
    bitmapProvider: (ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit) -> Job?,
    private val startScenarioListener: ((ScenarioListUiState.Item) -> Unit),
    private val exportClickListener: ((ScenarioListUiState.Item) -> Unit),
    private val deleteScenarioListener: ((ScenarioListUiState.Item) -> Unit),
) : RecyclerView.ViewHolder(viewBinding.root) {

    private val eventsAdapter = ScenarioEventsAdapter(bitmapProvider)

    init {
        viewBinding.listEvent.adapter = eventsAdapter
    }

    fun onBind(scenarioItem: ScenarioListUiState.Item.Valid.Smart) = viewBinding.apply {
        scenarioName.text = scenarioItem.displayName
        detectionQuality.text = scenarioItem.detectionQuality.toString()
        triggerEventCount.text = scenarioItem.triggerEventCount.toString()

        eventsAdapter.submitList(scenarioItem.eventsItems)
        if (scenarioItem.eventsItems.isEmpty()) {
            listEvent.visibility = View.GONE
            noImageEvents.visibility = View.VISIBLE
        } else {
            listEvent.visibility = View.VISIBLE
            noImageEvents.visibility = View.GONE
        }

        if (scenarioItem.showExportCheckbox) {
            buttonDelete.visibility = View.GONE
            buttonStart.visibility = View.GONE
            buttonExport.apply {
                visibility = View.VISIBLE
                isChecked = scenarioItem.checkedForExport
            }
        } else {
            buttonDelete.visibility = View.VISIBLE
            buttonStart.visibility = View.VISIBLE
            buttonExport.visibility = View.GONE
        }

        buttonStart.setOnClickListener { startScenarioListener(scenarioItem) }
        buttonDelete.setOnClickListener { deleteScenarioListener(scenarioItem) }
        buttonExport.setOnClickListener { exportClickListener(scenarioItem) }
    }
}